# How we'll make Oeil actually do things

Plain-English plan. No jargon unless it's explained.

---

## The big idea

Oeil is a chat app like ChatGPT, but with two extras:

1. **A real brain** — answers come from an NVIDIA model (we'll reach it through your
   Nebius Token Factory, which speaks the same "language" as ChatGPT's API).
2. **Real hands** — it can *use your phone*: open apps, tap buttons, type text, scroll,
   and read what's on screen. That's what "it does what you want" means.

## The three parts

| Part | What it is | In normal words |
|------|------------|-----------------|
| **Brain** | NVIDIA model over the internet | The AI that decides what to do next |
| **Eyes & hands** | Android Accessibility Service (AAS) | A special Android feature that lets an app read the screen and tap it, like a person would |
| **Conductor** | Our `AgentEngine` (new code) | The middleman: shows the model what's on screen, does what the model asks, sends the result back |

Think of it like a call centre: **you** place the order, the **conductor** takes notes and
dispatches, the **brain** decides, the **hands** carry it out.

## What happens when you ask something

1. You type: *"open WhatsApp and message Ana I'm late"*.
2. The conductor looks at the screen and writes a short description of it
   (buttons, text, app names) — this is the "snapshot".
3. It sends your request + the snapshot to the NVIDIA model.
4. The model replies with either:
   - **words** (just an answer), or
   - **an action**, like `tap the "New chat" button`.
5. Before anything happens on your phone, **you see the action and press Allow**
   (or "Allow for this session").
6. The hands perform it, take a new snapshot, and go back to step 3.
7. When the model has nothing left to do, you get the final answer.
   Every step is shown in the chat as a small card, so you can see what it's doing.

## Good news: we already have half of it

The app we've built so far (chat screen, bubbles, composer, mock replies) is the shell.
`data/ReplySource.java` was deliberately left as a plug socket — the real brain plugs in
there, and the mock stays as a spare for tests.

## Rules we won't break

- **Nothing touches your phone without you pressing Allow.** Reads (looking at the
  screen) can be automatic; taps and typing always ask.
- **A Stop button and a step limit** (say 12 actions) so it can't run off forever.
- **The API key never sits in the code.** For a public release we'd put a tiny relay
  server in front so the key can't be copied out of the app.
- **App store warning:** Google Play doesn't allow apps to use the Accessibility
  feature for non-accessibility jobs, so this will probably ship by direct download
  (sideload / F-Droid), not the Play Store. We keep a Shizuku option in our back pocket.

## The steps

| Step | What you get | What you need |
|------|--------------|---------------|
| **0** | Settings screen: base URL, model name, API key. Real streaming chat replaces the mock. | Your Nebius Token Factory key |
| **1** | Two safe tools: *open an app* and *read the screen*. It can answer "what's on my screen?". | The key |
| **2** | The hands: tap, type, scroll, back. Plus the Allow/Deny pop-up and onboarding for turning the Accessibility feature on. | Nothing — can be built today |
| **3** | The full loop: plan → act → check → repeat, with step cards, Stop, and saved history. | The key |
| **4** | Screenshots too (for screens text-reading can't understand) using the NVIDIA vision model. | The key |
| **5** | Choose how to publish it (sideload vs Shizuku vs Play). | Your call |

## What I need from you

1. The Nebius **base URL and API key** (step 0 needs it).
2. A decision on **step 5** later — it changes nothing in the code until then.

If you'd rather start today without a key, step **2** is the place to begin:
the phone-controlling side, the Allow/Deny gate, and onboarding. It's testable on its
own with a fake brain.
