# APK

`PixelScroll.apk` here is always the latest build of the app. Every debug build replaces it.

To make a new one:

```bash
./gradlew assembleDebug
```

Install it on a phone with USB debugging on:

```bash
adb install -r apk/PixelScroll.apk
```

The APK is not tracked by git. To keep it in the repo, delete the `apk/*.apk` line in `.gitignore`.
