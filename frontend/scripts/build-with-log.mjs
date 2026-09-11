import { spawnSync } from "node:child_process";

const buildId =
  process.env.NEXT_PUBLIC_APP_BUILD_ID ??
  process.env.VERCEL_GIT_COMMIT_SHA ??
  new Date().toISOString().replace(/[^0-9A-Za-z_-]/g, "-");

console.log(`[frontend] build started at ${new Date().toISOString()} build=${buildId}`);

const result = spawnSync("npx", ["next", "build"], {
  env: {
    ...process.env,
    NEXT_PUBLIC_APP_BUILD_ID: buildId,
  },
  shell: true,
  stdio: "inherit",
});

if (result.status === 0) {
  console.log(`[frontend] build finished at ${new Date().toISOString()} build=${buildId}`);
} else {
  console.error(`[frontend] build failed at ${new Date().toISOString()} build=${buildId}`);
}

process.exit(result.status ?? 1);
