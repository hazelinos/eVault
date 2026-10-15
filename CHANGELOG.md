# Changelog

All notable changes to eVault are documented here.

## [1.3.8] — 2026-10-15 (revised same day)

### Clone flow

- **PILIH CLONE** — eVault auto-detects clone data on shared storage (internal storage / external SD card), like Root Explorer: with All Files Access granted once, it scans every storage volume for folders containing `SaveData/AUTH/online_user_id_data.dat` and lets the user pick the clone. No folder picker, no root, no Shizuku. All AUTH operations (SWITCH, SIMPAN, AUTO LOGIN, HAPUS DATA, BUAT BARU) work against the clone unchanged.
- **EXPORT TAR** — packs the active clone's game data (`assetpacks/` + `SaveData/`) into a KONAMI.TAR-style `.tar` file for backup/transfer into another clone. Streamed, so a ~2 GB export never holds file contents in memory. The tar contains game data only — no AUTH file.
- Launch behaviour is unchanged: MASUK EFOOTBALL still launches the selected game package.
- Limitation (honest): clone data inside a cloner's private storage (`/data/data/...`) cannot be reached by any non-root app, so eVault only lists clones whose data is on shared storage.

### Maintenance

- Bumped Android version from **1.3.7** to **1.3.8** (version code 29 → 30).
- v1.3.8 was revised the same day: the first clone-flow build used a SAF folder grant; it was replaced with All Files Access + fully automatic volume scanning (no folder picker). Same version, fixed APK.

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
