import { readFileSync } from "node:fs";

let buildId = process.env.NEXT_PUBLIC_APP_BUILD_ID ?? "unknown";

try {
  buildId = readFileSync(".next/BUILD_ID", "utf8").trim() || buildId;
} catch {
  // The build file only exists after `npm run build`.
}

console.log(`[frontend] start at ${new Date().toISOString()} build=${buildId}`);
