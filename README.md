# Salah – Android app

Your HTML is in `www/index.html`. Capacitor wraps it in a native Android WebView.

## Option A – Android Studio (local)
1. Install Node.js 18+ and Android Studio.
2. In this folder: `npm install` then `npx cap sync android`
3. `npx cap open android`  -> Build > Build APK(s)
   APK ends up in android/app/build/outputs/apk/debug/

## Option B – No installs: GitHub builds it for you
1. Create a new GitHub repo and upload this whole folder (keep the .github folder).
2. Open the Actions tab -> "Build Android APK" -> Run workflow.
3. Download the `Salah-APK` artifact (contains app-debug.apk), copy it to the phone and install.

## Updating the page later
Replace `www/index.html`, then `npx cap sync android` and rebuild.
