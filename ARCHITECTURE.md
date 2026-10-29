# Architecture

Oeil follows a **single-activity, unidirectional Compose architecture**. There is no navigation
library yet: `OeilApp` switches between a sign-in gate and the chat screen based on the stored
session. The emphasis is on a clean separation between *state*, *behaviour*, *data* and
*presentation*.

---

## 1. High-level diagram

```
┌──────────────────────────────────────────────────────────────┐
│ MainActivity                                                  │
│   setContent { OeilTheme { OeilApp() } }                      │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│ OeilApp  (composition root + session gate)                    │
│   • owns OeilDatabase + AuthRepository                        │
│   • boot: currentUser()                                       │
│   • LoginScreen ──login──▶ AuthRepository ──▶ SQLite          │
│   • OeilScreen(user, BackendReplySource, database, onLogout)  │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│ screen/OeilScreen                                             │
│   • remember { OeilChatState(replySource, store, userId) }    │
│   • loadHistory() on entry, close() on dispose                │
│   • assembles Scaffold(topBar, content, bottomBar) + drawer   │
└───────┬──────────────────┬───────────────────┬────────────────┘
        │                  │                   │
        ▼                  ▼                   ▼
  components/ChatHeader  components/    components/ChatInput
  components/EmptyState  MessageItem    (composer, voice + photo intents)
                         TypingIndicator
                         components/AppDrawer
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│ state/OeilChatState  (behaviour + persistence)                │
│   messages / threads / input / isTyping / mode                │
│   send() ──▶ ConversationReplySource.reply(history, mode)     │
│              ├─ BackendReplySource ──▶ backend /chat ──▶ Gemini│
│              └─ MockConversationReplySource (tests/previews)  │
│   syncThread() ──▶ persistenceScope ──▶ OeilDatabase          │
└──────────────────────────────────────────────────────────────┘
```

## 2. Layers

| Layer      | Package                          | Responsibility                                                             |
|------------|----------------------------------|----------------------------------------------------------------------------|
| Entry      | *(root)* `MainActivity`, `OeilApp` | Edge-to-edge setup, theme, session gate and dependency wiring.            |
| Screen     | `screen`                         | Login and chat layout. Wires state to components.                          |
| State      | `state`                          | Conversation state, reply orchestration and persistence.                   |
| Components | `components`                     | Stateless (or nearly stateless) composables, reusable across screens.      |
| Data       | `data`, `data.local`             | `AuthRepository`, `BackendReplySource`, SQLite store and the mock seam.    |
| Model      | `model`                          | Plain immutable data classes (including `User`).                           |
| Theme      | `ui.theme`                       | Palette, typography and `OeilTheme`.                                       |

**Dependency direction** is strictly downward:
`screen → state/components → data/model → ui.theme`. Nothing in `data`, `model` or `ui.theme`
imports from `screen` or `components`.

## 2.1 Kotlin / Java split

Both languages live in the same source set and compile together, so either side can call the
other. The split is by concern, not by convenience:

| Written in | Packages                     | Why                                                                 |
|------------|------------------------------|---------------------------------------------------------------------|
| Kotlin     | `screen`, `components`, `state`, `model`, `ui.theme`, live `data` sources | Compose, coroutines, `data class` and sealed types are Kotlin-only. |
| Java       | `data/ReplySource.java`, `data/MockReplySource.java` | Legacy blocking reply seam, kept for the offline mock adapter. |

The live path is Kotlin: `BackendReplySource` and `AuthRepository` use `HttpURLConnection` on
`Dispatchers.IO`, and `OeilDatabase` wraps `SQLiteOpenHelper`. `MockConversationReplySource`
adapts the blocking Java `ReplySource` for tests and previews.
Java → Kotlin would work the same way, with the usual caveats: Java sees Kotlin `internal` members
as public, cannot see top-level functions (they compile to `FileNameKt`), and cannot use default
arguments without `@JvmOverloads`.

## 3. State management

State lives in `OeilChatState`, a plain class created with `remember` in `OeilScreen`, keyed by
the signed-in user, reply source and SQLite store. It is deliberately **not** a `ViewModel` yet:

- Conversation history is mirrored to SQLite, so a new state instance can restore it with
  `loadHistory()`.
- Keeping it a plain class avoids adding `lifecycle-viewmodel-compose` for two screens.
- The class is UI-framework-agnostic enough to migrate to a `ViewModel` later by changing only
  the construction site in `OeilScreen`.

```kotlin
val chat = remember(user.id, replySource, store) {
    OeilChatState(replySource = replySource, store = store, userId = user.id)
}
LaunchedEffect(chat) { chat.loadHistory() }
DisposableEffect(chat) { onDispose { chat.close() } }
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
   │     1. validate: text or attachment && !isTyping
   │     2. messages += ChatMessage(isUser = true)
   │     3. input = "" ; isTyping = true ; syncThread() → SQLite
   │     4. scope.launch {
   │            withContext(Dispatchers.IO) {
   │                replySource.reply(messages.toList(), mode)
   │            }
   │            messages += ChatMessage(reply, isUser = false)
   │            isTyping = false ; syncThread() → SQLite
   │        }
   │
   ├─ OeilScreen follows messages.size / isTyping
   │        → listState.animateScrollToItem(latestIndex)
   │
   └─ recomposition renders MessageItem / TypingIndicator
```

`send` takes an explicit `CoroutineScope` (`rememberCoroutineScope()` from the composable) so the
reply coroutine is cancelled if the screen leaves composition. SQLite writes go through a separate
single-lane `persistenceScope`, so they stay ordered and never block the UI thread.

## 4. Screen composition

`OeilScreen` is split into small private composables so each piece reads top-to-bottom:

| Composable            | Role                                                                  |
|-----------------------|-----------------------------------------------------------------------|
| `OeilApp`             | Boot/session gate: `LoginScreen` or `OeilScreen`                      |
| `LoginScreen`         | Email/password form                                                   |
| `OeilScreen`          | Drawer, background and Scaffold wiring                                |
| `FollowLatest`        | `LaunchedEffect` that follows the newest item                         |
| `Conversation`        | `LazyColumn` + empty-state overlay                                    |
| `JumpToLatest`        | Floating shortcut back to the newest message                          |

Public UI building blocks live in `components/` and take their behaviour as lambdas
(`onSend`, `onValueChange`, `onPick`), which keeps them previewable and reusable.

## 5. Theming

- **Dark-only palette** defined as top-level `Color` vals in `ui/theme/Color.kt`
  (e.g. `GreenPrimary #B6F35C`, `TealAccent #2FD4B0`, `BackgroundDark #040806`).
- `OeilTheme` maps those vals onto a Material 3 `darkColorScheme` and provides the typography.
- Components reference theme colors **directly** (not `MaterialTheme.colorScheme.*`) for
  gradients and one-off accents; `MaterialTheme.typography` is used for text styles.

## 6. Data layer

```
data/
├── AuthRepository.kt             # login API + local demo fallback
├── GeminiReplySource.kt          # Gemini generateContent REST client
├── ConversationReplySource.kt    # history-aware suspend seam
├── MockConversationReplySource.kt
├── ReplySource.java              # legacy blocking Java seam
├── MockReplySource.java          # offline canned answers
└── local/
    └── OeilDatabase.kt           # SQLiteOpenHelper store
```

**Reply path.** `ConversationReplySource` is a suspend interface that receives the whole
conversation and the current `ReplyMode`. The app uses `GeminiReplySource`, which POSTs to
`https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` with a system
instruction and a mode-specific temperature. `MockConversationReplySource` adapts the legacy Java
`ReplySource` for tests and previews.

**Auth path.** `AuthRepository` normalizes the email, validates the password, then either calls
the configured `LOGIN_API_URL` (`POST {"email","password"}`) or runs a local demo login. Either
way the user and active session are written to SQLite; `OeilApp` reads the active user on boot.

**Storage.** `OeilDatabase` owns five tables — `users`, `session`, `threads`, `messages` and
`settings` — and is accessed from `Dispatchers.IO` only. Threads and messages are keyed by user
and rewritten transactionally on each send. The Gemini key entered at sign-in is stored in
`settings` for the prototype; a production build should move it to encrypted storage.

The API key, model and login URL are injected through `BuildConfig` fields generated from
`local.properties`, environment variables or Gradle properties. No secret is committed.

## 7. Build & quality

- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- Compose compiler plugin applied via `kotlin-compose`.
- Release builds enable R8 optimisation with a `packageScope` allow-list; project-specific
  keep rules live in `app/src/main/keepRules/rules.keep`.
- Java 11 source/target compatibility for both the Kotlin and Java compilation tasks.

## 8. Extension points

| Goal                      | Where to change                                              |
|---------------------------|--------------------------------------------------------------|
| Another model/provider    | Implement `ConversationReplySource` and pass it to `OeilScreen` |
| Real login backend        | Set `LOGIN_API_URL`; adjust parsing in `AuthRepository`      |
| Richer persistence        | Extend `OeilDatabase` schema/methods and bump `DATABASE_VERSION` |
| Secure key storage        | Replace the `settings` row with `EncryptedSharedPreferences`/Keystore |
| Multiple screens/navigation | Add `navigation-compose` and move the gates into a graph   |
| Light theme               | Add a light `ColorScheme` in `Theme.kt` and honour `darkTheme` |
| View model migration      | Construct `OeilChatState` in a `viewModel { }` instead of `remember` |
