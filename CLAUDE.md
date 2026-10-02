# Pocket Money

Personal money app for Shyamal (formerly "Kharcha"): log expenses (heading, amount with a fold-away calculator keypad, Cash / Card / UPI, category), monthly dashboard with a "Today" hero card, budget, udhaar (money lent / borrowed with reminders), CSV export, backup. Indian context: amounts in ₹, `en-IN` formatting.

Brand reference (colours, fonts, voice, copy bank, logo set): the Claude doc "Pocket Money: Brand & Version 2 Plan", https://claude.ai/code/artifact/f68dcef3-5d1d-4326-bab2-f48aa7119b02. Short version: Pocket Blue `#1662F0`, Deep Blue `#0B3FB8`, Money Yellow `#FFC93C`; Baloo 2 for display text, system font for UI; voice is youthful, slightly witty, never judgmental (Zomato-style), Hinglish welcome. The ₹ coin appears as a still image only: **no running/animated coin** (Shyamal rejected it).

## How it ships

- The whole app is `index.html` (inline CSS + JS, no build step, no framework). Assets live in `assets/` (logo, Baloo 2 fonts). Data is in `localStorage` under `kharcha.v1` on the phone only.
- GitHub Pages serves `main` at https://imshyamalbhatt.github.io/kharcha/.
- The APK contains `android/loader.html` plus a bundled copy of `index.html` and `assets/`. On every launch the loader fetches the live `index.html` and runs it. **Pushing to `main` updates the phone app, no new APK needed.**
- A new APK is only needed for native changes: `android/loader.html`, Capacitor plugins (installed: Filesystem, Share, App, LocalNotifications), app icon or name. Build on Shyamal's laptop with `tools/build-apk.ps1`, bump `versionCode` in the Capacitor project and in `apk-version.json`, and publish it as a GitHub Release asset named `PocketMoney.apk`. The app shows a "New app version ready" banner when `apk-version.json` has a higher `versionCode` than the installed APK.

## Rules for changes

- Keep `index.html` a complete document ending in `</html>` and keep `id="tab-home"`: the loader rejects downloads without them.
- Keep the line that sets `localStorage['kharcha.boot'] = 'ok'` after the first render, or the loader treats the version as crashed.
- Never rename the storage key, the Android app ID `com.shyamal.kharcha`, or the record shapes without migrating old data. Expense: `id, title, amount, mode, category, date YYYY-MM-DD, note, createdAt`. Udhaar (`db.loans`): `id, person, type gave|took, amount, mode, date, remindOn, note, payments[{id, amount, date, mode}], writtenOff, createdAt`. Losing his records is the worst possible bug.
- New files in `assets/` are not inside already-installed APKs: images need the `onerror` fallback to the GitHub Pages URL (see the logo `<img>` tags), fonts fall back to the system font.
- No `alert` / `confirm` / `prompt`: use the in-app `ask()` dialog.
- Native features go through `Capacitor.Plugins` and must be guarded by the `native` flag, because the same file runs in a normal browser too.
- After changing cached files, bump `CACHE` in `sw.js`.

## Test before pushing

- `node tools/serve.js`, then open http://localhost:5173/tools/phone.html: the app in a phone frame with sample data, plus Restart app / Fresh install (onboarding) / dark mode buttons.
- Test-only URL switches: `?demo=1` (sample data), `?fresh=1` (new install, onboarding), `?tab=home|history|udhaar`, `?open=add`.
- Shyamal checks the laptop preview first; push to `main` only after he approves.
