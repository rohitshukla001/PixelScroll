# Pixel Scroll

Pixel Scroll is an Android app that scrolls YouTube Shorts for you. It moves to the next Short when the current one ends. After setup, it shows nothing inside YouTube.

The app has three screens: Welcome (first launch), Accessibility access (one-time setup) and Home. Home turns auto-scroll on or off and sets the fallback delay.

It swipes when each Short ends, worked out from the Short's length and its progress bar. A Short whose length can't be read swipes after the fallback delay (3 s to 120 s). Instagram Reels is not supported.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Android 8.0 (API 26) and later
- Gradle 8.11.1 with the Kotlin DSL and a version catalog (`gradle/libs.versions.toml`)

## Project structure

```
apk/                           Latest built APK (PixelScroll.apk)
app/src/main/java/dev/rohitshukla/pixelscroll/
├── MainActivity.kt            Screens host; implements the app's actions
├── service/                   Accessibility service, scroll engine, screen check, swipe
├── scroll/                    When to swipe (pure Kotlin, unit tested)
├── progress/                  Progress-bar reading and seek bar text (pure Kotlin, unit tested)
├── data/                      Settings (DataStore, on the phone only)
└── ui/                        Theme, components and the three screens from the design system
docs/                          User, developer, engineering and performance guides
```

## Build

You need JDK 17 and the Android SDK. Android Studio installs both.

```bash
./gradlew assembleDebug
```

Each debug build also copies the APK to `apk/PixelScroll.apk`, replacing the old one. That folder always holds one APK: the latest build.

## Docs

- [User guide](docs/user-guide.md): install, setup, Home, troubleshooting.
- [Developer guide](docs/developer.md): how the app works inside and where to change it.
- [Engineering handbook](docs/engineering-handbook.md): rules, workflow, checks and emulator testing.
- [Performance](docs/performance.md): what the app costs the phone, and what is not measured yet.

## Design

The UI follows the Pixel Scroll "Monochrome Futuristic" design system: black, white and neutral grey, Inter type, thin borders and no shadows. The app is light only, even when the phone is in dark mode.

---

Design and Developed by Rohit Shukla
