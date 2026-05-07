# Setup Guide

How to set up the phone and hand it over.

## Before you start

You need the app installed and your phone connected to a computer via USB with USB debugging enabled. Run:

```bash
./gradlew installDebug
```

## First-time setup

Launch the app from the app drawer. You will see the setup screen. Work through each step:

1. **Set as home app** — opens Android's Home settings. Choose Mum Launcher as the default. This is what makes Home always return here.
2. **Pin to home screen** — adds a shortcut to the native launcher's home screen so you can return easily after using the phone normally.
3. **Note prototype permissions** — no action needed, just a reminder that calls and messages use the system apps.

Set an admin PIN at the bottom of the setup screen. Write it down somewhere safe. If you lose it, clear the app data in Android Settings to reset everything.

Tap **Finish setup**.

## Before handing over the phone

1. Confirm the correct contacts are in the app (admin → Contacts).
2. Confirm the admin PIN is written down somewhere safe.
3. Confirm calling and SMS work on the live SIM.
4. Confirm the app is still the default Home app after a reboot.
5. Confirm battery, volume, ringtone, and text tone are set sensibly.

## Daily use

- **Calls** — opens the contact list, then the system dialler.
- **Messages** — opens the contact list, then the system SMS app.
- Triple-tap the small cog to open admin.

## Using the phone normally (for carers)

Open admin and tap **Use phone normally**. This opens the previous launcher directly. Press Home to return to Mum Launcher at any time, or use the shortcut pinned to the home screen during setup.

## Recovery

- If the app stops being the Home app: Android Settings → Apps → Default apps → Home app → Mum Launcher.
- If the PIN is lost: Android Settings → Apps → Mum Launcher → Storage → Clear data. This resets everything including contacts.
