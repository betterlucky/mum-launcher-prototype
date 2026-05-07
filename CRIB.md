# Crib sheet

## What this is

Dual-mode Android launcher. Simple mode = accessibility launcher (large call/message buttons,
local contacts). Relaxed mode = curated app grid driven by presets. Scheduling switches between
them on a timer. One app, one codebase — deliberate decision.

## Architecture

**Three layers:**
1. **Simple mode** — built. Mum/user-facing. Home always returns here.
2. **Relaxed mode** — built. App grid from a user-configured preset. Person owns it.
3. **Scheduling** — not yet built. Switches between Simple and Relaxed on a timer.

Stay single app. Simple/Relaxed/Scheduled share the same chassis (contacts, admin PIN, setup
flow). Split only if Relaxed grows toward widgets/custom icons/gesture nav — that's a different
product.

## Home screen limitation

When this app is the system launcher, Home always returns here. Relaxed mode is always this
app's grid, not the user's previous launcher. The admin screen has a "Use phone normally"
button that opens the previously-detected native launcher as a regular app.

## Scheduling reference

Codex built a scheduling foundation (archived at tag `archive/codex-focus-mode-experiment`).
Retrieve any file with:

```
git show archive/codex-focus-mode-experiment:app/src/main/java/com/daveharris/mumlauncher/ModeScheduler.kt
```

Key files worth referencing when implementing scheduling:
- `ModeScheduler.kt` — pure scheduling logic: `shouldUseFocusLauncherNow`, `currentScheduledWindow`,
  `currentDuePrompt`, `nextScheduleBoundaryMillis`. Nearly standalone, needs `LauncherSettings`
  fields adding (`launcherMode`, `scheduleDays`, `scheduleStartMinutes`, `scheduleEndMinutes`,
  `focusSessionActive`, `focusSessionAnchor`).
- `SchedulePromptController.kt` — AlarmManager + notification infrastructure. References
  `KioskRecoveryReceiver` (renamed/repurposed); adapt accordingly.
- `ScheduleEditor`, `ModeSelector`, `PromptActionDialog` composables in `MainActivity.kt`.
- `LauncherMode` enum and schedule fields in `SettingsStore.kt`.

## Known state of fixed issues (for reference)

Issues that existed in early builds — all fixed in current main:
- PIN hashing: uses PBKDF2WithHmacSHA256 with random 16-byte salt + exponential lockout.
- ContactRepository race: `Mutex` wraps all read-modify-write operations.
- `buildDiagnostics` in composable: moved to `StateFlow<Diagnostics?>` on ViewModel, refreshed via `LaunchedEffect`.
- Delete confirmation: `AlertDialog` in `ContactCard` before any delete.
