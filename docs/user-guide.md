# Pixel Scroll user guide

Pixel Scroll watches YouTube Shorts with you and moves to the next Short when the current one ends. You don't need to swipe.

## What you need

- An Android phone with Android 8.0 or later. Ending each Short at the exact moment needs Android 11 or later; on older phones, a Short moves on about 1 s after its length.
- The YouTube app.
- The file `PixelScroll.apk`.

Pixel Scroll does not work with Instagram Reels or other apps.

## Install

1. Copy `PixelScroll.apk` to your phone.
2. Open it and tap **Install**. If Android asks, allow installs from this source.
3. Open **Pixel Scroll**.

## First-time setup

### 1. Welcome

Tap **Get started**.

### 2. Turn on Accessibility access

Pixel Scroll needs Accessibility access to see when YouTube Shorts is open and to swipe to the next Short.

1. Tap **Open Settings**.
2. Go to **Accessibility**, then **Downloaded apps** (on some phones: **Installed services**), then **Pixel Scroll**.
3. Turn on **Use Pixel Scroll**.
4. If Android asks to allow full control, tap **Allow**.
5. Go back to Pixel Scroll. It opens Home by itself.

On some phones, including vivo, the switch turns itself off the first time. If it does, turn it on again.

Tap **Not now** to skip this step. Auto-scroll can't run until you turn access on.

### What Accessibility access is used for

It does:

- see which app is open;
- read the Short's progress bar;
- send a swipe gesture.

It never:

- reads your messages;
- collects personal data;
- sends anything off the phone. Pixel Scroll has no internet access.

## Using Pixel Scroll

1. On Home, check the card says **Auto-scroll is ON**.
2. Open YouTube and go to **Shorts**.
3. Watch. When a Short ends, Pixel Scroll swipes to the next one.

You can still swipe by hand at any time. Pixel Scroll starts timing the new Short from there.

If you pause a Short near its end, Pixel Scroll waits for you.

## The Home screen

### Auto-scroll card

Tap the card to turn auto-scroll on or off.

| Card says | Meaning |
| --- | --- |
| Auto-scroll is ON | Shorts move on when they end |
| Auto-scroll is OFF | Pixel Scroll does nothing |
| Auto-scroll is OFF (grey, switch disabled) | Accessibility access is off; turn it on first |

### Status row

| Row | Meaning | What to do |
| --- | --- | --- |
| Green, "Accessibility service connected" | Pixel Scroll is running | Nothing |
| Red, "Accessibility service is off" | Access is off, or Android stopped the service | Tap **Fix**, then turn Pixel Scroll off and on in Settings |

### Fallback delay

Pixel Scroll reads each Short's length. When it can't, it moves on after the fallback delay.

- Drag the slider to set it, from 3 s to 120 s. The default is 15 s.
- When you let go, a message confirms the new value for 3 seconds, for example "Delay set to 22 s."

## Troubleshooting

| Problem | Try this |
| --- | --- |
| Shorts don't move on at all | Check Home: card ON and status row green. If the row is red, tap **Fix** |
| It stopped after I installed a new version | Installing turns Accessibility access off. Turn it on again |
| Every Short moves on after 15 s, not at its end | Pixel Scroll can't read the length on this screen. Tell the developer your phone's language and Android version |
| The switch in Settings turns itself off | Turn it on again; some phones need two tries |

## Privacy

Pixel Scroll keeps two settings on your phone: auto-scroll on or off, and the fallback delay. It has no internet permission, keeps no history, and turns off phone backups for its data.

## Credits

Designed and Developed by Rohit Shukla. Tap the footer in the app to open https://info.rohitshukla.net/.
