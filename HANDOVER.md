# Mum Launcher Handover

## What this build does

- Replaces the normal Android home screen with a very simple launcher.
- Shows only two large actions on the main screen: `Calls` and `Messages`.
- Stores contacts inside the app rather than in Android Contacts.
- Opens the system dialer and system messaging app for the actual call and SMS flow.
- Supports a hidden admin area protected by PIN.
- Supports best-effort kiosk mode when the app has device-owner privileges.

## Before handing the phone over

1. Confirm the correct contacts are present.
2. Confirm the admin PIN is written down somewhere safe.
3. Confirm calling and SMS work on the live SIM/provider.
4. Confirm the app is still the default Home app after reboot.
5. Confirm the phone battery, volume, ringtone, and text tone are set sensibly.
6. Confirm kiosk mode is set the way you want it.

## Daily use

- `Calls` opens the contact list and then the system dialer for the chosen number.
- `Messages` opens the contact list and then the system SMS app for the chosen number.
- Triple-tap the small cog to open admin.

## Recovery notes

- If the app stops being the Home app, open Android Home settings and set `Mum Launcher` as default again.
- If kiosk mode becomes too restrictive, open admin and turn off `Kiosk mode enabled`.
- Keep ADB access available while testing new builds.
- Device-owner kiosk behavior is device-specific and works best when tested on the exact handset in use.

## Installable APKs

- Debug APK:
  - `app/build/outputs/apk/debug/app-debug.apk`
- Local release APK:
  - `app/build/outputs/apk/release/app-release.apk`

The local release APK is signed with the debug keystore for convenience. Replace that signing setup before any wider distribution.
