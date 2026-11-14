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

The APK is tracked by git, so the latest build is on GitHub too. Commit it together with the code it was built from.
