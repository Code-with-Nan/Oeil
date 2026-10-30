# Architecture

Oeil is a single-activity Android app. Conversation history, account session and
appearance preference are stored locally in SQLite. The app supports a local
profile as well as a backend account. The relay URL is compiled in at build time.

## Reply flow

```text
OeilScreen
  └── OeilChatState
        └── ConversationReplySource
              └── BackendReplySource
                    └── POST {backend URL}/chat
                          └── Python FastAPI backend
                                                                              └── Gemini generateContent

Android control path:

```text
OeilChatState (maximum 8 steps)
      ├── OeilAccessibility.screenElements()
      ├── OeilAccessibility.installedApps()
      ├── POST {backend URL}/agent/step
      │     └── Gemini function calling (fixed action declarations)
      ├── OeilAccessibility.approveAndPerform()
      │     └── Android Accessibility approval overlay
      └── OeilAccessibility.updateTaskStatus()
            └── Floating progress banner while another app is in front
```

The Android client sends the last 20 text messages, the selected response mode,
visible screen elements, and the launchable app list. It does not send an account
token or provider API key. The backend adds a mode-specific system prompt and keeps
`GEMINI_API_KEY` on the server. The relay URL is configured at build time, not
in the UI; an unconfigured build or missing provider key produces an explicit
error, and the running app never silently falls back to mock replies.

## Main pieces

| Area | Responsibility |
|---|---|
| `OeilApp` | Loads the local profile, build-time service URL and settings; opens chat or settings |
| `OeilScreen` | Chat layout, drawer, message list and composer |
| `SettingsScreen` | Appearance, account and backend settings |
| `AuthScreen` | Email/password sign-up and sign-in; launches provider OAuth |
| `AuthClient` | Calls account endpoints and validates OAuth callback sessions |
| `OeilChatState` | Message/thread state, reply lifecycle and local persistence |
| `BackendReplySource` | JSON HTTP client for the Python `/chat` endpoint |
| `OeilDatabase` | SQLite local profile, threads, messages and settings |
| `backend/auth.py` | Password hashing, revocable sessions and provider OAuth |
| `backend/main.py` | Validates chat and agent requests and calls Gemini |

`MockConversationReplySource` remains available for unit tests; it is not wired
into the installed app.

## Backend API

`POST /chat` accepts a JSON object with `mode` (`AUTO`, `PRECISE`, or
`CREATIVE`) and a non-empty list of `{role, content}` messages. `role` is
`user` or `assistant`. Successful responses are `{ "reply": "..." }`.
`GET /health` checks that the relay is running and reports the configured model
and whether a provider key is present.

Account endpoints are `POST /auth/signup`, `POST /auth/login`, `GET /auth/me`,
and `POST /auth/logout`. Passwords are PBKDF2-hashed, and sessions are signed
bearer tokens backed by revocable SQLite records. `GET /auth/oauth/{provider}/start`
begins Google, Microsoft or GitHub authorization; the backend handles the
provider callback and returns the session to the app through
`oeil://auth/callback`. Configure `OEIL_AUTH_SECRET`, `OEIL_PUBLIC_BASE_URL`, and
provider client credentials on the server. OAuth requires a public HTTPS backend.

Configuration is read from the backend process environment:

- `GEMINI_API_KEY` is required and must never be placed in Android resources,
  Gradle properties, or the APK.
- `GEMINI_API_BASE_URL` defaults to `https://generativelanguage.googleapis.com/v1beta`.
- `GEMINI_MODEL` defaults to `gemini-3.5-flash`.

`POST /agent/step` accepts a task, the recent conversation, up to 100 visible
screen elements, and up to 300 launchable apps. Gemini may return one of nine
declared actions (`tap`, `type`, `scroll`, `back`, `home`, `recents`,
`notifications`, `open_app`, `open_url`) or a final message. Apps can be selected
by package name or visible label. The app re-reads the screen between steps and
stops after eight actions. Every action is shown in an Android overlay and must
be approved; the accessibility service is never enabled automatically.

`/chat` remains guest-accessible and is not tied to account sessions. Before
public deployment, enable HTTPS and configure request rate limiting; enforce
bearer auth on `/chat` if guest access is not intended.

## Data and privacy

Conversations and appearance preferences are stored in the app's local SQLite
database. The service URL is compiled into the APK. Text conversation history
and, when Android control is active, visible screen labels and the launchable app
list are sent to the relay. The current attachment flow stores and displays a
label; image bytes are not sent to the model. Screenshot vision and arbitrary
gestures are not implemented.
