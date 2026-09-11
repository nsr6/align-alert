# AlignAlert

An offline-first Android companion for clear aligners and daily medication.

## What it does

- Keeps a live continuous-wear clock and only unlocks the “take out” action after four hours.
- Reminds you to put aligners back in one hour after a break.
- Tracks the active tray and prompts you after ten days to start the next tray.
- Supports any number of medicines, each with one or more daily reminder times and a one-tap taken log.
- Recreates pending reminders after a device restart or app update.

## Notification accuracy

On first launch, tap the bell to allow notifications, then the gear to allow **Alarms & reminders**. Exact alarms let Android deliver the four-hour, one-hour, tray, and medication reminders as accurately as the operating system permits. If exact alarms are unavailable, AlignAlert falls back to Android's battery-friendly alarm scheduling.

The 4-hour “food break unlocked” reminder is constrained to 8:00 AM–9:00 PM. If the four-hour mark falls outside that window, it is held for the next daytime window.

## Build and test on your Android phone — no Android Studio

This repository includes a Gradle wrapper and a GitHub Actions workflow, so Android Studio is optional.

### Easiest: GitHub Actions

1. Create a **private** GitHub repository and upload this folder (including `.github/workflows/build-apk.yml`).
2. Open the repository's **Actions** tab and run **Build Android APK**; it also runs automatically whenever you push to `main` or `master`.
3. Open the completed run and download the `AlignAlert-debug-apk` artifact.
4. Move `app-debug.apk` to your phone, open it from Files/Downloads, and allow that app to install unknown apps when Android asks.
5. Open AlignAlert, allow notifications from the bell icon, then allow **Alarms & reminders** from the gear icon.

The debug APK is safe for personal testing and does not require publishing through Google Play.

### Command line

On any computer with Java 17 and the Android command-line SDK installed, run:

```sh
./gradlew assembleDebug
```

The installable APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
