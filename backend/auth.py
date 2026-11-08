import re
import secrets
import sqlite3
import time
import uuid
from urllib.parse import urlencode, urlparse

import httpx
from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials
from pydantic import BaseModel, Field, field_validator
from starlette.responses import RedirectResponse

from app import config as app_config
from app import security

# Re-exported for backward compatibility.
from app.db import (
    connect as _connect,
    database_path as _database_path,
    initialize as _initialize,
)
from app.db import oauth, sessions, users

# Re-exported for backward compatibility.
from app.deps import bearer, current_user
from app.services import auth_service
from app.security import (
    PASSWORD_ROUNDS,
    SESSION_TTL_SECONDS,
    decode_session as _decode_session,
    hash_password as _hash_password,
    session_secret as _session_secret,
    verify_password as _verify_password,
)

router = APIRouter(prefix="/auth", tags=["authentication"])
EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
APP_REDIRECT_URI = "oeil://auth/callback"

PROVIDERS = {
    "google": {
        "client_id": "GOOGLE_CLIENT_ID",
        "client_secret": "GOOGLE_CLIENT_SECRET",
        "authorize": "https://accounts.google.com/o/oauth2/v2/auth",
        "token": "https://oauth2.googleapis.com/token",
        "profile": "https://openidconnect.googleapis.com/v1/userinfo",
        "scope": "openid email profile",
    },
    "microsoft": {
        "client_id": "MICROSOFT_CLIENT_ID",
        "client_secret": "MICROSOFT_CLIENT_SECRET",
        "authorize": "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        "token": "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        "profile": "https://graph.microsoft.com/v1.0/me",
        "scope": "openid email profile User.Read",
    },
    "github": {
        "client_id": "GITHUB_CLIENT_ID",
        "client_secret": "GITHUB_CLIENT_SECRET",
        "authorize": "https://github.com/login/oauth/authorize",
        "token": "https://github.com/login/oauth/access_token",
        "profile": "https://api.github.com/user",
        "scope": "read:user user:email",
    },
}


class Credentials(BaseModel):
    email: str = Field(min_length=3, max_length=254)
    password: str = Field(min_length=8, max_length=256)
    name: str | None = Field(default=None, min_length=1, max_length=80)

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        normalized = value.strip().casefold()
        if not EMAIL_PATTERN.fullmatch(normalized):
            raise ValueError("Enter a valid email address.")
        return normalized

    @field_validator("name")
    @classmethod
    def normalize_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("Name cannot be blank.")
        return normalized


def _issue_session(user_id: str) -> str:
    return auth_service.issue_session(user_id)


def _user_payload(user: sqlite3.Row) -> dict[str, str]:
    return {"id": user["id"], "name": user["name"], "email": user["email"]}


def _auth_response(user: sqlite3.Row) -> dict:
    return {"token": _issue_session(user["id"]), "user": _user_payload(user)}


def _get_or_create_oauth_user(
    provider: str, provider_user_id: str, email: str, name: str
) -> sqlite3.Row:
    normalized_email = email.strip().casefold()
    if not EMAIL_PATTERN.fullmatch(normalized_email):
        raise HTTPException(status_code=400, detail="Provider did not return a valid email.")
    display_name = name.strip()[:80] or normalized_email.split("@", 1)[0]
    with _connect() as connection:
        account = oauth.find_account(connection, provider, provider_user_id)
        if account:
            return account
        user = users.find_by_email(connection, normalized_email)
        if user is None:
            user_id = str(uuid.uuid4())
            users.create(
                connection,
                user_id=user_id,
                email=normalized_email,
                name=display_name,
                password_hash=None,
                created_at=int(time.time()),
            )
        else:
            user_id = user["id"]
        oauth.create_account(
            connection,
            provider=provider,
            provider_user_id=provider_user_id,
            user_id=user_id,
        )
        return users.find_by_id(connection, user_id)


@router.post("/signup")
def signup(credentials: Credentials) -> dict:
    try:
        user = auth_service.signup(
            email=credentials.email,
            password=credentials.password,
            name=credentials.name,
        )
    except auth_service.NameRequiredError:
        raise HTTPException(
            status_code=422, detail="Name is required to create an account."
        )
    except auth_service.DuplicateEmailError as error:
        raise HTTPException(
            status_code=409, detail="An account with this email already exists."
        ) from error.__cause__
    return _auth_response(user)


@router.post("/login")
def login(credentials: Credentials) -> dict:
    try:
        user = auth_service.login(
            email=credentials.email,
            password=credentials.password,
        )
    except auth_service.InvalidCredentialsError:
        raise HTTPException(status_code=401, detail="Email or password is incorrect.")
    return _auth_response(user)


@router.get("/me")
def me(user: sqlite3.Row = Depends(current_user)) -> dict:
    return _user_payload(user)


@router.post("/logout")
def logout(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    user: sqlite3.Row = Depends(current_user),
) -> dict[str, str]:
    payload = _decode_session(credentials.credentials)
    with _connect() as connection:
        sessions.revoke(connection, payload["jti"], user["id"])
    return {"status": "ok"}


def _provider_config(provider: str) -> dict:
    config = PROVIDERS.get(provider)
    if config is None:
        raise HTTPException(status_code=404, detail="Unsupported sign-in provider.")
    client_id = app_config.env_value(config["client_id"])
    client_secret = app_config.env_value(config["client_secret"])
    public_base = app_config.public_base_url()
    if not client_id or not client_secret or not public_base:
        raise HTTPException(
            status_code=503,
            detail=f"{provider.title()} sign-in is not configured on this backend.",
        )
    parsed_base = urlparse(public_base)
    if parsed_base.scheme != "https" or not parsed_base.netloc:
        raise HTTPException(
            status_code=503,
            detail="OEIL_PUBLIC_BASE_URL must be the public HTTPS backend URL.",
        )
    return {**config, "id": client_id, "secret": client_secret, "base": public_base}


@router.get("/oauth/{provider}/start")
def oauth_start(provider: str, redirect_uri: str = APP_REDIRECT_URI) -> RedirectResponse:
    if redirect_uri != APP_REDIRECT_URI:
        raise HTTPException(status_code=400, detail="Invalid application redirect URI.")
    config = _provider_config(provider)
    state = secrets.token_urlsafe(32)
    expires_at = int(time.time()) + 600
    _initialize()
    with _connect() as connection:
        oauth.delete_expired_states(connection, int(time.time()))
        oauth.create_state(
            connection,
            state=state,
            provider=provider,
            redirect_uri=redirect_uri,
            expires_at=expires_at,
        )
    callback = f"{config['base']}/auth/oauth/{provider}/callback"
    query = {
        "client_id": config["id"],
        "redirect_uri": callback,
        "response_type": "code",
        "scope": config["scope"],
        "state": state,
    }
    if provider == "google":
        query["access_type"] = "online"
    if provider == "github":
        query["allow_signup"] = "true"
    return RedirectResponse(
        f"{config['authorize']}?{urlencode(query)}", status_code=302
    )


async def _provider_profile(
    provider: str, config: dict, code: str, callback: str
) -> tuple[str, str, str]:
    async with httpx.AsyncClient(timeout=15.0) as client:
        token_response = await client.post(
            config["token"],
            data={
                "client_id": config["id"],
                "client_secret": config["secret"],
                "code": code,
                "redirect_uri": callback,
                "grant_type": "authorization_code",
            },
            headers={"Accept": "application/json"},
        )
        if not token_response.is_success:
            raise HTTPException(status_code=502, detail="Could not exchange provider authorization.")
        access_token = token_response.json().get("access_token")
        if not isinstance(access_token, str) or not access_token:
            raise HTTPException(status_code=502, detail="Provider did not return an access token.")
        headers = {"Authorization": f"Bearer {access_token}", "Accept": "application/json"}
        if provider == "github":
            headers["User-Agent"] = "Oeil"
        profile_response = await client.get(config["profile"], headers=headers)
        if not profile_response.is_success:
            raise HTTPException(status_code=502, detail="Could not load provider account.")
        profile = profile_response.json()
        email = profile.get("email") or profile.get("mail") or profile.get("userPrincipalName")
        if provider == "github":
            email = None
            emails_response = await client.get(
                "https://api.github.com/user/emails", headers=headers
            )
            if not emails_response.is_success:
                raise HTTPException(status_code=502, detail="Could not verify the GitHub account email.")
            emails = emails_response.json()
            email = next(
                (
                    item["email"]
                    for item in emails
                    if item.get("verified") and item.get("primary")
                ),
                None,
            )
        if provider == "google" and profile.get("email_verified") is not True:
            raise HTTPException(status_code=403, detail="Verify your Google email before signing in.")
        if provider == "github" and not email:
            raise HTTPException(status_code=403, detail="A verified GitHub email is required.")
        provider_user_id = str(profile.get("sub") or profile.get("id") or "")
        name = profile.get("name") or profile.get("displayName") or profile.get("login") or email
        if not provider_user_id or not isinstance(email, str):
            raise HTTPException(status_code=502, detail="Provider returned an incomplete account profile.")
        return provider_user_id, email, str(name)


@router.get("/oauth/{provider}/callback")
async def oauth_callback(
    provider: str,
    code: str | None = None,
    state: str | None = None,
    error: str | None = None,
) -> RedirectResponse:
    if provider not in PROVIDERS:
        raise HTTPException(status_code=404, detail="Unsupported sign-in provider.")
    app_redirect = APP_REDIRECT_URI
    if error or not code or not state:
        return RedirectResponse(
            f"{app_redirect}?{urlencode({'error': 'Sign-in was cancelled or not authorized.'})}",
            status_code=302,
        )
    _session_secret()
    _initialize()
    with _connect() as connection:
        oauth.begin_immediate(connection)
        saved_state = oauth.find_state(connection, state, provider)
        if saved_state:
            oauth.delete_state(connection, state)
    if (
        saved_state is None
        or saved_state["expires_at"] <= int(time.time())
        or saved_state["redirect_uri"] != app_redirect
    ):
        return RedirectResponse(
            f"{app_redirect}?{urlencode({'error': 'Sign-in expired. Please try again.'})}",
            status_code=302,
        )
    config = _provider_config(provider)
    callback = f"{config['base']}/auth/oauth/{provider}/callback"
    try:
        provider_id, email, name = await _provider_profile(provider, config, code, callback)
        user = _get_or_create_oauth_user(provider, provider_id, email, name)
        token = _issue_session(user["id"])
    except httpx.RequestError as request_error:
        raise HTTPException(status_code=502, detail="Could not connect to the sign-in provider.") from request_error
    return RedirectResponse(f"{app_redirect}#token={token}", status_code=302)
