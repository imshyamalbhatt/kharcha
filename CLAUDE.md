# Kharcha

Personal expense tracker for Shyamal (heading, amount with calculator keypad, Cash / Card / UPI, category, monthly dashboard, budget, CSV export, backup). Indian context: amounts in ₹, `en-IN` formatting.

## How it ships

- The whole app is `index.html` (inline CSS + JS, no build step, no framework). Data is in `localStorage` under `kharcha.v1` on the phone only.
- GitHub Pages serves this repo's `main` branch at https://imshyamalbhatt.github.io/kharcha/.
- The Android APK contains `android/loader.html` plus a bundled copy of `index.html`. On every launch the loader fetches the live `index.html` from GitHub Pages and runs it. **So pushing to `main` updates the phone app, no new APK needed.**
- A new APK is only needed when changing `android/loader.html`, Capacitor plugins, the app icon or name. That build runs on Shyamal's laptop: `tools/build-apk.ps1`.

## Rules for changes

- Keep `index.html` a complete document ending in `</html>` and keep the element `id="tab-home"`: the loader rejects downloads without them.
- Keep the line that sets `localStorage['kharcha.boot'] = 'ok'` after the first render. If it never runs, the loader treats the version as crashed and falls back to the bundled copy.
- Never change the storage key or the expense record shape (`id, title, amount, mode, category, date YYYY-MM-DD, note, createdAt`) without migrating old data. Losing his records is the worst possible bug.
- No `alert` / `confirm` / `prompt`: use the in-app `ask()` dialog (they are blocked in the Claude preview).
- Native features go through `Capacitor.Plugins` (Filesystem, Share, App are installed) and must be guarded by the `native` flag, because the same file also runs in a normal browser and in the Claude preview.
- After changing `sw.js` or cached files, bump `CACHE` in `sw.js`.

## Preview before pushing

`node tools/make-preview.js preview.html` produces a Claude Artifact version of the app (the `demo` flag loads sample data). Publish/update it so Shyamal can try the change, then push to `main` once he approves.

Existing preview artifact: https://claude.ai/artifact/3PKuXfWmFE7VRfnV4H7xL6
