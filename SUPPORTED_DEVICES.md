# Supported Devices

## Tested device

This project is currently tested on:

- Motorola Moto G31
- Android 12

## Expected compatibility

The app should be broadly usable as a normal launcher on many Android devices that support:

- Android 10 or newer
- setting a custom Home app
- standard system dialer and SMS intent handling

## Kiosk limitations

Kiosk-style behavior is much more device-specific than the basic launcher flow.

Areas that vary by handset and OEM:

- status bar behavior
- navigation bar behavior
- lock task behavior
- device-owner provisioning quirks
- whether transient system bars re-hide exactly as requested

## Recommendation

If you want stronger kiosk behavior, test on the exact phone model you plan to deploy.

Treat the currently tested Moto G31 / Android 12 path as the known-good reference setup for this repository.
