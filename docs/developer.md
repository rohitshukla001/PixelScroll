# Developer guide

This guide explains how Pixel Scroll works inside, how to build it, and where to change things. Read it before you change the scroll engine.

Pixel Scroll is an Android app. It swipes to the next YouTube Short when the current one ends. It uses an Android accessibility service to read YouTube's screen and to send the swipe gesture.

## Requirements

| Item | Version |
| --- | --- |
| JDK | 17 (`export JAVA_HOME=/opt/homebrew/opt/openjdk@17` on this Mac) |
| Android SDK | compileSdk and targetSdk 35; `local.properties` sets `sdk.dir` |
| Gradle | 8.11.1 (wrapper) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.0 |
| Jetpack Compose BOM | 2024.12.01 |
| Lowest Android version | 8.0 (API 26). Reading the progress bar needs Android 11 (API 30) or later |

## Build and run

```bash
./gradlew assembleDebug
```

Each debug build copies the APK to `apk/PixelScroll.apk`. That folder always holds one APK: the latest build.

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew lintDebug
```

CI (`.github/workflows/android.yml`) runs the same three commands on every push to `main` and on every pull request.

To install on a phone, copy `apk/PixelScroll.apk` to the phone and open it. Installing a new APK turns the accessibility service off, so turn it on again after each install.

## Project structure

```
app/src/main/java/dev/rohitshukla/pixelscroll/
├── MainActivity.kt                 Hosts the screens; implements AppController
├── data/
│   ├── Settings.kt                 The two settings: auto-scroll on/off, fallback delay
│   ├── SettingsRepository.kt       Reads and writes Settings with DataStore
│   ├── AppData.kt                  One SettingsRepository per process
│   └── SetupState.kt               Whether first-run setup is finished
├── service/
│   ├── PixelScrollAccessibilityService.kt   The accessibility service; runs the engine
│   ├── ScrollEngine.kt             The once-a-second loop that decides and swipes
│   ├── ScreenScan.kt               One walk over YouTube's screen
│   ├── TargetScreens.kt            YouTube's package name and Shorts view ids
│   ├── TargetScreenDetector.kt     Is the Shorts screen showing?
│   ├── SwipeDispatcher.kt          Sends the upward swipe
│   ├── SwipeGeometry.kt            Where the swipe starts and ends
│   └── ServiceStatus.kt            Is the service on, and is it running?
├── scroll/                         Pure Kotlin, unit tested
│   ├── VideoState.kt               The Short on screen: start time, length, when to read
│   ├── ScrollDecider.kt            When to swipe
│   └── EndPredictor.kt             Times the swipe to the exact end
├── progress/                       Pure Kotlin, unit tested
│   ├── ProgressBarReader.kt        How full the progress bar is, from pixels
│   ├── MedianOfThree.kt            Drops one wrong reading
│   └── SeekBarDescription.kt       The Short's length from the seek bar text
└── ui/
    ├── PixelScrollApp.kt           Navigation between the three screens
    ├── screens/                    WelcomeScreen, AccessScreen, HomeScreen
    ├── components/                 Buttons, card, switch, slider, status row, footer, icons
    └── theme/                      Colour, type, spacing and radius tokens
```

## How a swipe happens

```
YouTube on screen
      │
      ▼
ScrollEngine.tick()  ── every 1 s while auto-scroll is on
      │
      ├─ screen off? app in front is not YouTube? → stop tracking
      ├─ ScreenScan: view ids, seek bar text and bounds
      ├─ TargetScreenDetector: Shorts screen? (reel_recycler or reel_player_page_container)
      ├─ VideoState.onLength(): length from the seek bar; a new length means a new Short
      ├─ ScrollDecider.onTick(): fallback delay due?
      └─ near the end: screenshot → ProgressBarReader → MedianOfThree
                        → ScrollDecider.onProgress() and EndPredictor
      │
      ▼
SwipeDispatcher.swipeUp(): 80% → 20% of YouTube's window, 250 ms
      │
      ▼
YouTube sends a scroll event → ScrollEngine.onScrolled() → new Short
```

### 1. Is the Shorts screen showing?

Each tick, the engine gets the active window's root node. It stops at once if the app in front is not YouTube, so it never reads other apps' screens. Otherwise `ScreenScan` walks the node tree (60 levels at most) and collects:

- every view id, for the screen check;
- the visible `SeekBar` inside `reel_time_bar`, with its description and bounds;
- the visible `reel_progress_bar` bounds, when YouTube shows it.

Before the walk, one search for the Shorts list (`findAccessibilityNodeInfosByViewId`) stops it at once on YouTube's other screens.

`TargetScreenDetector` returns `YOUTUBE_SHORTS` when the package is exactly `com.google.android.youtube` and one of the marker ids is on screen.

### 2. How long is the Short?

The seek bar's description holds the position and the length, for example "0 minutes 2 seconds of 0 minutes 45 seconds". `SeekBarDescription.durationSec` reads the English form first. In other languages it uses the numbers alone: the first half is the position, the second half the length.

`VideoState` keeps the length for the current Short. A different length on a later tick means the user swiped to a new Short without a scroll event reaching us.

### 3. How far has it played?

YouTube draws the played part as a light-grey line about 6 px high. The engine takes a screenshot (Android 11+), cuts out the bar's area, and `ProgressBarReader` finds the longest light-grey run from the left edge. A run counts only when the rows above and below are mostly not light, so bright video content is not mistaken for the bar. `MedianOfThree` keeps the middle of the last three readings.

The engine times its readings from where the video really is, not from the clock (`VideoState.onProgressRead`). Each reading gives the time left: the length times the unread part of the bar. It reads every 3 s (`SAMPLE_MS`) until 5 s are left (`READ_LEAD_MS`), then every second. When you jump forward or back in a Short, the next reading sees it and the end moves with it.

### 4. When to swipe

`ScrollDecider` returns `SwipeNow` for one of three reasons:

| Reason | When |
| --- | --- |
| `END_REACHED` | The smoothed fill is 0.97 or more |
| `LOOPED` | The fill was 0.8 or more and is now 0.15 or less (the Short started again). A rewind to the middle doesn't count |
| `FALLBACK_DELAY` | No progress reading for 3 s, and the delay has passed: the length plus 1 s when known, else the user's fallback delay |

Readings come once a second near the end, so the last part of a Short often falls between two readings. `EndPredictor` works out the time left from the latest reading and the length. When the end comes before the next reading, the engine schedules the swipe 250 ms before the end. Just before it fires, it takes one more reading: if the bar went back (the user rewound), it cancels and times the end again.

A paused Short is not swiped while its bar is being read: the reading stays the same, so no reason applies.

After one `SwipeNow`, the decider waits for the next `onNewVideo`. One Short never causes two swipes.

### 5. The swipe

`SwipeDispatcher` sends a straight swipe up the middle of YouTube's window, from 80% to 20% of its height, in 250 ms. If Android cancels the gesture, the engine tries once more for the same Short. If Android refuses it outright, the engine sends it again on the next tick without restarting the Short's timing.

A new Short starts when one of these happens:

- YouTube sends a scroll event from `reel_recycler` (the user's swipe or ours);
- the seek bar shows a different length;
- 3 s pass after our swipe with no scroll event (`VideoState.NO_SCROLL_EVENT_MS`).

### 6. Errors and status

Each tick and each screenshot reading runs inside a `try`/`catch`. One failure skips that second instead of ending the service. After 5 failed seconds in a row, the engine reports itself as not running and Home shows "Auto-scroll stopped working" in red. The row goes green again after a clean second.

A screenshot that gets no answer within 3 s is given up, so readings never stop for good. While screenshots fail after good readings, the fallback delay is held for up to 10 s, so a paused Short isn't swiped.

`ServiceStatus` holds two facts:

- `isEnabled(context)`: Pixel Scroll is in Android's list of enabled accessibility services;
- `running`: the service is connected in this process and the engine is healthy.

The status row is green only when both are true. Android can list a service as on while it is not running, after a crash or when the phone stops it.

## Settings and storage

| Setting | Default | Range | Where |
| --- | --- | --- | --- |
| Auto-scroll on | true | on, off | DataStore `settings` |
| Fallback delay | 15 s | 3 s to 120 s, 1 s steps | DataStore `settings` |
| Setup finished | false | true, false | SharedPreferences `setup` |

The app and the service run in one process and share one `SettingsRepository`. The service collects the settings flow and passes each change to the engine.

Nothing leaves the phone. The app has no internet permission. Backups and device transfer are off (`backup_rules.xml`, `data_extraction_rules.xml`).

## Accessibility service config

`res/xml/accessibility_service_config.xml`:

| Setting | Value | Why |
| --- | --- | --- |
| Event types | `typeViewScrolled` | Scroll events tell the engine a new Short is on screen |
| Package names | `com.google.android.youtube` | No events from other apps |
| Flags | `flagReportViewIds`, `flagIncludeNotImportantViews` | View ids for the screen check; the seek bar is a "not important" view |
| `canRetrieveWindowContent` | true | Read YouTube's screen |
| `canPerformGestures` | true | Send the swipe |
| `canTakeScreenshot` | true | Read the progress bar (Android 11+) |

## When YouTube changes its app

YouTube can rename view ids or move the bar. All ids live in two files:

- `service/TargetScreens.kt`: the package name and the Shorts marker ids;
- `service/ScreenScan.kt`: `reel_time_bar` and `reel_progress_bar`.

The package name is also in `accessibility_service_config.xml`. If the bar's colour changes, update `ProgressBarReader.isLight` and its tests.

The current values were found on a vivo I2214 (Android 16, 1080 x 2400) on 13 Nov 2026:

- `reel_recycler` and `reel_player_page_container` are on every Shorts screen.
- The seek bar is a `SeekBar` with no id inside `reel_time_bar`, bounds about `[0,2085][1080,2217]`. Its description holds the position and the length.
- `reel_progress_bar` is about `[22,2146][1058,2152]` (6 px high). It is visible in screenshots but rarely marked visible in the node tree, so the engine usually reads the bar from the seek bar's middle band.
- The played part is light grey: every colour channel above 150 and within 40 of each other.
- YouTube's media session reports length 0 for Shorts, so it can't be used.

## UI

Three screens, set in `ui/PixelScrollApp.kt`:

| Screen | Shows | Leaves to |
| --- | --- | --- |
| Welcome | First launch only | Accessibility access |
| Accessibility access | Why the app needs access; "Open Settings", "Not now" | Home (when the service connects, or on "Not now") |
| Home | Power card, service status, fallback delay | Accessibility access ("Fix") |

Every screen ends with the shared `Footer`. The app is light only. Colours, type, spacing and radii come from the design system's tokens in `ui/theme/`. Do not hard-code values in screens.

## Testing

Unit tests cover the pure Kotlin parts:

| Test | Covers |
| --- | --- |
| `ScrollDeciderTest` | End, loop, fallback, pause, one swipe per Short |
| `VideoStateTest` | New-Short detection, length reset, when to read, silent swipe |
| `EndPredictorTest` | Swipe timing near the end |
| `ProgressTest` | Pixel reading, median filter, seek bar text in several languages |
| `TargetScreenDetectorTest` | Shorts screen only; other apps ignored |
| `SwipeGeometryTest` | Swipe line |
| `SettingsRepositoryTest` | Defaults, read-back, delay limits |

The engine itself (`ScrollEngine`) needs a device. See the engineering handbook for how to test it on the emulator without YouTube.
