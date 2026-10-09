# Google Drive backup: one-time Google Cloud setup

Pocket Money backs up to a hidden app folder in the user's own Google Drive
(scope `drive.appdata` + `email`). Google only lets the app ask for that if this
setup exists. Done once, in the Google account that will be used (Shyamal's).

| Value | |
|---|---|
| Package name | `com.shyamal.kharcha` |
| SHA-1 signing fingerprint | `7A:81:5F:2A:F9:39:5B:3E:08:E5:1B:38:EB:0E:A1:78:68:12:41:20` |

1. Open https://console.cloud.google.com and sign in with the Gmail account you'll back up to.
2. Top bar → project picker → **New project** → name `Pocket Money` → **Create**, then select it.
3. Search **Google Drive API** → open it → **Enable**.
4. Left menu → **Google Auth Platform** (older screens: APIs & Services → OAuth consent screen) → **Get started**:
   - App name `Pocket Money`, support email = your Gmail → Next
   - Audience: **External** → Next
   - Contact email = your Gmail → Next → agree → **Create**
5. **Data access** → **Add or remove scopes** → tick `.../auth/drive.appdata` (search "drive.appdata") and `.../auth/userinfo.email` → Update → **Save**.
6. **Clients** → **Create client** → Application type **Android**:
   - Name `Pocket Money Android`
   - Package name `com.shyamal.kharcha`
   - SHA-1 fingerprint (from the table above) → **Create**
7. **Audience** → under Test users, **Add users** → your Gmail → Save.
   Then click **Publish app** → Confirm, so the sign-in doesn't expire every 7 days.
   (`drive.appdata` and `email` are not sensitive scopes, so no Google review is needed.)

Nothing needs to be copied back into the app: Google matches the app by package name + SHA-1.

If the app says "Google setup not finished", the Android client in step 6 is missing or its
package name / SHA-1 doesn't match. The signing key is saved in
`C:\Users\bhatt\android-build\KEEP-SAFE-signing-key` (keep a copy in Google Drive).
