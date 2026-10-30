# Oeil

Oeil is an Android chat assistant built with Jetpack Compose. It provides local
conversation history, Gemini-powered replies through a Python relay, and an
optional Android Accessibility Service for user-approved screen actions.

## What works today

- Compose chat UI with conversation history stored locally in SQLite.
- Account settings with email/password sign-up and sign-in, plus Google,
  Microsoft and GitHub OAuth sign-in.
- Light, dark and system appearance options saved on the device.
- Auto, Precise and Creative response modes.
- A Python FastAPI relay that calls Gemini and keeps the provider key off-device.
- Optional Android control for reading visible screen labels and opening apps,
  tapping controls, entering ordinary text, scrolling, navigating Back, going
  home, opening recent apps, opening notifications, and opening links. Each
  action requires approval in an Android system overlay; tasks stop after eight
  actions. Accessibility control is opt-in in Android Settings.
- The app is pre-pointed at an Oeil service at build time, so users never enter
  an API URL or key. The provider key stays on the backend and is never compiled
  into or sent from the APK.
- Text replies, stop generation, conversation search, and voice dictation.

## Run the Python backend

Requirements: Python 3.10+. A Gemini API key is required for chat
replies; account endpoints work independently.

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # or create .env with GEMINI_API_KEY=...
# GEMINI_API_KEY=your-Gemini-key
# OEIL_AUTH_SECRET=$(openssl rand -hex 32)
uvicorn main:app --host 0.0.0.0 --port 8000
```

`backend/.env` is loaded automatically. Shell environment variables take
precedence over the file. A blank `GEMINI_MODEL` falls back to the working
default.

The auth database is stored in `backend/oeil-auth.sqlite3` by default. Set
`OEIL_AUTH_DB_PATH` to choose another persistent location. `OEIL_AUTH_SECRET`
must be a stable random secret of at least 32 characters; changing it invalidates
existing sessions.

To enable provider sign-in, serve the backend over HTTPS, set
`OEIL_PUBLIC_BASE_URL` to its public origin, and configure each provider's client
ID and secret on the backend only:

| Provider | Environment variables | Provider callback URL |
|---|---|---|
| Google | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | `{OEIL_PUBLIC_BASE_URL}/auth/oauth/google/callback` |
| Microsoft | `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` | `{OEIL_PUBLIC_BASE_URL}/auth/oauth/microsoft/callback` |
| GitHub | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | `{OEIL_PUBLIC_BASE_URL}/auth/oauth/github/callback` |

The Android app receives OAuth sessions through the `oeil://auth/callback`
application link. Do not put provider secrets in the Android app or commit them.

The backend defaults to the Gemini API and model:

- API base URL: `https://generativelanguage.googleapis.com/v1beta`
- Model: `gemini-3.5-flash`

Override with `GEMINI_API_BASE_URL` or `GEMINI_MODEL` if needed. `GET /health`
checks the relay and reports the configured model; `POST /chat` serves normal
chat and `POST /agent/step` returns one bounded Android action or a final text
response.

**Use HTTPS and configure `OEIL_AUTH_SECRET` before deploying publicly.** The
email/password and OAuth account endpoints issue revocable bearer sessions.
`/chat` remains available to local/guest profiles and does not currently require
an account token; add rate limiting and enforce authentication before exposing a
production service.

## Point the app at the backend

The app has no API/URL field. Its service address is compiled in from the
`oeilBackendUrl` Gradle property, `OEIL_BACKEND_URL`, or the default
`http://10.0.2.2:8000` (Android emulator loopback):

```bash
./gradlew assembleDebug -PoeilBackendUrl=https://oeil.example.com
# or
OEIL_BACKEND_URL=https://oeil.example.com ./gradlew assembleDebug
```

Keep `GEMINI_API_KEY` on the backend; never put it in the Android app. To control
other apps, enable **Oeil Android control** in the system Accessibility settings.
Android control turns on automatically once the service is connected, and the
phone icon in the chat header toggles it. Screen labels and the installed-app list
are sent to the backend while Android control is active. While a task runs outside
Oeil, a small floating progress banner shows what Oeil is currently doing.

This is not unrestricted control: Android only exposes accessible screen content,
secure fields are excluded, unsupported actions are not available, and every
action displays its exact target/content for approval. Vision screenshots,
gesture recording, and app-specific integrations are not implemented.
Accessibility use for general assistant automation may not meet Google Play
policy requirements; check current store policy before release.

## Android build

Requirements: Android Studio with a full JDK 17+ (one that includes `jlink`)
and Android SDK 37. The Gradle daemon is pinned to Oracle JDK 25; Gradle
downloads it automatically on the first build if it is not installed.

```bash
./gradlew assembleDebug
./gradlew test
```

For emulator local HTTP, the debug network security config permits
`10.0.2.2`. Release builds require HTTPS.

## Backend tests

```bash
cd backend
pip install -r requirements.txt
python3 -m unittest
```

## Project layout

```text
app/src/main/java/com/example/oeil/
├── OeilApp.kt                 # Local profile, settings, account and dependency wiring
├── data/                      # Relay/auth clients, endpoint validation, SQLite store
├── screen/                    # Chat, appearance, account and settings screens
├── state/                     # Conversation state and reply orchestration
├── system/                    # Android Accessibility Service and approvals
├── components/                # Chat UI components
└── model/                     # Messages, threads, and local profile
backend/
├── main.py                    # FastAPI relay to Gemini and the Android agent
├── auth.py                    # Password, bearer session and OAuth endpoints
└── test_main.py               # Backend contract and auth tests
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the current data flow and
[PLAN.md](PLAN.md) for the planned AAS and vision work.
