const bcrypt = require("bcryptjs");
const crypto = require("node:crypto");
const jwt = require("jsonwebtoken");

const JWT_SECRET = process.env.JWT_SECRET || "dev-secret-change-me";
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || "7d";
const MIN_PASSWORD_LENGTH = 8;

if (!process.env.JWT_SECRET) {
  console.warn(
    "[auth] JWT_SECRET is not set. Using an insecure development secret; " +
      "set JWT_SECRET before deploying."
  );
}

function normalizeEmail(value) {
  return String(value ?? "").trim().toLowerCase();
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function displayName(email) {
  const local = email.split("@")[0] || "Oeil user";
  return (
    local
      .replace(/[._-]+/g, " ")
      .split(/\s+/)
      .filter(Boolean)
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ") || "Oeil user"
  );
}

function validateCredentials(email, password) {
  if (!isValidEmail(email)) {
    return "Enter a valid email address.";
  }
  if (typeof password !== "string" || password.length < MIN_PASSWORD_LENGTH) {
    return `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
  }
  return null;
}

function newUserId() {
  return crypto.randomUUID();
}

async function hashPassword(password) {
  return bcrypt.hash(password, 10);
}

async function verifyPassword(password, passwordHash) {
  return bcrypt.compare(password, passwordHash);
}

function createToken(user) {
  return jwt.sign({ sub: user.id }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

function readToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

function publicUser(user) {
  return { id: user.id, email: user.email, name: user.name };
}

module.exports = {
  MIN_PASSWORD_LENGTH,
  createToken,
  displayName,
  hashPassword,
  isValidEmail,
  newUserId,
  normalizeEmail,
  publicUser,
  readToken,
  validateCredentials,
  verifyPassword,
};
