# Plan

Current state and what comes next. Keep this updated as decisions are made.

## What exists

Simple mode and Relaxed mode are complete:

- Large-button home screen with Calls and Messages
- Local contact storage with add/edit/delete (with confirmation) and mutex-safe writes
- Admin area behind a triple-tap, PIN-protected once set, open until then
- PIN hashing: PBKDF2WithHmacSHA256 with random salt; exponential lockout after failures; legacy SHA-256 fallback for old installs
- Setup flow: set as home app, pin shortcut, finish
- Native launcher detection: captured during setup, stored, accessible via admin one-tap
- Diagnostics: battery, network, dialer/SMS package names
- Pinned shortcut on first debug launch to simulate Play Store install behaviour
- App visible in native launcher's app drawer via CATEGORY_LAUNCHER

Relaxed mode is complete:

- Preset data layer (Preset, PresetRepository) with full CRUD via DataStore
- PresetManagerScreen, PresetEditorScreen, NewPresetNameDialog, PresetPickerDialog, FirstRunPresetDialog
- Relaxed home screen driven by active preset (paged grid, horizontal/vertical scroll toggle)
- Admin controls: "Show Relaxed mode button", "Manage presets", "Use phone normally" (native launcher handoff)

## Architecture decisions

**One app, three layers:**

1. **Simple mode** — built. Mum/user-facing. Home always returns here.
2. **Relaxed mode** — built. Curated app grid driven by presets. The person owns and configures it themselves. No admin restriction.
3. **Native launcher** — one-tap handoff from admin. The phone working normally. Home still returns to Mum Launcher. Carer uses this.

Our app stays the default launcher permanently. "Native launcher access" is just launching the old launcher as a regular app — no launcher switching required.

Kiosk mode was removed. Lock task without device-owner does nothing useful, and device-owner provisioning requires ADB, which is out of reach for most users.

**Name:** Working name is **Dial It Back** (pun on phone/simplifying). Mum Launcher is the current package/string name and can be updated when publishing is closer.

## What's next

### Scheduling

Switches between Simple and Relaxed mode on a timer. A Codex prototype was built and archived
at `archive/codex-focus-mode-experiment` — see `CRIB.md` for what to reuse. The scheduling
logic (`ModeScheduler.kt`) and notification infrastructure (`SchedulePromptController.kt`) are
solid starting points.

Key design questions still open:
- What does the transition look like? (silent switch, notification prompt, or countdown?)
- Can the person extend or skip a session from the home screen?
- Does the schedule live in admin only, or does the person configure it themselves?

### Further out

- Import/export contacts and settings (backup)
- Proper release signing setup
- Play Store submission (name needs to be finalised first)
- Screenshots and store listing copy

## Open questions

- Whether scheduling UI belongs in admin only, or the person can configure their own schedule.
- Whether the carer flow (admin → use normally) needs a more prominent entry point than the admin screen.

## Known rough edges

- Screenshots in docs are from an earlier build with dummy contact data — need updating once Standard mode exists.
- `SUPPORTED_DEVICES.md` references the old Moto G31 prototype; update to G85.
- Package name is still `com.daveharris.mumlauncher` — fine for now, update before store distribution.
