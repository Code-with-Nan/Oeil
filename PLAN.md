# Android control: current scope

Oeil now uses Gemini through the Python relay for chat and Android agent steps.
Keep `GEMINI_API_KEY` on the backend. The Android app has no URL/API-key field;
its relay address is compiled in from `oeilBackendUrl`/`OEIL_BACKEND_URL`.

## Available actions

The app reads visible accessible labels and the list of launchable apps, then
asks the backend for one step. Gemini can answer normally or request one action:

- open an installed app by package name or visible label;
- tap a visible control;
- enter ordinary text in a visible field;
- scroll the current screen;
- go Back, Home, to recent apps, or to notifications;
- open an http/https link in the browser.

Android shows an approval overlay with the action details before each action.
Denying an action stops the task. The loop stops after eight steps and has a Stop
control in chat. Android control enables automatically when the accessibility
service connects; the phone icon in the header toggles it.

Screen labels and app names are untrusted input. Password fields are excluded,
and the agent is told not to enter passwords, payment details, or security codes.
Image bytes, screenshot-based vision, arbitrary touch gestures, and app-specific
integrations are not implemented. Accessibility availability and Play
distribution policy depend on Android version and current Google Play rules.

## Remaining work

- Add screenshot-based vision for interfaces without accessible labels.
- Consider app-specific intents or a platform-compliant alternative for actions
  that Accessibility cannot safely or reliably expose.
- Add backend rate limiting and decide whether `/chat` and `/agent/step` should
  require account authentication before public deployment.
