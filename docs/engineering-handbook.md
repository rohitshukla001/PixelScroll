# Engineering handbook

How we work on Pixel Scroll: the rules, the workflow, and the checks that every change passes. The developer guide explains how the code works; this handbook explains how to change it safely.

## Scope

Pixel Scroll does one job: it swipes to the next YouTube Short when the current one ends. The supported features are:

1. One-time setup that turns on the accessibility service.
2. Auto-scroll on or off from Home.
3. Detecting the YouTube Shorts screen.
4. Swiping when a Short ends, from its length and progress bar.
5. A fallback delay when a Short's length can't be read.

Instagram Reels was removed on 14 Nov 2026. Do not add other apps, statistics, notifications, Quick Settings tiles, extra settings pages or themes unless the owner asks for them.

The app is for personal use, installed from an APK. Google Play rules do not apply until the owner decides to publish.

## Engineering principles

In priority order: correctness, security, reliability, performance, maintainability, readability.

- Fix the root cause. Reproduce a bug before you fix it, and prove the fix with a test or a device run.
- Write the least code that solves the problem. No speculative options or abstractions.
- Keep changes small. Every changed line traces back to the task.
- Match the existing style and structure.
- Never claim a fix works on the phone until it has run on the phone. Say "checked on the emulator" when that is all you know.

## Code rules

### Kotlin

- Keep decisions in pure Kotlin (`scroll/`, `progress/`) with the time passed in, so unit tests can drive them. Android code (`service/`) only gathers input and acts.
- All YouTube view ids live in `TargetScreens.kt` and `ScreenScan.kt`. The package name is also in `accessibility_service_config.xml`.
- No code in the tick loop or a screenshot callback may throw out of its `try`/`catch`. An exception there ends the service while Android still lists it as on.
- Any `Bitmap` the engine makes is recycled in a `finally` block.
- Comments explain why, not what. Remove a comment when the code it describes changes.

### UI

- Follow the Pixel Scroll design system: monochrome, Inter, 1 dp borders, no shadows, no gradients, light theme only.
- Use the tokens in `ui/theme/` (`Ps.colors`, `PsType`, `Space`, `Radius`, `Size`). Never hard-code a colour or size in a screen.
- Every control has a tap target of 48 dp or more.
- Never show status by colour alone: pair it with an icon and words.
- Every screen ends with the shared `Footer`.
- All user-facing text lives in `res/values/strings.xml`. Write short, plain sentences in sentence case. Write seconds as a number, a space and "s" ("15 s").

## Workflow

### Branches and commits

- Never commit to `main` directly. Work on a feature branch.
- The owner commits and pushes. Do not commit or push unless the owner asks.
- Never rewrite history that is already pushed.

### Every change

1. Read the code you will change and trace the real flow.
2. Write or update the unit tests.
3. Run the checks:

   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```

4. Fix every error your change introduced. Lint must report 0 errors. The only warnings allowed are the existing "newer version available" ones.
5. Check the change on the emulator (screens and, for engine changes, the fake Shorts run below).
6. Confirm `apk/PixelScroll.apk` is from the last build. The debug build copies it there automatically.
7. Write down what was checked on the emulator and what still needs the phone.

### The APK rule

After every app change, run `./gradlew assembleDebug`. The `apk/` folder always holds exactly one file, `PixelScroll.apk`, built from the latest code, and it is committed with that code. This debug APK is the build users install.

## Testing the engine without YouTube

The emulator's YouTube is out of date and does not play Shorts. To run the real engine end to end, use a fake Shorts app:

1. A small app (package `fake.shorts`) with a vertical `RecyclerView` (id `reel_recycler`) and `PagerSnapHelper`. Each page has the id `reel_player_page_container`, a `reel_time_bar` holding a `SeekBar` whose description reads "0 minutes X seconds of 0 minutes N seconds", and a light-grey 6 px progress bar that fills and loops like a Short.
2. A copy of Pixel Scroll with `com.google.android.youtube` replaced by `fake.shorts` everywhere (Kotlin and the accessibility config).
3. Temporary `Log.d` lines in the copy only, never in the real code.

Pass mark: over 3 minutes, every Short is swiped at its end, with no failed gestures. The 14 Nov 2026 run swiped 22 Shorts in a row.

Keep these tools in a scratch folder, outside the repo.

### Emulator pitfalls

- `uiautomator dump` connects as an accessibility client and briefly unbinds other accessibility services. While it runs, Pixel Scroll is unbound and its status shows off. Use `adb exec-out screencap` to check the status row instead.
- `adb install -r` turns the accessibility service off. Turn it on again with:

  ```bash
  adb shell settings put secure enabled_accessibility_services dev.rohitshukla.pixelscroll/dev.rohitshukla.pixelscroll.service.PixelScrollAccessibilityService
  ```

- `am force-stop` removes Pixel Scroll from the enabled list entirely.
- An auto-scroll setting saved in an earlier test stays after `install -r`. Check that the power card says ON before a run.

## Testing on the phone

The test phone is a vivo I2214 on Android 16 (API 36), 1080 x 2400.

- Install the APK by hand and turn the service on again after each install.
- vivo sometimes turns the switch off on the first try. Turn it on again.
- The owner reports behaviour in plain words. Debug from that behaviour and from the code. Do not ask the owner to read logs.
- Ask for a short, specific check when the code can't decide between two causes, for example "Is the status row green or red when it stops?"

## Known open issues

| Issue | Status |
| --- | --- |
| The switch needs two tries on the vivo phone | Not reproduced on the emulator; needs the phone over USB |
| Release signing | Not used: the owner chose the debug-signed APK. Updates must be built on the same computer |
| Event and package filtering in the accessibility config | Checked on the emulator only (14 Nov 2026) |
| Library updates | The newest AndroidX and Compose versions need Android Gradle Plugin 9.1+ and compileSdk 37: a separate migration (Gradle 9, new SDK) to do and test on its own |

## Releasing

1. Raise `versionCode` by 1 and set `versionName` in `app/build.gradle.kts`.
2. Run the full checks.
3. Build the APK: `./gradlew assembleDebug` (copies it to `apk/PixelScroll.apk`).
4. Run the fake Shorts test on the emulator (see "Testing the engine without YouTube"): 3 minutes, every Short swiped at its end, plus one Short that jumps forward and one that rewinds. Then install on the phone and check setup, Home and a few Shorts.
5. Commit and push to `main`. The README's download link points to `apk/PixelScroll.apk` on `main`.

The APK is signed with the debug key of the computer that builds it. Android installs an update only over an app signed with the same key, so build releases on the same computer; a build from another computer needs users to uninstall first.
