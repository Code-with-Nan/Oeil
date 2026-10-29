# Oeil

**Oeil** (French for *eye*) is an Android chat assistant built entirely with Jetpack Compose.
It is just like another AI models except, its different as well. You can chat with it, and create histories if you want like other applications
but you also can ask it do any task. Just by chatting to it, you can make it do any task, as the way you want. It has the ability to interfere with the system while being a fully functional AI Chat.
> Status: working prototype. Chat replies are proxied through the bundled Node/Express
> backend, so the Gemini key stays on the server. Accounts, sessions and conversation
> history are stored locally in SQLite.

---

## Features

- **Header** — flat row of plain controls: menu, wordmark, answer-style selector
  (Auto → Precise → Creative) and new chat. No status indicators.
- **Conversation view** — LazyColumn history: right-aligned neutral bubbles for you,
  plain left-aligned text for Oeil. Long-press either to copy it.
- **Typing indicator** — three quiet neutral dots while a reply is pending, with a stop
  control in the composer to abandon it.
- **Landing view** — one plain greeting and three prompts that pre-fill the composer and
  raise the keyboard in the same tap.
- **Composer** — multi-line input (up to 5 lines) whose trailing control answers to
  state: a quiet mic, a solid send disc once there's text, a stop square while a
  reply is in flight.
- **Real system intents** — the mic runs `RecognizerIntent` dictation into the draft;
  `+` opens the photo picker and attaches the chosen image.
- **Keyboard-aware** — the composer rides above the IME and the conversation shrinks
  upwards, so the keyboard never covers what you just typed.
- **Sign in** — email/password login against the bundled backend, with a local demo
  fallback when no `LOGIN_API_URL` is set. New accounts are created on first sign-in,
  and the session survives relaunches.
- **Gemini replies** — the app sends the whole conversation to the backend `/chat`
  endpoint, which calls Gemini with the server-side key. Follow-up questions keep their
  context; Auto/Precise/Creative map to system prompts and temperatures.
- **Local storage** — accounts, sessions, threads, messages and settings live in SQLite
  (`oeil.db`); history is restored per account on launch.
- **Conversation history** — the drawer lists real threads, filters them as you search,
  reopens one on tap and clears all of them on request.
- **Press feedback** — every tappable control ripples, dims and lifts on touch.
- **Edge-to-edge** — status, navigation-bar and IME insets composed with
  `windowInsetsPadding()`.

## Tech stack

| Layer        | Choice                                          |
|--------------|-------------------------------------------------|
| Language     | Kotlin 2.2.10 (UI/state) + Java 11 (interop seam) |
| UI           | Jetpack Compose (BOM 2026.02.01), Material 3    |
| Data         | Node/Express backend (Gemini REST, JWT) + SQLite |
| Min / Target | API 24 / API 37                                 |
| Build        | Gradle Kotlin DSL, AGP 9.4.1                    |
| Icons        | `material-icons-extended`                       |

## Project structure

```
app/src/main/java/com/example/oeil/
├── MainActivity.kt          # Entry point, sets OeilTheme + OeilApp
├── OeilApp.kt               # Session gate: boot, login or chat
├── screen/
│   ├── LoginScreen.kt       # Email/password sign-in
│   └── OeilScreen.kt        # Chat: drawer, scaffold, auto-scroll, notices
├── state/
│   └── OeilChatState.kt     # Messages, threads, input, typing + persistence
├── data/
│   ├── AuthRepository.kt    # Login API with local demo fallback
│   ├── BackendReplySource.kt # Authenticated client for the backend /chat endpoint
│   ├── ConversationReplySource.kt
│   ├── MockConversationReplySource.kt
│   ├── ReplySource.java     # Legacy Java reply seam (mock adapter)
│   ├── MockReplySource.java # Offline canned answers
│   └── local/
│       └── OeilDatabase.kt  # SQLite: users, session, threads, messages, settings
├── components/              # Reusable UI pieces
│   ├── AppDrawer.kt         # History, search, clear, account + sign out
│   ├── ChatHeader.kt        # Brand mark, reply-mode pill, new chat
│   ├── ChatInput.kt         # Composer, voice and photo-picker intents
│   ├── EmptyState.kt        # Time-aware greeting and prompt rows
│   ├── MessageItem.kt       # Bubbles, attachment labels, long-press copy
│   ├── OeilTouch.kt         # One press-feedback path for every control
│   └── TypingIndicator.kt   # Pulsing pending-reply row
├── model/
│   ├── Attachment.kt        # Picked image (uri + display label)
│   ├── ChatMessage.kt       # Immutable message data class
│   ├── ChatThread.kt        # Saved conversation with title and recency
│   ├── ReplyMode.kt         # Auto / Precise / Creative
│   └── User.kt              # Signed-in account
└── ui/theme/                # Colors, typography, OeilTheme
```

The `backend/` folder contains the Express server, JWT auth, JSON user store and the
server-side Gemini client.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the design rationale and data flow.

## Kotlin + Java

Both languages compile in the same module (no extra Gradle config). The split:

- **Kotlin** owns everything Compose, coroutines, state and the live data layer:
  `screen/`, `components/`, `state/`, `model/`, `ui/theme/`, plus `AuthRepository`,
  `BackendReplySource` and `OeilDatabase`.
- **Java** keeps the original offline reply seam: `data/ReplySource.java` and
  `data/MockReplySource.java`. `MockConversationReplySource` adapts it to the
  history-aware Kotlin interface used by tests and previews.

The app talks to the backend through `BackendReplySource`; the backend owns the Gemini
key and calls the Gemini REST API. The mock is no longer the default in the running app.

## Getting started

### Prerequisites

- Android Studio (Ladybug or newer) with JDK 11+
- Android SDK 37

### Configuration

The Android app only needs to know where the backend is. Add these to
`local.properties`, or export them as environment variables / Gradle properties before
building:

```properties
LOGIN_API_URL=http://10.0.2.2:3000/login
CHAT_API_URL=http://10.0.2.2:3000/chat
```

- **`LOGIN_API_URL`** — optional. When blank, login runs in local demo mode (any email,
  password of 4+ characters). When set, the app POSTs `{"email","password"}` and reads
  `token`/`access_token`/`accessToken`/`id_token` plus `id`/`userId`/`user_id`,
  `name`/`displayName` from the JSON response.
- **`CHAT_API_URL`** — optional. Defaults to `LOGIN_API_URL` with `/login` replaced by
  `/chat`. The app POSTs the conversation with the session token and reads `reply`.

The app values are compiled into `BuildConfig`, so rebuild after changing them. The
Gemini key is never compiled into the APK.

The backend reads its own configuration from `backend/.env`:

```properties
GEMINI_API_KEY=your-key
GEMINI_MODEL=gemini-3.5-flash-lite
JWT_SECRET=generate-a-long-random-secret
```

- **`GEMINI_API_KEY`** — required for live replies; stays on the server.
- **`GEMINI_MODEL`** — optional; defaults to `gemini-3.5-flash-lite`. Use
  `gemini-3.8-flash` when it is not under high demand.
- **`JWT_SECRET`** — signs session tokens; use a long random value.

### Local Node backend

A small Express server lives in `backend/`. It implements `POST /login`, `POST /register`,
`GET /me` and the authenticated `POST /chat` proxy. It stores users in
`backend/data/users.json`, hashes passwords with bcrypt, and keeps the Gemini key
server-side.

```bash
cd backend
npm install
cp .env.example .env   # then set GEMINI_API_KEY and a real JWT_SECRET
npm start
```

Point the app at it from `local.properties`:

```properties
LOGIN_API_URL=http://10.0.2.2:3000/login
CHAT_API_URL=http://10.0.2.2:3000/chat
```

`10.0.2.2` is the Android emulator's alias for your computer. For a physical device use
an HTTPS tunnel, or add your machine's LAN IP to
`app/src/debug/res/xml/network_security_config.xml`. Debug builds allow cleartext HTTP
only for the emulator loopback; release builds block it. The server creates an account
on first login so the app works without a separate sign-up screen.

### Build & run

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug

# Unit tests
./gradlew test
```

Or open the project in Android Studio and press **Run**.

## Documentation

| Document                          | Contents                                    |
|-----------------------------------|---------------------------------------------|
| [PLAN.md](PLAN.md)                  | Plain-English plan for real models + phone control |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layers, data flow, state and theming design |
| [CONTRIBUTION.md](CONTRIBUTION.md) | Workflow, style rules, PR checklist         |
| [LICENSE](LICENSE)                 | Apache 2.0                                  |


## License

Licensed under the [Apache License 2.0](LICENSE).
