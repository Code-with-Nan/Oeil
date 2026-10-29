require("dotenv").config({ quiet: true });

const express = require("express");
const {
  createToken,
  displayName,
  hashPassword,
  newUserId,
  normalizeEmail,
  publicUser,
  readToken,
  validateCredentials,
  verifyPassword,
} = require("./auth");
const { generateReply } = require("./gemini");
const { readUsers, withUsers } = require("./store");

const app = express();
const PORT = Number(process.env.PORT) || 3000;

app.use(express.json({ limit: "32kb" }));

async function requireAuth(req, res, next) {
  const header = req.get("authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) {
    return res.status(401).json({ error: "Missing bearer token." });
  }

  let payload;
  try {
    payload = readToken(token);
  } catch {
    return res.status(401).json({ error: "Invalid or expired token." });
  }

  const users = await readUsers();
  const user = users.find((candidate) => candidate.id === payload.sub);
  if (!user) {
    return res.status(401).json({ error: "Account no longer exists." });
  }

  req.user = user;
  return next();
}

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.post("/register", async (req, res) => {
  const body = req.body ?? {};
  const email = normalizeEmail(body.email);
  const validation = validateCredentials(email, body.password);
  if (validation) {
    return res.status(400).json({ error: validation });
  }

  const name =
    typeof body.name === "string" && body.name.trim()
      ? body.name.trim()
      : displayName(email);

  const result = await withUsers(async (users) => {
    if (users.some((user) => user.email === email)) {
      return { conflict: true };
    }
    const user = {
      id: newUserId(),
      email,
      name,
      passwordHash: await hashPassword(body.password),
      createdAt: new Date().toISOString(),
    };
    users.push(user);
    return { user };
  });

  if (result.conflict) {
    return res.status(409).json({ error: "An account with that email already exists." });
  }

  return res.status(201).json({
    token: createToken(result.user),
    user: publicUser(result.user),
  });
});

app.post("/login", async (req, res) => {
  const body = req.body ?? {};
  const email = normalizeEmail(body.email);
  const validation = validateCredentials(email, body.password);
  if (validation) {
    return res.status(400).json({ error: validation });
  }

  const result = await withUsers(async (users) => {
    const existing = users.find((user) => user.email === email);
    if (!existing) {
      // Development convenience: the Android screen has no sign-up form, so
      // the first login creates the account. Remove this and rely on
      // /register when you add a real sign-up flow.
      const user = {
        id: newUserId(),
        email,
        name: displayName(email),
        passwordHash: await hashPassword(body.password),
        createdAt: new Date().toISOString(),
      };
      users.push(user);
      return { user };
    }

    const valid = await verifyPassword(body.password, existing.passwordHash);
    return valid ? { user: existing } : { user: null };
  });

  if (!result.user) {
    return res.status(401).json({ error: "Invalid email or password." });
  }

  return res.json({
    token: createToken(result.user),
    user: publicUser(result.user),
  });
});

app.get("/me", requireAuth, (req, res) => {
  return res.json({ user: publicUser(req.user) });
});

app.post("/chat", requireAuth, async (req, res) => {
  const body = req.body ?? {};
  const mode = ["Auto", "Precise", "Creative"].includes(body.mode)
    ? body.mode
    : "Auto";

  try {
    const reply = await generateReply(body.messages, mode);
    return res.json({ reply });
  } catch (error) {
    const message =
      error?.name === "TimeoutError"
        ? "Gemini took too long to respond. Please try again."
        : error?.message || "The chat request failed.";
    console.error("[oeil-backend] chat failed:", message);
    return res.status(502).json({ error: message });
  }
});

app.use((_req, res) => {
  res.status(404).json({ error: "Not found." });
});

app.use((error, _req, res, _next) => {
  console.error("[oeil-backend]", error);
  res.status(500).json({ error: "Internal server error." });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`[oeil-backend] listening on http://localhost:${PORT}`);
  console.log(`[oeil-backend] Android emulator: http://10.0.2.2:${PORT}/login`);
});
