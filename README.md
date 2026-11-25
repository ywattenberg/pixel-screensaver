# Drift Clock

Drift Clock is a native Android screensaver for Google Pixel. It follows your
phone’s 12- or 24-hour setting. In clock-only mode, it moves the clock once a minute.

## Install and preview

1. Open this folder in Android Studio and let it sync. Use Android SDK Platform 35,
   Gradle 8.11.1, and JDK 17 or newer. If Android Studio asks for a Gradle
   distribution, select a local Gradle 8.11.1 installation.
2. Enable USB debugging on your Pixel, connect it to your computer, and accept the
   debugging prompt. Run the app on your Pixel.
3. In Drift Clock, tap **Open notification access** and grant access to **Drift
   Clock** if you want media integration. Then start playback in Spotify or
   another media app.
4. Open Drift Clock again and tap **Open screen saver settings**. Turn on the
   screen saver, select **Drift Clock**, and tap **Preview**. Choose when to run
   the screen saver in the same Settings screen.

With Gradle and ADB on your PATH, you can also install it from this folder.

```sh
gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Media controls

Drift Clock uses Android’s active media sessions, so it works with apps that
publish playback status without requiring a separate sign-in. It shows track
details and embedded artwork when available. Play, Pause, Previous, and Next
appear as supported by the selected player. Playback takes priority over a paused
session. A paused session stays visible so you can resume it. Tap the background
or use Android’s Back gesture to exit the screensaver.

## Customize the display

Edit `ClockDreamService.java` to change the clock, media layout, or movement
interval. Change the app’s name and other labels in
`app/src/main/res/values/strings.xml`.
