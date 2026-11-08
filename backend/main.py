import asyncio

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app import config
from app.prompts import (
    AGENT_FUNCTIONS,
    AGENT_INSTRUCTIONS,
    AGENT_STATE_TEMPLATE,
    SYSTEM_PROMPTS,
)
from app.schemas import (
    ActionType,
    AgentAction,
    AgentStepRequest,
    ChatMessage,
    ChatRequest,
    InstalledApp,
    Mode,
    Role,
    ScreenElement,
)
from auth import router as auth_router

app = FastAPI(title="Oeil API", version="1.1.0")
app.include_router(auth_router)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Content-Type", "Authorization"],
)

MAX_HISTORY_MESSAGES = 20

TEMPERATURES = {"AUTO": 0.7, "PRECISE": 0.2, "CREATIVE": 1.0}


def gemini_payload(request: ChatRequest) -> dict:
    return {
        "systemInstruction": {"parts": [{"text": SYSTEM_PROMPTS[request.mode]}]},
        "contents": [
            {
                "role": "model" if message.role == "assistant" else "user",
                "parts": [{"text": message.content}],
            }
            for message in request.messages
        ],
        "generationConfig": {
            "temperature": TEMPERATURES[request.mode],
            "maxOutputTokens": 2048,
        },
    }


def agent_payload(request: AgentStepRequest) -> dict:
    screen = [element.model_dump() for element in request.screen]
    apps = [app.model_dump() for app in request.apps]
    state = AGENT_STATE_TEMPLATE.format(
        task=request.task,
        apps=apps,
        screen=screen,
    )

    contents = [
        {
            "role": "model" if message.role == "assistant" else "user",
            "parts": [{"text": message.content}],
        }
        for message in request.messages[-MAX_HISTORY_MESSAGES:]
    ]
    if contents and contents[-1]["role"] == "user":
        contents[-1]["parts"].append({"text": state})
    else:
        contents.append({"role": "user", "parts": [{"text": state}]})

    return {
        "systemInstruction": {"parts": [{"text": AGENT_INSTRUCTIONS}]},
        "contents": contents,
        "tools": [{"functionDeclarations": AGENT_FUNCTIONS}],
        "toolConfig": {"functionCallingConfig": {"mode": "AUTO"}},
        "generationConfig": {
            "temperature": TEMPERATURES[request.mode],
            "maxOutputTokens": 2048,
        },
    }


def gemini_url() -> str:
    base_url = config.gemini_api_base_url().rstrip("/")
    model = config.gemini_model()
    if not model or "/" in model or " " in model:
        raise HTTPException(status_code=503, detail="GEMINI_MODEL is invalid.")
    return f"{base_url}/models/{model}:generateContent"


async def call_gemini(payload: dict) -> dict:
    api_key = config.gemini_api_key()
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="Gemini is not configured. Set GEMINI_API_KEY in backend/.env.",
        )

    url = gemini_url()
    last_status: int | None = None
    async with httpx.AsyncClient(timeout=120.0) as client:
        for attempt in range(3):
            try:
                response = await client.post(
                    url,
                    headers={"x-goog-api-key": api_key},
                    json=payload,
                )
            except httpx.RequestError as error:
                if attempt == 2:
                    raise HTTPException(
                        status_code=502,
                        detail="Could not connect to the Gemini API. Check the backend network.",
                    ) from error
                await asyncio.sleep(1.5 * (attempt + 1))
                continue

            if response.is_success:
                try:
                    return response.json()
                except ValueError as error:
                    raise HTTPException(
                        status_code=502, detail="Gemini returned an unreadable response."
                    ) from error

            last_status = response.status_code
            if response.status_code == 404:
                raise HTTPException(
                    status_code=502,
                    detail=(
                        "The configured Gemini model is unavailable. Set GEMINI_MODEL "
                        f"to a supported model (default: {config.DEFAULT_MODEL})."
                    ),
                )
            if response.status_code in (401, 403):
                raise HTTPException(
                    status_code=502,
                    detail="Gemini rejected the API key. Check GEMINI_API_KEY on the backend.",
                )
            if response.status_code in (429, 500, 502, 503, 504) and attempt < 2:
                await asyncio.sleep(1.5 * (attempt + 1))
                continue
            break

    raise HTTPException(
        status_code=502,
        detail=f"Gemini API request failed (HTTP {last_status}). Please try again.",
    )


def response_parts(response: dict) -> list[dict]:
    try:
        candidate = response["candidates"][0]
        parts = candidate["content"]["parts"]
        if not isinstance(parts, list):
            raise TypeError
        return parts
    except (KeyError, IndexError, TypeError) as error:
        reason = ""
        try:
            reason = response["candidates"][0].get("finishReason", "")
        except (KeyError, IndexError, TypeError):
            reason = response.get("promptFeedback", {}).get("blockReason", "")
        detail = "Gemini returned an unreadable response."
        if reason:
            detail += f" (reason: {reason})"
        raise HTTPException(status_code=502, detail=detail) from error


@app.get("/health")
async def health() -> dict:
    return {
        "status": "ok",
        "model": config.gemini_model(),
        "ai_configured": bool(config.gemini_api_key()),
    }


@app.post("/chat")
async def chat(request: ChatRequest) -> dict[str, str]:
    response = await call_gemini(gemini_payload(request))
    reply = "".join(
        part.get("text", "")
        for part in response_parts(response)
        if isinstance(part, dict)
    )
    if not reply.strip():
        raise HTTPException(status_code=502, detail="Gemini returned an empty reply.")
    return {"reply": reply.strip()}


@app.post("/agent/step")
async def agent_step(request: AgentStepRequest) -> dict:
    response = await call_gemini(agent_payload(request))
    parts = response_parts(response)
    for part in parts:
        if not isinstance(part, dict):
            continue
        call = part.get("functionCall")
        if not isinstance(call, dict):
            continue
        name = call.get("name")
        args = call.get("args")
        if (
            name not in {function["name"] for function in AGENT_FUNCTIONS}
            or not isinstance(args, dict)
        ):
            raise HTTPException(status_code=502, detail="Gemini returned an unsupported action.")
        try:
            action = AgentAction.model_validate({**args, "type": name})
        except ValueError as error:
            raise HTTPException(status_code=502, detail="Gemini returned an invalid action.") from error

        required_fields = {
            "tap": ("target_id", "target_label"),
            "type": ("target_id", "target_label", "text"),
            "scroll": ("direction",),
            "open_url": ("url",),
        }.get(name, ())
        if any(not getattr(action, field) for field in required_fields):
            raise HTTPException(status_code=502, detail="Gemini returned an invalid action.")
        if name == "open_app" and not (action.package_name or action.app_name):
            raise HTTPException(status_code=502, detail="Gemini returned an invalid action.")
        if name == "open_url" and not action.url.lower().startswith(("http://", "https://")):
            raise HTTPException(status_code=502, detail="Gemini returned an unsafe link.")
        return {"type": "action", "action": action.model_dump(exclude_none=True)}

    reply = "".join(
        part.get("text", "")
        for part in parts
        if isinstance(part, dict)
    ).strip()
    if not reply:
        raise HTTPException(status_code=502, detail="Gemini returned an empty agent step.")
    return {"type": "message", "message": reply}
