# Pixel Scroll implementation plan

> History: written in Nov 2026 when Pixel Scroll also targeted Instagram Reels. Since 14 Nov 2026 the app supports YouTube Shorts only; see the README for the current app.

Pixel Scroll is built in six phases. Phase 0 is a test that decides how the app knows when a video ends. Every later phase depends on its result.

The work was tracked as 10 tasks.

Sources: the [feature list](https://claude.ai/artifact/PZrsvE9YjKxX2hm3URMSgt) and the Pixel Scroll design system.

## Decisions

| Area | Decision | Why |
| --- | --- | --- |
| Distribution | Personal use, installed as an APK. No Google Play work | No plan to publish |
| Language and UI | Kotlin, Jetpack Compose, Material 3 | The accessibility service is native Android; one language for the whole app |
| Modules | One `app` module | The app is small; more modules add build work and no value yet |
| Settings storage | Jetpack DataStore (Preferences) | Small key-value settings, observable as a Flow by both the UI and the service |
| Stats storage | Decided in Phase 4 (DataStore or Room) | Depends on how many days of history the Stats screen keeps |
| Background work | The accessibility service only | The system keeps a bound accessibility service running; no foreground service needed |
| On-screen UI in YouTube or Instagram | None | Design rule from the feature list |
| Icons | `material-icons-extended`, Rounded set | Closest Compose match to the design's Material Symbols Rounded |
| Minimum Android version | 8.0 (API 26) | Already set in the project. Screenshot-based detection needs Android 11 (API 30) |

## Package layout

New code goes under `app/src/main/java/dev/rohitshukla/pixelscroll/`:

```
service/    Accessibility service, screen detection, swipe
progress/   Progress sources: one class per way of reading a video's progress
scroll/     Pure Kotlin logic: when to swipe (no Android types, unit tested)
data/       Settings and stats repositories
system/     Notification and Quick Settings tile
ui/theme/   Colours, type, shapes from the design system
ui/screens/ One folder per screen
ui/nav/     Navigation graph and first-run routing
```

## How the app knows a video ended

The app tries several sources, from cheapest to most expensive, and uses the first one that works for the current app. Phase 0 tests each one.

| # | Source | What it gives | Cost | Needs |
| --- | --- | --- | --- | --- |
| 1 | Accessibility nodes: a progress node's `RangeInfo`, or text and content descriptions with times such as "0:12 / 0:30" | Position and length | Low | Window content access (already planned) |
| 2 | Media session: `MediaSessionManager.getActiveSessions` | Duration from metadata, position from playback state | Low | Notification access, granted once by the user |
| 3 | Progress-bar pixels: `AccessibilityService.takeScreenshot`, then measure how much of the bar is filled | Position as a fraction of the length; a drop to near 0 means a loop | Medium | Android 11 or later; the bar's location from source 1 or fixed per app |
| 4 | First-frame match: keep a small grey copy of the video's first frame, compare later frames to it | Detects the loop back to the start | Medium to high | Android 11 or later; no progress bar needed |
| 5 | Fixed delay | Nothing; swipe after a set time | None | Always works |

Notes:

- Sources 1 to 4 only need to answer "did the video end or loop?". Exact seconds are not required.
- Source 3 works even when the progress node has no `RangeInfo`, as long as its screen bounds are known.
- Source 4 can misfire on videos with a still image. Use it only with a minimum watch time, and only when sources 1 to 3 fail.
- Screenshots are rate-limited by Android. Phase 0 measures the real limit and the battery cost at 1 to 3 screenshots a second.

## Phase 0: progress detection test

Goal: for YouTube Shorts and Instagram Reels, find the cheapest source that detects the end of a video.

1. Install Android Studio (adds JDK 17 and the Android SDK). Run `./gradlew assembleDebug` to confirm the skeleton builds.
2. Build a debug-only test service with `canRetrieveWindowContent` and `canTakeScreenshot`, plus a notification listener.
3. While 3 Shorts and 3 Reels of different lengths play on a real phone, log:
   - every node's class name, view id, bounds, text, content description and `RangeInfo`;
   - active media sessions, their duration and playback position;
   - one screenshot per second, saved to the app's files folder.
4. Check each source against the logs, in the order of the table above. For sources 3 and 4, test the detection on the saved screenshots.
5. Write down the findings: for each app, which source works, its view ids or pixel regions, how often it updates, and its battery cost.

**Done when:** for each app, the findings name one working source, or say that only the fixed delay works.

## Phase 1: scroll engine

Goal: the app swipes to the next video at the right time, with no UI yet. Settings are hardcoded constants.

1. `PixelScrollAccessibilityService` with only the events and flags the chosen sources need.
2. Target check: exact package names, then a check for the Shorts or Reels screen (view ids from Phase 0).
3. `SwipeDispatcher`: one upward swipe from 80% to 20% of the screen height, sized from the current window.
4. `ProgressSource` implementations for the sources Phase 0 chose, tried in order for each app.
5. `ScrollDecider` in `scroll/`: given progress samples, returns "swipe now" or "wait". Handles end reached, loop back to 0, paused (position not moving), user swiped (new video).
6. Fixed-delay fallback when no source works for the current video.
7. Stop on screen off and when the user leaves the target app. Clear all state when the service disconnects.

**Done when:**
- Unit tests for `ScrollDecider` cover end, loop, pause, user swipe and missing progress.
- On a real phone, a 7-second and a 30-second video each play to the end before the swipe.
- Nothing happens on normal YouTube videos, the home screen or chats.
- The engine keeps working with the Pixel Scroll app closed.

## Phase 2: on and off

Goal: the user turns auto-scroll on and off from three places that always agree.

1. `SettingsRepository` (DataStore): `enabled`, plus the settings Phase 4 adds.
2. The service reads `enabled` as a Flow and stops at once when it turns off.
3. Persistent notification "Pixel Scroll is active" with Pause and Stop actions. Ask for the notification permission on Android 13 and later.
4. Quick Settings tile (`TileService`) showing ON or OFF.

**Done when:** turning it off from any one of the notification, the tile or the app updates the other two within 1 second, and scrolling stops.

## Phase 3: screens

Goal: build the eight screens from the design system.

1. Theme: add the Figtree font file and the type scale from `tokens.json`; spacing and shape values.
2. Shared components: Button, Switch, Slider, ListItem, PowerCard, StatusRow, TipCard, StatTile, NavigationBar, PageDots.
3. First-run flow: Welcome, How it works (3 slides), Accessibility access, Setup complete. Add a step for notification access if Phase 0 chose the media session source. Show the flow until the service is enabled once.
4. Main flow: Home, Settings, Stats, About, with the bottom navigation bar.
5. Home detects whether the accessibility service is on and shows the error state with a Fix button when it is off.
6. App icon, notification icon and tile icon from the Logo assets.

**Done when:** each screen matches its design in light and dark themes, on a 412 x 915 dp emulator and one small phone, and every tap target is at least 48 dp.

## Phase 4: settings and stats

Goal: make the Settings and Stats screens work.

1. Settings: fallback delay (3 to 120 s, default 15 s), per-app on/off, theme. Saved in DataStore; the service uses new values at once.
2. Stats: count each swipe and the watch time per app per day. Choose DataStore or Room here.
3. Stats screen: Today, This week, per app, the 7-day bar chart, empty state, Clear stats.

**Done when:** a changed delay applies to the next video without restarting anything, and stats survive an app restart.

## Phase 5: release build

Goal: a signed release APK installed on your phone.

1. Create a release signing key and keep it out of git (`keystore.properties`, already in `.gitignore`).
2. Release build with R8; check that the accessibility service and tile still work after shrinking.
3. Install the release APK on a real phone and use it for a few days.

**Done when:** the release APK runs on your phone for a week of normal use with no crash and no missed swipes on supported screens.

## After the first release

From the feature list's second release: per-app delays, random variation for the fallback delay, sleep timer, watch-time reminder, faster skip for ads, Material You colours from the wallpaper.

## Risks

| Risk | Effect | Plan |
| --- | --- | --- |
| No source detects the end of a video for an app | That app uses the fixed delay only | Sources 1 to 4 in order (see "How the app knows a video ended"); test again after each app update |
| YouTube or Instagram change their view ids or layout | Screen check or progress reading stops working | Keep ids and pixel regions in one file; fall back to the next source, then to the fixed delay |
| Screenshot sources use too much battery | Battery drains faster while watching | Take screenshots only on Shorts and Reels screens, at the lowest rate that still catches the end |
| Phone maker battery savers stop the service | Scrolling stops without warning | Home status row shows the service state; turn off battery saving for Pixel Scroll on your phone |
