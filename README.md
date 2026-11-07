# Oeil

**An Android assistant that can actually use your phone — with your approval.**

Oeil is a Jetpack Compose Android app backed by a small Python relay. It chats
with a model API, and when you enable Android control it can read the visible
screen and perform one approved action at a time: open an app, tap a button,
type text, scroll, go back, and more.

## What it does

- **Chat assistant** with local conversation history.
- **Android control** through an Accessibility Service: open apps, tap controls,
  enter ordinary text, scroll, go Back, go Home, open recent apps, open
  notifications, and open links. Every action requires explicit approval.
- **Floating progress pill** while Oeil works in another app.
- **No API URL or key setup** in the app; the backend URL is compiled in.
- **Provider key stays on the backend**, never in the APK.
- **Accounts** with email/password and Google, Microsoft or GitHub sign-in.
- **Light, dark and system themes**, plus Auto, Precise and Creative reply modes.
- **Voice dictation**, stop generation and conversation search.

Supported model provider: **NVIDIA NIM AI** (OpenAI-compatible).

## The idea

Most assistants answer questions but cannot act on your phone. Fully automatic
phone control is powerful but also risky. Oeil takes a middle path:

1. The model proposes **one small action**.
2. Android shows **exactly what will happen**.
3. You approve or deny it.
4. The result is sent back, and Oeil decides what to do next.

The Accessibility Service reads the accessibility tree — labels, IDs, clickable/
editable flags, installed apps — **not screenshots**. Password fields are excluded.
If you deny an action, the task stops. Tasks stop after eight actions.

## See it in action

1. Ask **“Open Settings.”**
2. Oeil shows an approval card for the Settings app.
3. Tap **Allow** — Settings opens, the floating pill shows Oeil is working.
4. Ask **“Go home.”**
5. Allow the action — the phone returns to the launcher.
6. Deny an action to show that Oeil stops immediately.

## Tech stack

| Layer | Technology |
|---|---|
| Android app | Kotlin, Jetpack Compose, SQLite |
| Device control | Android Accessibility Service |
| Backend | Python 3.10+, FastAPI, Uvicorn |
| Model provider | NVIDIA NIM AI (OpenAI-compatible) |
| Build | Gradle 9.8, AGP 9.4, compileSdk 37 |

## Quick start

### 1. Run the backend

Requirements: Python 3.10+ and an NVIDIA API key.

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env

# Edit .env and set your NVIDIA API key:
#   NVIDIA_API_KEY=your-nvidia-key
# Optional model override:
#   NVIDIA_MODEL=nvidia/nemotron-3.5-lightning-30b-a3b
uvicorn main:app --host 0.0.0.0 --port 8000
```

Check it:

```bash
curl http://127.0.0.1:8000/health
```

### 2. Build and install the Android app

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open Oeil, then enable **Oeil Android control** in Android Accessibility
settings. Control turns on automatically once the service connects.

### 3. Point the app at your backend

The app has no URL field. The address is compiled in at build time:

```bash
# Default backend URL: https://oeil.onrender.com
./gradlew assembleDebug

# Real deployment
./gradlew assembleDebug -PoeilBackendUrl=https://oeil.example.com
# or
OEIL_BACKEND_URL=https://oeil.example.com ./gradlew assembleDebug
```

## Configuration

`backend/.env` is loaded automatically. Shell environment variables take
precedence over the file. A blank `NVIDIA_MODEL` falls back to the default (`nvidia/nemotron-3.5-lightning-30b-a3b`).

| Variable | Purpose |
|---|---|
| `NVIDIA_API_KEY` | Provider API key (required) |
| `NVIDIA_MODEL` | Model name |
| `NVIDIA_BASE_URL` | Provider base URL |
| `OEIL_AUTH_SECRET` | Stable secret for account sessions |
| `OEIL_AUTH_DB_PATH` | Optional path for the auth database |
| `OEIL_PUBLIC_BASE_URL` | Public HTTPS origin used for OAuth callbacks |

Accounts use email/password, Google, Microsoft or GitHub sign-in. Configure
provider OAuth secrets on the backend only. The app receives OAuth sessions
through `oeil://auth/callback`. Never put provider keys in the app or commit them
to Git.

## Safety and privacy

- Every Android action requires explicit approval.
- The model cannot enter passwords, payment details or security codes.
- Only accessible screen data is sent: labels, IDs, clickable/editable flags,
  and the launchable app list. Image bytes are not sent.
- Provider keys stay on the backend.
- Tasks stop after eight actions.
- The floating status pill only appears while Oeil is working in another app.
- Check current Google Play policy before release; accessibility use may be
  restricted.

## Current limitations

- Screenshot vision is not implemented; Oeil relies on accessible labels.
- No arbitrary touch gestures or gesture recording.
- No app-specific integrations.
- Add rate limiting and authentication before public deployment.

## Tests

```bash
cd backend && python3 -m unittest
./gradlew test
```

## Project layout

```text
app/src/main/java/com/example/oeil/
├── MainActivity.kt            # Single activity and auth deep link
├── OeilApp.kt                 # Profile, settings, account and screen wiring
├── model/Models.kt            # Messages, threads, actions and local profile
├── data/
│   ├── OeilApi.kt             # Relay, auth, agent clients and URL rules
│   └── OeilDatabase.kt        # SQLite profile, threads, messages and settings
├── state/OeilChatState.kt     # Conversation state and reply orchestration
├── ui/theme/Theme.kt          # Palette and typography
├── components/
│   ├── AppDrawer.kt           # Assistant-style sidebar
│   ├── ChatComponents.kt      # Messages, typing dots, prompts, touch surface
│   └── Composer.kt            # Header and composer
├── screen/
│   ├── OeilScreen.kt          # Chat layout, drawer and message list
│   └── AccountScreens.kt      # Settings, appearance, account and sign-in
└── system/OeilAccessibilityService.kt  # Android control, approvals, status pill
backend/
├── main.py                    # FastAPI relay to NVIDIA model provider and agent
├── auth.py                    # Password, bearer session and OAuth endpoints
└── test_main.py               # Backend contract and auth tests
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for data flow.
