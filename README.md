# Biller

An Android app that records your bills into an Excel file, keeps the sheet growing
as you add bills, and lets you "quit" the Excel — which saves it securely to Google
Drive (Firebase Storage), keeps an offline copy on the phone, saves a copy to
Downloads, and starts a fresh new Excel file.

## Features

- Login / Sign Up with email and password (Firebase Authentication), or **Guest mode**
- Add bills: Bill Number, Amount, Name of Biller, Remarks, and a photo of the bill
- **Submit** button — appends the bill as a new row in the current Excel file
  (with the bill image embedded, neatly formatted sheet)
- **Quit Excel** button —
  - saves it securely to Firebase Storage (your "Drive") when signed in
  - keeps the file on the phone — always available **offline**
  - saves a copy to the phone's Downloads folder
  - provides Download / Share buttons
  - automatically creates a new Excel file with a new file name
- **My Excel Files** — offline history of every completed Excel on this phone,
  with Download and Share for each
- English + Tamil language toggle
- Dark / light theme
- No emails — everything stays in the app, your cloud Drive, and Downloads

## App structure

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/invoicesaver/app/data/ExcelManager.kt` | Creates/reads/updates the `.xlsx` file (Apache POI, formatted sheet) |
| `app/src/main/java/com/invoicesaver/app/data/BillRepository.kt` | Firebase Auth + Storage calls |
| `app/src/main/java/com/invoicesaver/app/data/FileSaver.kt` | Saves to Downloads / shares the Excel |
| `app/src/main/java/com/invoicesaver/app/ui/` | Screens (login, sign up, home, history) + ViewModels |
| `app/google-services.json` | **Placeholder** — replace with your Firebase config (see SETUP_GUIDE.md) |

## How the Excel works

- Every user session has one "current" Excel file: `Bills_<date>_<time>.xlsx`.
- Each **Submit** appends a row: `Bill Number | Amount | Name of Biller | Remarks | Date/Time | Bill Image`.
- The sheet is auto-formatted: colored header, borders, aligned amount column,
  frozen header row, auto-filter, and embedded bill photos.
- **Quit Excel** finalizes the current file and immediately creates a new one
  with a new timestamped name. The finished file stays on the phone (offline
  history) and, when signed in, is backed up securely to the server.

## Build the app (Windows)

Requirements: JDK 17, Android SDK (build-tools 36.x, platform android-36).

```
gradlew.bat :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

For a release APK you need a signing key — see SETUP_GUIDE.md.

> Tip: if you copy this folder into OneDrive/synced storage, build performance may
> be slow. Copy the project to a local folder like `C:\dev\Biller` for building.

## Before the app can log in / upload

The app needs your Firebase project's `google-services.json`.
Follow **SETUP_GUIDE.md** — it walks through creating the free Firebase project,
replacing the placeholder, and publishing to the Play Store.
