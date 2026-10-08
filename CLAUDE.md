# Pocket Money

Personal money app for Shyamal (formerly "Kharcha"): log expenses (heading, amount with a fold-away calculator keypad, Cash / Card / UPI, category), monthly dashboard with a "Today" hero card, budget, udhaar (money lent / borrowed with reminders), CSV export, backup. Indian context: amounts in ₹, `en-IN` formatting.

Brand (since v2.1, after user feedback "a finance app should be green, font should be professional"): emerald green `--pm #0A8F5C`, deep `#065F3E`, logo green `#0BA368`, soft tint `#E6F4EE`, gold accent `#FFD166` / `#F2B630`, coral `#E5484D` for over-budget and "you owe". Font: **Inter** everywhere (assets/inter*.woff2), tabular numbers for amounts. Icons: **Lucide** line icons, embedded as the `ICONS` map in index.html (add new ones from the `lucide-static` npm package); no emoji as icons. The logo is `design/logo-source-green.png` (the blue original was recoloured). Voice is youthful, slightly witty, never judgmental (Zomato-style), Hinglish welcome. No running/animated coin. The older brand doc (blue, Baloo 2) is superseded for colours and fonts: https://claude.ai/code/artifact/f68dcef3-5d1d-4326-bab2-f48aa7119b02

Add-expense screen (his calls, 2.3): the heading starts **blank, with no suggestions or recent entries**, even for repeat spends; the amount prompt is just "Tap to enter amount"; a plain "Split this bill" tick with − / + people saves only your share (note "Split N ways, total ₹X"). Split is for expenses only, never Udhaar.

Dashboard is deliberately minimal (testers said it showed too much): Today card, Budget card (always shown, with days left and a safe per-day amount, or a "Set a monthly budget" button), due Udhaar reminders, Where it went (top 4 categories), Recent (4). The Cash/Card/UPI split lives on the History filter chips. Don't add blocks back to the dashboard without asking.

## How it ships

- The whole app is `index.html` (inline CSS + JS, no build step, no framework). Assets live in `assets/` (logo, Baloo 2 fonts). Data is in `localStorage` under `kharcha.v1` on the phone only.
- GitHub Pages serves `main` at https://imshyamalbhatt.github.io/kharcha/.
- The APK contains `android/loader.html` plus a bundled copy of `index.html` and `assets/`. On every launch the loader fetches the live `index.html` and runs it. **Pushing to `main` updates the phone app, no new APK needed.**
- A new APK is only needed for native changes: `android/loader.html`, `android/native/*.java`, Capacitor plugins (installed: Filesystem, Share, App, LocalNotifications), app icon or name. Build on Shyamal's laptop with `tools/build-apk.ps1`, bump `versionCode` in the Capacitor project's `app/build.gradle` and in `apk-version.json`, and publish it as a GitHub Release asset named `PocketMoney.apk`.
- **Update gate (Shyamal's rule):** at launch, if `apk-version.json` has a higher `versionCode` than the installed APK, a blocking pop-up shows over the splash: **Update** downloads and installs inside the app (`android/native/ApkUpdater.java`, APK 2.2+, needs `REQUEST_INSTALL_PACKAGES` in the Capacitor project's manifest), **Cancel** closes the app. Older APKs fall back to opening the download in the browser. Small online updates never ask: the 2.2+ loader fetches the live code at launch (1.5 s cap), and `kharchaUpdateReady` reloads silently.
- **The update gate must never lock him out:** after a failed or abandoned install it shows "Try again" and "Continue with current version". If a released APK can't be installed, set `apk-version.json` back to the installed versionCode at once (done on 2026-10-08 when 2.2 failed).
- **SMS status:** 2.2 (with READ_SMS / RECEIVE_SMS) failed to install on his phone, most likely Google Play Protect's block on sideloaded SMS apps in India. 2.3 shipped **without** SMS. **2.4 (2026-10-08) puts SMS back** and is installed through the in-app updater instead of Chrome; whether Play Protect allows that is unconfirmed. If 2.4 fails to install: set `apk-version.json` back to versionCode 5 / 2.3, and copy a no-SMS manifest back (2.3's is in git history; `AndroidManifest.sms.xml` is the SMS one).
- Theme: Settings → Theme (System default / Light / Dark) in `db.settings.theme`, applied as `data-theme` on `<html>`.
- **SMS auto-capture (code kept for that attempt):** `android/native/SmsReader.java` (READ_SMS) only returns raw inbox messages; `parseSms()` in index.html decides what is a debit, so new bank formats ship online. Found spends go to `db.pending` ("Found in your SMS" card on Home); ✓ adds, ✕ ignores, tap to edit; the user's heading/category per payee is learnt in `db.payeeTitles` / `db.payeeCats`; `db.smsSeen` dedupes by UPI ref. **When fixing a format, add the SMS as a case in `tools/test-sms.js` and run `node tools/test-sms.js`.**
- **WhatsApp nudges:** WhatsApp cannot send from a personal number automatically, so the app opens `wa.me/<number>?text=…` and the user taps Send. Friends are linked to one contact via `android/native/ContactPicker.java` (Android's picker, no contacts permission), stored in `db.contacts`. Udhaar notifications: 8 PM the day before (UDHAAR_PRE) and 10 AM on the day (UDHAAR_SEND / UDHAAR_OWE) with Send / Snooze 1 day / Already paid buttons. Message template: `db.settings.waText` with {name} {amount} {date}.
- Native project changes made by hand in `C:\Users\bhatt\android-build\kharcha` (not in git): app name in strings.xml, versionCode, green launch theme in styles.xml + `pm_colors.xml`, `REQUEST_INSTALL_PACKAGES` and `READ_SMS` permissions, launcher/splash images (made from `design/logo-source-green.png`).

## Rules for changes

- Keep `index.html` a complete document ending in `</html>` and keep `id="tab-home"`: the loader rejects downloads without them.
- Keep the line that sets `localStorage['kharcha.boot'] = 'ok'` after the first render, or the loader treats the version as crashed.
- Never rename the storage key, the Android app ID `com.shyamal.kharcha`, or the record shapes without migrating old data. Expense: `id, title, amount, mode, category, date YYYY-MM-DD, note, createdAt`. Udhaar (`db.loans`): `id, person, type gave|took, amount, mode, date, remindOn, note, payments[{id, amount, date, mode}], writtenOff, createdAt`. Losing his records is the worst possible bug.
- New files in `assets/` are not inside already-installed APKs: images need the `onerror` fallback to the GitHub Pages URL (see the logo `<img>` tags), fonts fall back to the system font.
- No `alert` / `confirm` / `prompt`: use the in-app `ask()` dialog.
- Native features go through `Capacitor.Plugins` and must be guarded by the `native` flag, because the same file runs in a normal browser too.
- After changing cached files, bump `CACHE` in `sw.js`.
- Every release: bump `pm-version` / `pm-notes` meta tags in index.html (the "new version" pop-up shows them).
- Udhaar is behind `const UDHAAR = true`; set false to show "coming soon" instead.

## Test before pushing

- `node tools/serve.js`, then open http://localhost:5173/tools/phone.html: the app in a phone frame with sample data, plus Restart app / Fresh install (onboarding) / dark mode buttons.
- Test-only URL switches: `?demo=1` (sample data), `?fresh=1` (new install, onboarding), `?tab=home|history|udhaar`, `?open=add`.
- Shyamal checks the laptop preview first; push to `main` only after he approves.
