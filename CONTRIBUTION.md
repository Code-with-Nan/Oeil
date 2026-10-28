# Contributing to Oeil

Thanks for helping improve Oeil. This document covers the workflow and the conventions we
follow so reviews stay fast and the codebase stays consistent.

---

## Development setup

1. Fork and clone the repository.
2. Open the project in Android Studio (JDK 11+, Android SDK 37).
3. Let Gradle sync complete, then verify a clean build:

```bash
./gradlew assembleDebug
./gradlew test
```

## Workflow

1. Create a feature branch from `main`:

   ```bash
   git checkout -b feat/short-description
   ```

2. Keep commits small and prefixed with a type:

   | Prefix   | Use for                              |
   |----------|--------------------------------------|
   | `feat:`  | New user-visible behaviour           |
   | `fix:`   | Bug fixes                            |
   | `refactor:` | Code changes with no behaviour change |
   | `docs:`  | Documentation only                   |
   | `test:`  | Tests                                |
   | `chore:` | Build, tooling, housekeeping         |

3. Open a pull request against `main` and fill in the summary + testing notes.

## Code conventions

- **Kotlin style**: follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html);
  4-space indentation; no wildcard imports; imports sorted alphabetically.
- **Compose rules**:
  - Stateless composables take a `modifier: Modifier = Modifier` parameter and apply it first.
  - Hoist state out of leaf composables; pass lambdas (`onSend`, `onPick`, …) instead.
  - Never read `Modifier` from globals or hard-code padding that belongs to the caller.
  - Prefer small private composables inside a file over deeply nested anonymous blocks.
- **Naming**:
  - Screens: `OeilScreen` in `screen/`
  - Reusable UI: nouns in `components/`
  - State holders: `*State` in `state/`
  - Data/source objects: nouns in `data/` or `model/`
- **Colors**: add new palette entries to `ui/theme/Color.kt` — do not hard-code hex values
  inside composables.
- **Comments**: document *why*, not *what*. KDoc public API; leave self-explanatory code alone.
- **Java style**: 4-space indentation, Javadoc on public types, `@NonNull`/`@Nullable` from
  `androidx.annotation` on every parameter and return value of interop APIs.
  Follow the [Android Java style guide](https://developer.android.com/style).
- **Language choice**: Kotlin by default. New Java code is allowed **only** at interop seams
  (`data/` and similar) where a Java SDK, annotation processor or Java-only consumer is involved.
  Never write Compose UI, coroutines/`Flow` or `data class` in Java — they aren't supported.
- **No ChatGPT/other product names** anywhere in file names, symbols or copy — Oeil is a
  standalone product.

## Tests

- Unit tests go in `app/src/test/`, instrumentation/UI tests in `app/src/androidTest/`.
- Pure logic (`MockReplySource`, `OeilChatState` behaviour) should be covered by unit tests;
  plain JUnit tests may be written in Java (see `MockReplySourceTest`).
- Run everything before opening a PR:

  ```bash
  ./gradlew test debugAndroidTest
  ```

## Pull request checklist

- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew test` passes
- [ ] No new lint warnings introduced
- [ ] New/changed composables keep `modifier` as a parameter
- [ ] Docs updated (`README.md`, `ARCHITECTURE.md`) when behaviour or structure changes
- [ ] Commit messages follow the prefix table above

## Reporting issues

Open an issue with:

- Device/emulator, Android version, app version
- Steps to reproduce
- Expected vs. actual behaviour
- Screenshots or logs where possible

## License

By contributing you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE), the same license as the project.
