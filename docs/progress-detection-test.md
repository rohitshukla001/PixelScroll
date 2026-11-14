# Progress detection test

> History: written in Nov 2026 when Pixel Scroll also targeted Instagram Reels. Since 14 Nov 2026 the app supports YouTube Shorts only; see the README for the current app.

YouTube Shorts can be read on the test phone: the accessibility tree gives each Short's total length, and the progress bar's pixels give the position and the loop. Instagram Reels is not tested yet.

Run: 13 Nov 2026, vivo I2214, Android 16 (API 36), 1080 x 2400. 178 s, 169 samples, 169 screenshots, all in YouTube Shorts. Three Shorts watched: 45 s, 51 s and 1 min 2 s.

## Results by source

| # | Source | YouTube Shorts | Instagram Reels |
| --- | --- | --- | --- |
| 1 | Accessibility nodes | Partly works. The seek bar's description gives the length, for example "0 minutes 2 seconds of 0 minutes 45 seconds". The position in it updates only now and then (stayed at 0 s for 43 s, then jumped to 45 s). No `RangeInfo`, no time text | Not tested |
| 2 | Media session | Does not work. YouTube's session reports duration 0, position 0, state 0 for Shorts | Not tested |
| 3 | Progress-bar pixels | Works. The white fill grows about 2% a second and drops to near 0 at each loop, for all three Shorts | Not tested |
| 4 | First-frame match | Not needed for YouTube | Not tested |

## YouTube Shorts details

- Screen markers: `com.google.android.youtube:id/reel_recycler` and `reel_player_page_container` appear on every full dump of the Shorts screen. These match the guesses already in `service/TargetScreens.kt`.
- Seek bar: a `SeekBar` with no view id inside `com.google.android.youtube:id/reel_time_bar`, bounds `[0,2085][1080,2217]`. Its content description holds "X minutes Y seconds of A minutes B seconds".
- Progress bar: `com.google.android.youtube:id/reel_progress_bar`, bounds `[22,2146][1058,2152]` (6 px high). Seen in the tree only once, but visible in every screenshot.
- Pixel reading: on a half-size screenshot, rows y = 1070 to 1079, the longest light-grey run from the left edge (all channels above 150, channel spread under 40). Measured fill per second:

| Short | Samples | Fill at start | Fill at end | Loop seen |
| --- | --- | --- | --- | --- |
| 45 s | ticks 6 to 50 | 0.00 | 1.01 | Yes, drop to 0.01 at tick 51 |
| 51 s | ticks 54 to 104 | 0.01 | 1.01 | Yes, drop to 0.01 at tick 105 |
| 1 min 2 s | ticks 109 to 169 | 0.01 | 0.99 | Yes, drop to 0.01 at tick 170 |

- Event rate: window-content-changed events from YouTube are 0 in most seconds, up to 22 in one second. Too irregular to use as a clock.

## Decision for task 6

- YouTube Shorts: read the fill from a screenshot of the progress bar once a second, and pass it to `ScrollDecider.onProgress`. The bar's bounds come from `reel_progress_bar` (or the seek bar) when present, else from the last known position. Use the seek bar's description for the length, to set the end threshold and to log it.
- Screenshots need Android 11 or later. The test phone runs Android 16.
- Instagram Reels: run the test again on Reels only, then decide.

## Other findings

- The probe kept YouTube as the app in front after the user went to the home screen (last sample shows the launcher and clock widget). Task 6 must read the package of the active window's root node, not only the window-state events.
- The run ended with "service off" at 178 s, not the 180 s limit. Cause unknown: either the user turned the service off, or the phone stopped it.
