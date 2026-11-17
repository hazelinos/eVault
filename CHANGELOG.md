# Changelog

All notable changes to eVault are documented here.

## [1.4.1] — 2026-11-17

### Figma UI redesign

- Light surfaces, lime action buttons, rounded cards, and a live selected-account summary.
- Separate Settings screen for folders, clone selection, delay, Discord, activity log, and AUTH deletion.
- Account list includes daily login verification labels; Auto Login has a separate progress summary.
- Existing AUTH and Auto Login handlers retained; application ID and signing key unchanged.
- Android 8.0 navigation bar compatibility; version code increased to **33**.

## [1.4.0] — 2026-11-14

### Interface refresh

- Dark slate surfaces with mint accents and grouped AUTH actions.
- Larger rounded buttons with ripple feedback and consistent Auto Login, input, and log styling.
- Existing AUTH, clone selection, and Auto Login handlers are preserved.
- Fixed stray literal newline escapes in AndroidManifest.xml without changing permissions.
- Added pull-request build and lint checks with an APK artifact.
- Android version code increased to **32**; existing application ID and signing key retained.

## [1.3.9] — 2026-11-09

### Clone detection and selection

- Added Android launcher-intent package visibility so installed game launchers can be discovered on Android 11+.
- Recognizes the `ids.patch` package family in addition to `idsphone` and `idspatch`.
- Excludes the official Konami package `jp.konami.pesam` as well as the legacy `epl.idsphone` package from clone selection.
- Added automatic package detection and a clone selector inside **Setup**; the selected clone is remembered for MASUK EFOOTBALL, Create Account, and Auto Login.
- MASUK EFOOTBALL now launches the selected clone directly without opening a selection dialog.
- Requires an explicit clone selection in Setup when no valid selection exists.
- Bumped Android version code from **30** to **31**.

## [1.3.8] — 2026-10-15 (revised twice same day)

### What 1.3.8 ended up as

- **No more pickers**: the PILIH CLONE, PILIH GAME, and EXPORT TAR buttons (added earlier the same day) were removed at the user's request.
- **MASUK EFOOTBALL auto-detects the clone**: it launches the first installed eFootball package that is NOT the original (`epl.idsphone`). No picker, no manual package input, and deliberately no `epl.idsphone` fallback — if no clone is installed it reports "Clone eFootball tidak ditemukan." Detection recognizes the "ids" package family (`ucl.idsphone`, `idspatch`, …) in addition to the Konami/eFootball/PES markers.
- AUTH target handling is unchanged: a manually picked SAF folder (PILIH FOLDER AUTH) plus file-path targets set by the earlier 1.3.8 clone build keep working.

### Maintenance

- Bumped Android version from **1.3.7** to **1.3.8** (version code 29 → 30).
- v1.3.8 was revised twice the same day: first a SAF clone flow, then All Files Access + auto-scan, then the pickers/export were removed in favor of pure auto-detect. Same version, fixed APK.

## [1.3.7] — 2026-10-04

### Bug fixes

- **Auto Login resume** — a session can now be resumed after the app was killed while the game was open, or after a FAILED run. The persisted workflow mode is cleared on completion, failure, and exit so the resume dialog is never permanently skipped.
- **Rotation** — a configuration change no longer aborts an in-flight Auto Login; the activity handles orientation/screen changes in place.
- **Switch Account** — selecting an account no longer fails with "Pilih akun terlebih dahulu." after the library list is reloaded (selection is now resolved by stable account key).
- **Revoked storage access** — a revoked/expired SAF grant no longer crashes the app.
- **Save/Create AUTH** — storage work runs off the UI thread, and a provider that renames created files no longer causes a silent save failure.
- **Save AUTH dialog** — the account-list dialog height is now capped so it is not clipped on short screens.

### Maintenance

- Bumped Android version from **1.3.6** to **1.3.7** (version code 28 → 29).
- Pinned the Gradle wrapper to 8.10.2 to match the Android Gradle Plugin used by CI.
- Untracked build artifacts (`build/`, `app/build/`, `.gradle/`, `.idea/`) from version control.

## [1.3.6] — 2026-10-01

### Auto Login

- Simplified the Auto Login menu: it now offers only *all accounts* or *accounts not yet logged in today*; the single-account option and its picker were removed.

## [1.3.5] — 2026-09-30

### Core behavior

- Atomic AUTH install (replace in place) so a failed copy never leaves the target with no AUTH.
- Create-account backs up and restores the installed AUTH on cancel.
- Safer SAF handling and stricter name validation.
- Webhook hardening: strict URL validation, real JSON id parsing, 404 recovery, and a bounded request queue.
- Android 15 edge-to-edge insets; thread-safe activity log; rotation-safe account selection.

## [1.3.4] — 2026-09-28

### Maintenance

- Restored the stable pre-Cloud eVault core as the release baseline.
- Cleaned up project documentation and release metadata.
- Bumped Android version from **1.3.3** to **1.3.4**.
- Bumped Android version code from **25** to **26**.
- Standardized release artifact naming to `eVault-v1.3.4.apk`.

### Core behavior

- Preserved the local AUTH Library workflow.
- Preserved Save AUTH, Delete Data, Create Account, Switch Account, Auto Login, Login Verification, and Activity Log.
- No Cloud integration is included in this baseline release.

## [1.3.3]

Previous stable core release before Cloud integration work.

---

Release history is maintained through GitHub Releases.
