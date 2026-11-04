# MobileCyberShield Android app
This repository packages the deployed [MobileCyberShield](https://mobilecybergaurd.netlify.app/) dashboard as a small Android application.
The app uses an Android WebView pointed at the live site, so the dashboard stays in sync with the Netlify deployment while still being installable as an APK.
## Build locally
Install Android Studio or the Android command-line tools, then run:
gradle assembleDebug
The debug APK is written to app/build/outputs/apk/debug/app-debug.apk.
## Build on GitHub
The included GitHub Actions workflow builds the debug APK on every push to main and uploads it as the mobilecybershield-debug-apk workflow artifact. A release build can be added later once a signing key is available.
## App configuration
The website URL is defined in app/src/main/java/com/mobilecyberguard/MainActivity.java as START_URL.
