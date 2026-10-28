# Oeil

**Oeil** (French for *eye*) is an Android chat assistant built entirely with Jetpack Compose.
It is just like another AI models except, its different as well. You can chat with it, and create histories if you want like other applications
but you also can ask it do any task. Just by chatting to it, you can make it do any task, as the way you want. It has the ability to interfere with the system while being a fully functional AI Chat.
> Status: UI-complete prototype. Replies are generated locally by the Java `MockReplySource` —
> no network calls are made.

---

## Features

- **Header band** — solid brand bar with a pulsing "Ready" status pill, new-chat and
  profile actions, closed by a gradient hairline.
- **Conversation view** — LazyColumn history with two visual treatments: gradient pills for
  you, accent-railed callout panels for Oeil.
- **Typing indicator** — same callout shell as a real answer, with a staggered three-dot
  animation while a reply is pending.
- **Prompt carousel** — the landing view greets you by time of day and offers four
  horizontally scrolling prompt cards that pre-fill the composer.
- **Composer** — multi-line input (up to 5 lines); the send button expands from a quiet
  circle into a gradient "Send" pill the moment there's text.
- **New chat** — clears the thread and cancels any reply still in flight.
- **Auto-scroll** — the list follows the newest message automatically.
- **Edge-to-edge** — status/navigation bar insets handled with `statusBarsPadding()` / `navigationBarsPadding()`.

## Tech stack

| Layer        | Choice                                          |
|--------------|-------------------------------------------------|
| Language     | Kotlin 2.2.10 (UI/state) + Java 11 (interop seam) |
| UI           | Jetpack Compose (BOM 2026.02.01), Material 3    |
| Min / Target | API 24 / API 37                                 |
| Build        | Gradle Kotlin DSL, AGP 9.4.1                    |
| Icons        | `material-icons-extended`                       |

## Project structure

```
app/src/main/java/com/example/oeil/
├── MainActivity.kt          # Entry point, sets OeilTheme + OeilScreen
├── screen/
│   └── OeilScreen.kt        # Root screen: layout, scaffold, auto-scroll
├── state/
│   └── OeilChatState.kt     # Conversation state holder (messages, input, typing)
├── data/                    # Reply engine (Java interop seam)
│   ├── ReplySource.java     # Interface a real backend SDK plugs into
│   └── MockReplySource.java # Offline canned answers
├── components/              # Reusable UI pieces
│   ├── ChatHeader.kt        # Header band: brand, status pill, quick actions
│   ├── ChatInput.kt         # Composer with morphing send button
│   ├── EmptyState.kt        # Time-aware greeting + prompt carousel
│   ├── AccentPanel.kt       # Shared callout shell for everything Oeil says
│   ├── MessageItem.kt       # Bubbles, callouts and avatars
│   └── TypingIndicator.kt   # Animated pending-reply callout
├── model/
│   └── ChatMessage.kt       # Immutable message data class
└── ui/theme/                # Colors, typography, OeilTheme
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the design rationale and data flow.

## Kotlin + Java

Both languages compile in the same module (no extra Gradle config). The split:

- **Kotlin** owns everything Compose, coroutines and state: `screen/`, `components/`,
  `state/`, `model/`, `ui/theme/`.
- **Java** owns the backend seam: `data/ReplySource.java` is the interface a Java/Kotlin
  SDK implements, `data/MockReplySource.java` is the current offline implementation.
  Kotlin calls it from `OeilChatState` on `Dispatchers.IO` because the contract is blocking.

Swap in a real backend by writing a second `ReplySource` implementation — no Compose code changes.

## Getting started

### Prerequisites

- Android Studio (Ladybug or newer) with JDK 11+
- Android SDK 37

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
