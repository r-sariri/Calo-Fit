# Calo Fit — Security & Privacy Review

## 1. Zero Cloud Exfiltration by Default
- Calo Fit requires no mandatory sign-in, account registration, phone number, or cloud synchronization.
- All food records, body measurements, target weights, and dietary metrics reside strictly in the app's sandboxed Room SQLite database on device.

## 2. No Hardcoded Secrets
- No raw Gemini or external API secrets are hardcoded in the codebase or APK.
- Any server-side AI integrations utilize Google Cloud App Check / Firebase AI Logic.

## 3. Minimal Permissions
- Only standard install-time permissions (`INTERNET`, `CAMERA` for food photos).
- No sensitive location permissions (`ACCESS_FINE_LOCATION`) are requested for manual workout logging.

## 4. User-Controlled Local Data Ownership
- The user can export 100% of their historical data into a standard JSON file at any time from Settings.
- Full local data clearing is supported.
