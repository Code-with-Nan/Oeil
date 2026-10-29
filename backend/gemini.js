const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-3.5-flash-lite";
const ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";

const SYSTEM_PROMPTS = {
  Auto:
    "You are Oeil, a helpful, warm and clear AI assistant. Answer the user's " +
    "latest message using the conversation context. Use plain text with short " +
    "paragraphs. Never claim to be Gemini.",
  Precise:
    "You are Oeil. Give direct, factual and concise answers. Prefer the shortest " +
    "response that fully answers the question, and avoid filler.",
  Creative:
    "You are Oeil, an imaginative assistant. Offer fresh angles, vivid examples " +
    "and creative possibilities while still being useful and accurate.",
};

const TEMPERATURES = {
  Auto: 0.8,
  Precise: 0.3,
  Creative: 1.1,
};

function cleanMessages(messages) {
  if (!Array.isArray(messages)) {
    throw new Error("The request must include a messages array.");
  }

  return messages
    .slice(-50)
    .map((message) => {
      const role = message?.role === "user" ? "user" : "model";
      const content = String(message?.content ?? "").trim().slice(0, 8000);
      return { role, content };
    })
    .filter((message) => message.content.length > 0);
}

async function generateReply(messages, mode) {
  if (!GEMINI_API_KEY) {
    throw new Error("GEMINI_API_KEY is not configured on the server.");
  }

  const contents = cleanMessages(messages).map((message) => ({
    role: message.role,
    parts: [{ text: message.content }],
  }));

  if (contents.length === 0) {
    throw new Error("There is no message to send.");
  }

  const model = GEMINI_MODEL.replace(/^models\//, "");
  const url =
    `${ENDPOINT}/${encodeURIComponent(model)}:generateContent?key=` +
    encodeURIComponent(GEMINI_API_KEY);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      contents,
      systemInstruction: {
        parts: [{ text: SYSTEM_PROMPTS[mode] || SYSTEM_PROMPTS.Auto }],
      },
      generationConfig: {
        temperature: TEMPERATURES[mode] ?? TEMPERATURES.Auto,
        maxOutputTokens: 1024,
      },
    }),
    signal: AbortSignal.timeout(60_000),
  });

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    const message =
      payload?.error?.message || `Gemini request failed (HTTP ${response.status}).`;
    throw new Error(message);
  }

  const candidates = payload?.candidates;
  if (!Array.isArray(candidates) || candidates.length === 0) {
    const blocked = payload?.promptFeedback?.blockReason;
    return blocked
      ? `I can't help with that request (${blocked}).`
      : "Gemini didn't return a response. Please try again.";
  }

  const parts = candidates[0]?.content?.parts;
  const text = Array.isArray(parts)
    ? parts.map((part) => part?.text || "").join("").trim()
    : "";

  return text || "Gemini returned an empty response. Please try again.";
}

module.exports = { generateReply };
