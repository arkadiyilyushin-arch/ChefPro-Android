# Firebase setup (ChefPro Android)

Project: **chefpro-735** (shared with iOS ChefPro).

## 1. Register Android app

1. Open [Firebase Console](https://console.firebase.google.com/project/chefpro-735)
2. **Project settings** → **Your apps** → **Add app** → **Android**
3. Package name: `com.chefpro`
4. Download `google-services.json` → replace `app/google-services.json`

## 2. Enable services

| Service | Action |
|---------|--------|
| **Authentication** | Enable **Anonymous** sign-in |
| **Cloud Firestore** | Create database (production mode) |

## 3. Deploy security rules

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules --project chefpro-735
```

Rules file: `firestore.rules` (members-only access per restaurant).

## 4. Sync between devices

1. Open **Ещё → Синхронизация** on device A
2. Copy the **8-character sync code**
3. On device B enter the code and tap **Подключиться**
4. Both devices must have Google Play Services and internet

## 5. Troubleshooting

| Problem | Fix |
|---------|-----|
| Sync error / auth failed | Replace `google-services.json` with official file from console |
| Works offline only | Normal without Firebase setup; local JSON storage still works |
| Huawei / no GMS | App runs locally; cloud sync unavailable |
