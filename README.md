# Water Tracker

A simple Android app for keeping track of how much you drink each day.

## Features

- **Today** – a ring that shows what you drank today. Below 1000 ml it's shown in millilitres
  (`750 ml`); from 1000 ml on it switches to litres with decimals (`1.25 L`).
- **Add a drink**
  - a dropdown with every drink type, plus one-tap buttons beside it for your favorites
    (by default Water, Soft drinks and Tea)
  - a box for the amount in ml, or tap a preset: **⅓ glass**, **½ glass** or **full glass**
  - date and time are saved automatically; each entry can be deleted, and a snackbar lets you undo
- **Drink types** – Water, Soft drinks, Non-alcoholic, Alcoholic, Tea and Other to start with.
  In Settings you can rename them, change their icon, add new ones, remove them, and star the
  (up to 3) favorites that get a quick button.
- **Statistics** – daily (7 days per page) and weekly (8 weeks per page) bar charts with your
  daily goal as a dashed line, total / daily average / best day, how many days you reached your
  goal, and a breakdown by drink type. Use the arrows to go back in time.
- **Settings** – daily goal (default 2 L) and glass size (default 250 ml; the presets are ⅓, ½ and
  all of it).
- **Storage** – everything is stored on the phone (Room/SQLite). You can back up to and restore
  from **Google Drive**, by hand or automatically once a day. The backup is a single
  `WaterTracker-backup.json` file in your Drive; the app can only see files it created itself.

## Building

Open the project in Android Studio and press *Run*, or from the command line:

```sh
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```

Every push also builds the APK on GitHub Actions; download it from the run's
**Artifacts** (`water-tracker-debug-apk`).

Requirements: JDK 17+, Android SDK 35. The app runs on Android 8.0 (API 26) and newer.

## Setting up Google Drive backup

Google only lets an app access Drive after it has been registered once in the Google Cloud
Console. No keys go into the code; Google recognises the app by its package name and the SHA-1
fingerprint of the key it is signed with.

1. Go to <https://console.cloud.google.com/>, create a project (e.g. "Water Tracker").
2. **APIs & Services → Library**: enable the **Google Drive API**.
3. **APIs & Services → OAuth consent screen**: choose *External*, fill in the app name and your
   email, add the scope `.../auth/drive.file`, and add your own Google account under
   **Test users**.
4. **APIs & Services → Credentials → Create credentials → OAuth client ID**, type **Android**:
   - Package name: `io.github.behnamrm.watertracker`
   - SHA-1: the fingerprint of the key the APK is signed with. For builds from your own computer:
     ```sh
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
       -storepass android -keypass android | grep SHA1
     ```
5. In the app, go to **Settings → Google Drive backup → Back up now** and pick your account.

### Using the APK built on GitHub Actions

Each GitHub runner creates a fresh debug key, so its SHA-1 changes between builds. To keep it
stable, give the workflow your own debug keystore:

```sh
base64 -w0 ~/.android/debug.keystore   # on macOS: base64 -i ~/.android/debug.keystore
```

Save the output as a repository secret named `DEBUG_KEYSTORE_BASE64`
(**Settings → Secrets and variables → Actions**). The CI APK is then signed with the same key
as your local builds, so the SHA-1 from step 4 works for both, and you can install new builds
over old ones without losing data.

## Project layout

```
app/src/main/java/io/github/behnamrm/watertracker/
├── data/        Room database, settings (DataStore), repository, statistics math
├── drive/       Google Drive authorization, REST client, backup manager, daily backup worker
├── ui/home/     Today screen
├── ui/stats/    Statistics screen and bar chart
├── ui/settings/ Settings screen
└── util/        Volume formatting, date helpers
```
