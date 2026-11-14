# First release tasks

> History: written in Nov 2026 when Pixel Scroll also targeted Instagram Reels. Since 14 Nov 2026 the app supports YouTube Shorts only; see the README for the current app.

The first release is split into 10 tasks. Each task ends with a check you can run, and each finished task leaves a working app and a fresh `apk/PixelScroll.apk`.

Tasks 3, 4, 5 and 7 need nothing from each other and can be built in any order. The "Needs" column lists the only tasks that must come first.

| # | Task | Needs | Test |
| --- | --- | --- | --- |
| 1 | Build toolchain | – | `./gradlew assembleDebug` passes and `apk/PixelScroll.apk` installs and opens |
| 2 | Progress detection test | 1 | `docs/progress-detection-test.md` names a working source, or none, for Shorts and for Reels |
| 3 | Scroll decision logic | 1 | Unit tests pass |
| 4 | Target screen check | 1 | Unit tests pass |
| 5 | Swipe gesture | 1 | On a phone, the swipe test moves Shorts and Reels to the next video |
| 6 | Scroll engine | 2, 3, 4, 5 | On a phone, the screen check is true only on Shorts and Reels, and a 7 s and a 30 s video each play to the end, then swipe |
| 7 | Settings store | 1 | Unit tests pass for read, write and default values |
| 8 | On and off controls | 6, 7 | Turning it off from the notification, the tile or the app updates the other two within 1 s |
| 9 | Setup screens | 1 | Welcome, How it works, Accessibility access and Setup complete match the design in light and dark |
| 10 | Main screens | 7, 9 | Home, Settings, Stats and About match the design; a changed delay applies to the next video |

## 1. Build toolchain

- [x] Install JDK 17 and the Android SDK (platform 35, build tools, platform tools).
- [x] `./gradlew assembleDebug` passes; `apk/PixelScroll.apk` is created.
- [x] Install the APK on a phone; it opens and shows "Pixel Scroll". Done on the vivo test phone.

## 2. Progress detection test

- [x] Debug-only test service: logs nodes (class, view id, bounds, text, description, `RangeInfo`), media sessions, and one screenshot a second. "Start progress test" on Home; results zipped by "Share results". Checked on an emulator.
- [ ] Run it on 3 Shorts and 3 Reels of different lengths. Shorts done (45 s, 51 s, 1 min 2 s); Reels still to run.
- [ ] Write `docs/progress-detection-test.md`: the working source per app, its view ids or pixel region, update rate, battery cost. Written for YouTube; Instagram section waits for the Reels run.
- [ ] Replace the guessed marker view ids in `service/TargetScreens.kt` with the real ones. YouTube confirmed; Instagram open.

## 3. Scroll decision logic

- [x] `scroll/ScrollDecider`: pure Kotlin, no Android types.
- [x] Input: progress samples (position, length, or loop signal) and time. Output: "swipe now" or "wait".
- [x] Unit tests: end reached, loop back to 0, paused, user swiped to a new video, no progress (fixed delay).

## 4. Target screen check

- [x] `service/TargetScreenDetector`: exact package names for YouTube and Instagram, then the Shorts or Reels screen check. Marker view ids live in `service/TargetScreens.kt`.
- [x] Logic takes a simple snapshot of node data, so tests need no phone.
- [x] Unit tests: Shorts, Reels, normal YouTube video, Instagram feed, other apps.

## 5. Swipe gesture

- [x] `PixelScrollAccessibilityService` with gesture permission only.
- [x] `service/SwipeDispatcher`: one upward swipe from 80% to 20% of the screen height, sized from the current window. Full display for now; task 6 sizes it from the target window for split screen.
- [x] Debug-only "Start swipe test": waits for YouTube or Instagram to open, then swipes 3 times, 8 s apart. A fixed countdown was too short to reach Shorts on a phone.
- [x] Phone check: the swipe test moved YouTube Shorts to the next video 3 times on a vivo phone (1080 x 2400). Instagram Reels not yet tried.

## 6. Scroll engine

- [x] YouTube: progress-bar pixels from a screenshot (`progress/ProgressBarReader`), smoothed by a median of three, plus the length from the seek bar description. Instagram: fallback delay until its source is known.
- [x] `service/ScrollEngine` wires tasks 3, 4 and 5 into the service, using the settings from task 7. New videos are detected from scroll events, a change in length, or 3 s after our own swipe.
- [x] Stops on screen off and when leaving the target app; clears state when the service disconnects.
- [ ] Phone check: the screen check is true only on Shorts and Reels.
- [ ] Phone check: works with the Pixel Scroll app closed; nothing happens on normal YouTube videos, the home screen or chats.

## 7. Settings store

- [x] `data/SettingsRepository` on DataStore: `enabled`, fallback delay (3 to 120 s, default 15 s), per-app on/off, theme. Also random variation, sleep timer and watch-time reminder, to match the Settings design.
- [x] Exposed as a Flow for the UI and the service.
- [x] Unit tests for defaults, writes and limits. Stats storage (`data/StatsRepository`) added with tests too.

## 8. On and off controls

- [x] Service reads `enabled`, `paused` and the per-app switches from task 7 and stops at once when turned off.
- [x] Persistent notification with Pause/Resume and Stop; notification permission on Android 13 and later. Checked on an emulator.
- [x] Quick Settings tile showing On, Off or Set up. Checked on an emulator: tile, notification and app stay in step.

## 9. Setup screens

- [x] Theme: Figtree font, type scale, spacing and shapes from the design system.
- [x] Shared components: Button, PageDots, TipCard.
- [x] Welcome, How it works (3 slides), Accessibility access, Setup complete; shown once, ending at Home after "Go to home" or "Not now". Checked on an emulator in light and dark.

## 10. Main screens

- [x] Shared components: Switch, Slider, ListItem, PowerCard, StatusRow, StatTile, BarChart, SegmentedButton, ChoiceChips, NavigationBar.
- [x] Home (with the service-off error state), Settings, Stats (with stats storage and the empty state), About, privacy policy and licences. Checked on an emulator in light and dark; Stats with data checked by unit tests only.
- [x] App, notification and tile icons from the Logo assets.
