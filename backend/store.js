const fs = require("node:fs/promises");
const path = require("node:path");

const DATA_DIR = path.join(__dirname, "data");
const USERS_FILE = path.join(DATA_DIR, "users.json");

let queue = Promise.resolve();

async function readUsers() {
  try {
    const raw = await fs.readFile(USERS_FILE, "utf8");
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    if (error.code === "ENOENT") {
      return [];
    }
    throw error;
  }
}

async function writeUsers(users) {
  await fs.mkdir(DATA_DIR, { recursive: true });
  const tempFile = `${USERS_FILE}.${process.pid}.tmp`;
  await fs.writeFile(tempFile, JSON.stringify(users, null, 2), "utf8");
  await fs.rename(tempFile, USERS_FILE);
}

/**
 * Runs a read-modify-write cycle one at a time. Without this, two logins that
 * arrive together could both read the same array and the last writer would
 * silently drop the other account.
 */
function withUsers(mutator) {
  const run = queue.then(async () => {
    const users = await readUsers();
    const result = await mutator(users);
    await writeUsers(users);
    return result;
  });
  queue = run.catch(() => {});
  return run;
}

module.exports = { readUsers, withUsers };
