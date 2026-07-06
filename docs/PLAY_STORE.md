# Google Play publication checklist

## Before upload

1. **Release keystore** (one-time):
   ```bash
   keytool -genkey -v -keystore chefpro-release.jks -alias chefpro \
     -keyalg RSA -keysize 2048 -validity 10000
   cp keystore.properties.example keystore.properties
   # fill passwords in keystore.properties
   ```

2. **Build signed release**:
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

3. **Or build AAB for Play Console** — add to `app/build.gradle.kts`:
   ```kotlin
   // bundle { ... }
   ```
   Then: `./gradlew bundleRelease`

## Store listing (draft)

| Field | Suggested value |
|-------|-----------------|
| App name | ChefPro — Restaurant ERP |
| Short description | Техкарты, склад, food cost и аналитика для ресторана |
| Category | Business |
| Content rating | Everyone |
| Privacy policy | Required — host a page describing local-only + optional Firebase sync |

## Screenshots needed

- Login screen
- Dashboard
- Tech card detail
- Inventory list
- Analytics / Food Cost

## Permissions justification

| Permission | Reason |
|------------|--------|
| CAMERA | Barcode scan, dish photos |
| POST_NOTIFICATIONS | Low stock, expiry alerts |
| READ_MEDIA_IMAGES | Pick dish photos from gallery |
| INTERNET | Optional Firebase sync |
