# Mum Launcher Prototype

Minimal Android launcher for a simplified phone setup.

It was built for a real single-user accessibility scenario, but the code is intentionally generic enough to reuse, adapt, and learn from.

## Who this is for

This repo is for people who want:

- a very simple Android launcher with only a couple of obvious actions
- a codebase to borrow from for kiosk or accessibility-oriented launcher work
- a practical prototype rather than a broad commercial launcher product

## What it does

- Replaces the normal Android home screen with a two-button launcher.
- Keeps contacts local to the app.
- Uses the system Phone and Messages apps for actual call/SMS handling.
- Hides admin access behind a PIN-protected flow.
- Supports best-effort kiosk mode when the app is device owner.

## Current state

- Tested on a Motorola Moto G31 running Android 12.
- Intended as a practical prototype rather than a finished store-ready product.
- Focused on one calm, low-friction interaction model rather than broad Android compatibility.
- Suitable for open-source reuse, with device-specific kiosk caveats.

## Local setup on a fresh Mac

1. Install JDK 17.
2. Install Android Studio.
3. Install Android platform tools.
4. Open the project in Android Studio once so it can install SDK Platform 35 and the required build tools.
5. Enable Developer Options and USB debugging on the target phone.

If you prefer terminal-only setup, make sure:

- `JAVA_HOME` points to a JDK 17 installation
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to a valid Android SDK
- `adb` is on `PATH`

Then build with:

```bash
./gradlew assembleDebug
```

If Java is installed through Homebrew, this works well on Apple Silicon Macs:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build outputs

- Debug APK:
  - `app/build/outputs/apk/debug/app-debug.apk`
- Local release APK:
  - `app/build/outputs/apk/release/app-release.apk`

Build a local release APK with:

```bash
./gradlew assembleRelease
```

The current release build is signed with the local debug keystore so it can be installed easily during prototyping. Replace that signing setup before any Play Store or wider public distribution.

## Install on a device

```bash
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```

Or for the local release build:

```bash
adb install -r "app/build/outputs/apk/release/app-release.apk"
```

## Using the app

1. Install the APK.
2. Set `Mum Launcher` as the default Home app.
3. Complete the one-time setup flow.
4. Add the contacts you want available.
5. Triple-tap the small cog to open admin when needed.

## Kiosk and device-owner notes

- Set the app as the default Home app from Android Home settings.
- Enable device admin from the setup flow if prompted.
- For stronger lockdown, provision the app as device owner.
- Keep ADB enabled while prototyping so you can recover from kiosk misconfiguration.
- After provisioning device owner, enable `Kiosk mode enabled` in admin.

Example command:

```bash
adb shell dpm set-device-owner com.daveharris.mumlauncher/.MumDeviceAdminReceiver
```

## Device support and limitations

- Basic launcher behavior should be portable.
- Kiosk behavior is not guaranteed to be identical across manufacturers.
- This repository currently treats the Moto G31 on Android 12 as the reference device.

See [SUPPORTED_DEVICES.md](/Users/daveharris/Documents/New%20project/SUPPORTED_DEVICES.md).

## Privacy

Contacts and settings are stored locally on the device. The app does not include analytics, accounts, or cloud sync.

See [PRIVACY.md](/Users/daveharris/Documents/New%20project/PRIVACY.md).

## Public repo suitability

This project is suitable for an MIT-licensed public repository if the goal is:

- sharing a working accessibility-oriented launcher prototype
- giving others a base for kiosk launcher experiments
- letting people borrow and adapt the code

It is not yet a polished general-purpose product. Before wider public promotion, I would recommend:

- adding screenshots
- making release signing fully user-owned rather than using the debug keystore

So: good as an open prototype/codebase, not yet positioned as a fully supported consumer app.
