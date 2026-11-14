# Performance

What Pixel Scroll costs the phone while it runs, what we did to keep that cost low, and what is still unmeasured.

Pixel Scroll runs an accessibility service in the background. While auto-scroll is on, its scroll engine wakes once a second. The work it does depends on what is on screen.

## Cost per second

| Situation | Work each second |
| --- | --- |
| Screen off | Checks the screen state, then stops |
| Another app in front | Reads the name of the app in front, then stops. It does not read that app's screen |
| YouTube, not on Shorts | One search for the Shorts list, then stops. No full screen walk |
| YouTube Shorts | The search, a full screen walk (view ids, seek bar), plus one screenshot and a progress-bar reading every 3 s; every second in the last 5 s |

Auto-scroll off: the engine does not run at all.

## Where the cost goes

### 1. The screen walk

`ScreenScan` asks Android for each node of YouTube's screen. Each node is a call into another process. The walk stops at 60 levels deep.

- It runs only when YouTube is in front.
- It collects view ids, the seek bar and the progress bar in a single pass.

### 2. The screenshot

The progress bar is read from a screenshot (Android 11+). Android returns the screenshot as a hardware buffer. The app copies it into a normal bitmap of the full screen to read the bar's pixels:

| Item | Size |
| --- | --- |
| Full-screen copy at 1080 x 2400 | about 10 MB (4 bytes per pixel) |
| Pixels actually read | the bar's strip, about 1,000 x 20 |

The engine takes a screenshot every 3 s, and every second in the last 5 s of a Short. The timing follows the video's real position, so it also works when you jump forward or back. For a 30 s Short that is about 13 copies instead of 30 at one per second. An earlier version read only in the last 5 s by the clock: fewer copies (about 5 per Short), but a Short you jumped forward in would replay before the swipe.

Each bitmap is recycled straight after reading. When Android can't allocate the copy, the reading is skipped for that second.

### 3. Accessibility events

The service subscribes to scroll events only, and only from YouTube (`accessibility_service_config.xml`). Window-change and content-change events from other apps no longer wake it.

### 4. The swipe

One 250 ms gesture per Short. If Android cancels it, the engine retries once.

## Changes made for performance (14 Nov 2026)

| Change | Before | After |
| --- | --- | --- |
| App check before the screen walk | Walked every app's screen every second | Walks YouTube only |
| Screenshot timing | One full-screen copy every second on Shorts | Every 3 s, and every second in the last 5 s; follows jumps forward and back |
| Screen check on YouTube | Full screen walk every second | One search for the Shorts list first; full walk only on Shorts |
| Accessibility events | Window, content and scroll events from all apps | Scroll events from YouTube only |
| Error handling | One error could end the service | The second is skipped; status turns red after 5 errors in a row |

## Results

| Check | Result |
| --- | --- |
| Engine run on the emulator against a fake Shorts app, 3 minutes | 22 Shorts swiped, each at its end; 22 gestures completed, 0 cancelled |
| Swipe timing on the emulator | The swipe is timed for 250 ms before each Short ends; no replay seen. The exact landing time was not measured |
| Phone test (vivo I2214, Android 16) | Auto-scroll keeps working past 3–4 Shorts after the crash guard |

## Not measured yet

These numbers have not been measured. Treat any claim about them as unverified.

- Battery drain per hour of Shorts, with auto-scroll on and off.
- CPU time of one tick on the phone.
- Memory use over a long session.

To measure battery, compare `adb shell dumpsys batterystats --charged dev.rohitshukla.pixelscroll` after one hour of Shorts with auto-scroll on and one hour with it off, from a full charge each time.

## Ideas, not done

Each of these would lower the cost further. None is needed today.

- Copy only the bar's strip instead of the full screen. It needs a way to crop a hardware bitmap without a full copy; this has not been tested.
- Read the position from the seek bar text and skip screenshots for long Shorts until the last seconds. YouTube updates that text only now and then, so it can't replace the screenshot near the end.
- Stop the tick loop while YouTube is not in front, and start it again on YouTube's first scroll event.
