# Biller — Firebase & Play Store Setup Guide

Follow these steps **in order**. Steps 1–3 are needed for the app to work on your
phone (server storage + secure login); step 4 is for publishing.

---

## 1. Create a Firebase project (free)

1. Go to https://console.firebase.google.com and sign in with your Google account.
2. Click **Add project** → name it `Biller` → continue (Google Analytics optional).
3. You now have a Firebase project.

## 2. Add your Android app to Firebase

1. In the Firebase console, click the **Android icon** (add app).
2. Package name: `com.invoicesaver.app`
3. Nickname: `Biller` → **Register app**.
4. Firebase shows a **Download google-services.json** button → download it.
5. **Replace** the placeholder file in the project:
   `InvoiceSaver\app\google-services.json`
6. In the Firebase console, note your **Web API key**.
   - Project settings ⚙ → General → under *Your apps* → Web API Key
   - Copy it and paste it into `app/google-services.json` here:
     ```
     "api_key": [ { "current_key": "PASTE_YOUR_REAL_KEY" } ]
     ```
   - Also set the real project id in the same file:
     ```
     "project_id": "your-real-project-id",
     "storage_bucket": "your-real-project-id.appspot.com",
     "mobilesdk_app_id": "1:REAL...",
     "project_number": "REAL"
     ```
   > Easiest: don't edit by hand. Download the file again from step 4 after
   > enabling the services below — it already contains everything.

## 3. Enable the services the app uses

In the Firebase console:

1. **Authentication** → Get started → *Sign-in method* → enable **Email/Password** → Save.
2. **Storage** → Get started → choose **Start in production mode** → Done.
3. Open **Storage** → **Rules** tab → set rules to:
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /bills/{userId}/{fileName} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
   Publish. This keeps every user's files private to them and stored securely.

> The app never emails anything. Finished Excel files are stored on the phone
> (offline history) and, when signed in, backed up securely under your account.

## 4. Publish to the Play Store

1. Create a **signed release APK**:
   - A signing key already exists in this project:
     `invoicesaver-release.jks` + `key.properties` + `KEYSTORE_INFO.txt`
     (keep these safe forever — you need them for every future update).
   - Build the signed APK:
     ```
     gradlew.bat :app:assembleRelease
     ```
     Output: `app/build/outputs/apk/release/app-release.apk`
2. Sign up for a **Play Console** account: https://play.google.com/console
   (one-time fee of ~$25).
3. **Create app** → name `Biller`, category Business/Finance.
4. **Set up your app** → complete the store listing (description, screenshots,
   icon, privacy policy).
   - Privacy policy: you must state that the app stores bills in Google's cloud.
     You can generate one free at https://app-privacy-policy-generator.firebaseapp.com
5. **Production → Create new release** → upload `app-release.apk`.
6. Complete the **Data safety** form (declare: no personal data collected beyond
   the email used to log in; data stored in cloud storage).
7. **Test your app** using *Closed testing* first (optional but recommended) or
   submit directly for review.
8. Google reviews the app (usually 1–7 days) and then it goes live.

> Play Store requirement: you must have a real Firebase project (steps 1–3) before
> publishing — the placeholder `google-services.json` will not work on real users'
> phones.

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| Login says "login failed" | `google-services.json` is the placeholder or Authentication sign-in method not enabled |
| Upload says "failed, check internet" | Storage rules wrong, or user not signed in |
| App crashes on start | `google-services.json` project_id/mobilesdk_app_id invalid |
