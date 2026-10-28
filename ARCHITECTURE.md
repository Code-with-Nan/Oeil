# Architecture

Oeil follows a **single-activity, unidirectional Compose architecture**. There is no navigation
graph yet — the app is one screen — so the emphasis is on a clean separation between
*state*, *behaviour*, and *presentation*.

---

## 1. High-level diagram

```
┌──────────────────────────────────────────────────────────────┐
│ MainActivity                                                  │
│   setContent { OeilTheme { OeilScreen() } }                   │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│ screen/OeilScreen  (composition root of the UI)               │
│   • owns remember { OeilChatState() }                          │
│   • owns rememberLazyListState() + rememberCoroutineScope()    │
│   • assembles Scaffold(topBar, content, bottomBar)             │
└───────┬──────────────────┬───────────────────┬────────────────┘
        │                  │                   │
        ▼                  ▼                   ▼
  components/ChatHeader  components/    components/ChatInput
  components/EmptyState  MessageItem    (composer, morphing send)
                        components/
                        TypingIndicator + AccentPanel
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ state/OeilChatState  (behaviour)                              │
│   messages: SnapshotStateList<ChatMessage>                    │
│   input / isTyping: mutableStateOf                            │
│   send(scope) ──launch──▶ delay ──▶ ReplySource.answer()      │
│                                     ▲ (Java) MockReplySource  │
└──────────────────────────────────────────────────────────────┘
```

## 2. Layers

| Layer      | Package                          | Responsibility                                                             |
|------------|----------------------------------|----------------------------------------------------------------------------|
| Entry      | *(root)* `MainActivity`          | Edge-to-edge setup, theme + screen host. No business logic.                |
| Screen     | `screen`                         | Layout composition only. Wires state to components.                        |
| State      | `state`                          | Holds conversation state and mutates it in response to user events.        |
| Components | `components`                     | Stateless (or nearly stateless) composables, reusable across screens.      |
| Data       | `data` (**Java**)               | `ReplySource` interface + `MockReplySource`. The swap point for a real backend. |
| Model      | `model`                          | Plain immutable data classes.                                              |
| Theme      | `ui.theme`                       | Palette, typography and `OeilTheme`.                                       |

**Dependency direction** is strictly downward:
`screen → state/components → data/model → ui.theme`. Nothing in `data`, `model` or `ui.theme`
imports from `screen` or `components`.

## 2.1 Kotlin / Java split

Both languages live in the same source set and compile together, so either side can call the
other. The split is by concern, not by convenience:

| Written in | Packages                     | Why                                                                 |
|------------|------------------------------|---------------------------------------------------------------------|
| Kotlin     | `screen`, `components`, `state`, `model`, `ui.theme` | Compose, coroutines, `data class` and sealed types are Kotlin-only. |
| Java       | `data`                       | The backend seam: SDK/HTTP clients in the Android ecosystem are commonly Java, and an interface with `@NonNull`/`@Nullable` gives both call sites a precise contract. |

Kotlin → Java happens in one place, `OeilChatState(replySource)`, and because `ReplySource` is a
blocking API the call is wrapped in `withContext(Dispatchers.IO)`.
Java → Kotlin would work the same way, with the usual caveats: Java sees Kotlin `internal` members
as public, cannot see top-level functions (they compile to `FileNameKt`), and cannot use default
arguments without `@JvmOverloads`.

## 3. State management

State lives in `OeilChatState`, a plain class created with `remember` at the top of
`OeilScreen`. It is deliberately **not** a `ViewModel` yet:

- The app has no configuration-change survival requirement (no process death handling yet).
- Keeping it a plain class avoids adding `lifecycle-viewmodel-compose` for a single screen.
- The class is UI-framework-agnostic enough to migrate to a `ViewModel` later by changing only
  the construction site in `OeilScreen`.

```kotlin
val chat = remember { OeilChatState() }
```

### State properties

| Property    | Type                              | Notes                                   |
|-------------|-----------------------------------|-----------------------------------------|
| `messages`  | `SnapshotStateList<ChatMessage>`  | Append-only history; drives `LazyColumn` |
| `input`     | `String` (private set)            | Composer text                            |
| `isTyping`  | `Boolean` (private set)           | Gates send + shows `TypingIndicator`     |

### Message flow

```
user taps send
   │
   ├─ OeilChatState.send(scope)
   │     1. validate: non-blank && !isTyping
   │     2. messages += ChatMessage(isUser = true)
   │     3. input = "" ; isTyping = true
   │     4. scope.launch {
   │            delay(1_100 ms)                 // simulated latency
   │            withContext(Dispatchers.IO) { replySource.answer(text) }
   │            messages += ChatMessage(reply, isUser = false)
   │            isTyping = false
   │        }
   │
   ├─ OeilScreen LaunchedEffect(messages.size, isTyping)
   │        → listState.animateScrollToItem(latestIndex)
   │
   └─ recomposition renders MessageItem / TypingIndicator
```

`send` takes an explicit `CoroutineScope` (`rememberCoroutineScope()` from the composable) so the
delay coroutine is cancelled automatically if the screen leaves composition.

## 4. Screen composition

`OeilScreen` is split into small private composables so each piece reads top-to-bottom:

| Composable            | Role                                                                  |
|-----------------------|-----------------------------------------------------------------------|
| `OeilScreen`          | Gradient background, ambient glow, Scaffold wiring                    |
| `AmbientGlow`         | Decorative radial gradient behind the header                          |
| `AutoScrollToLatest`  | `LaunchedEffect` that follows the newest item                         |
| `Conversation`        | `LazyColumn` + empty-state overlay                                    |

Public UI building blocks live in `components/` and take their behaviour as lambdas
(`onSend`, `onValueChange`, `onPick`), which keeps them previewable and reusable.

## 5. Theming

- **Dark-only palette** defined as top-level `Color` vals in `ui/theme/Color.kt`
  (e.g. `GreenPrimary #B6F35C`, `TealAccent #2FD4B0`, `BackgroundDark #040806`).
- `OeilTheme` maps those vals onto a Material 3 `darkColorScheme` and provides the typography.
- Components reference theme colors **directly** (not `MaterialTheme.colorScheme.*`) for
  gradients and one-off accents; `MaterialTheme.typography` is used for text styles.

## 6. Data layer (Java)

```
data/
├── ReplySource.java      # interface  String answer(String query)   ← the seam
└── MockReplySource.java  # offline canned answers (current behaviour)
```

`ReplySource` is the single seam between the UI and a future backend. It is deliberately a plain
blocking Java interface: nothing Compose- or coroutine-specific leaks into it, so it can be
implemented (and unit tested) by Java-only code.

Swapping in a real client means:

1. Add `ApiReplySource implements ReplySource` that calls the SDK/HTTP client.
2. Keep `MockReplySource` behind a debug/preview flag.
3. Pass the chosen implementation to `OeilChatState(replySource)` — the default is already
   `MockReplySource`.

```kotlin
// OeilChatState.kt
class OeilChatState(
    private val replySource: ReplySource = MockReplySource()
)
```

No persistence exists yet; `ChatMessage` is in-memory only (`id` is a UUID, used as the
`LazyColumn` key).

## 7. Build & quality

- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- Compose compiler plugin applied via `kotlin-compose`.
- Release builds enable R8 optimisation with a `packageScope` allow-list; project-specific
  keep rules live in `app/src/main/keepRules/rules.keep`.
- Java 11 source/target compatibility for both the Kotlin and Java compilation tasks.

## 8. Extension points

| Goal                      | Where to change                                              |
|---------------------------|--------------------------------------------------------------|
| Real model responses      | Add `ApiReplySource implements ReplySource` in `data/`       |
| Persist history           | Add a repository; back `OeilChatState.messages` with it      |
| Multiple screens/navigation | Add `navigation-compose` and move `OeilScreen` into a graph |
| Light theme               | Add a light `ColorScheme` in `Theme.kt` and honour `darkTheme` |
| View model migration      | Construct `OeilChatState` in a `viewModel { }` instead of `remember` |
