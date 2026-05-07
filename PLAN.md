# Plan

Current state and what comes next. Keep this updated as decisions are made.

## What exists

Simple mode is complete:

- Large-button home screen with Calls and Messages
- Local contact storage with add/edit/delete (with confirmation) and mutex-safe writes
- Admin area behind a triple-tap, PIN-protected once set, open until then
- PIN hashing: PBKDF2WithHmacSHA256 with random salt; exponential lockout after failures; legacy SHA-256 fallback for old installs
- Setup flow: set as home app, pin shortcut, finish
- Native launcher detection: captured during setup, stored, accessible via admin one-tap
- Diagnostics: battery, network, dialer/SMS package names
- Pinned shortcut on first debug launch to simulate Play Store install behaviour
- App visible in native launcher's app drawer via CATEGORY_LAUNCHER

## Architecture decisions

**One app, three layers:**

1. **Simple mode** — built. Mum/user-facing. Home always returns here.
2. **Standard mode** — not yet built. A curated, distraction-reduced app grid for scheduled off-session time. The person owns and configures it themselves. No admin restriction.
3. **Native launcher** — one-tap handoff from admin. The phone working normally. Home still returns to Mum Launcher. Carer uses this.

Our app stays the default launcher permanently. "Native launcher access" is just launching the old launcher as a regular app — no launcher switching required.

Kiosk mode was removed. Lock task without device-owner does nothing useful, and device-owner provisioning requires ADB, which is out of reach for most users.

**Name:** Working name is **Dial It Back** (pun on phone/simplifying). Mum Launcher is the current package/string name and can be updated when publishing is closer.

## What's next

### Standard mode (middle layer)

The person's own home screen for off-session time. Design decisions to make:

- Shows all installed apps automatically, or person curates what appears?
- Current lean: show all apps, let them hide ones they don't want to see (hiding temptation is a deliberate choice, not a restriction)
- Paged grid layout
- Person can reorder apps
- No widgets, folders, or gesture nav — feature gap is intentional, this is a focus layer not a full launcher

### Scheduling

Switches between Simple and Standard mode on a timer. Key questions:

- How does the person set a schedule? (time blocks, recurring, manual override?)
- What does the transition look like? (silent switch, notification, countdown?)
- Can the person extend or skip a session?

### Further out

- Import/export contacts and settings (backup)
- Proper release signing setup
- Play Store submission (name needs to be finalised first)
- Screenshots and store listing copy

## Open questions

- Standard mode name — "Standard", "Open", "Relaxed"? Needs to feel natural to the person, not clinical.
- Whether Standard mode needs any scheduling UI of its own, or scheduling is purely an admin/setup concern.
- Whether the carer flow (admin → use normally) needs a more prominent entry point than the admin screen.

## Known rough edges

- Screenshots in docs are from an earlier build with dummy contact data — need updating once Standard mode exists.
- `SUPPORTED_DEVICES.md` references the old Moto G31 prototype; update to G85.
- Package name is still `com.daveharris.mumlauncher` — fine for now, update before store distribution.
