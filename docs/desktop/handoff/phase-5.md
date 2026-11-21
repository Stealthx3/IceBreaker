# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md` (Phase 5 section). Earlier phases: `phase-0.md` … `phase-4.md`.

**Updated:** 2026-11-21 (parked for usage mid-unit 5a). Steps 1–4 done; step 5 in progress.

## State

- **Cowork pause is over:** the owner resumed development on 2026-11-21. Pacer state: `working`.
- **Step 4 closed** on beta users' test data (owner, 2026-11-21). No republish: Pages already serves the build of `311679236` (issue #3 fix), and no code changed since.
- **Git:** `main` is 1 commit ahead of `origin/main` before this unit's commit; push only at the phase close.
- **Other sessions' uncommitted files:** `docs/desktop/flathub-plan.md` (app-id line from 2026-11-14), `scripts/desktop/vmlab/lab.sh`, `gm-define.sh`, `qemu-venus.sh`. Leave them alone.

## Scope and exit criterion

Owner decision 2026-10-27: Linux adaptation and compatibility, not a 1:1 clone. Exit: install, wizard, sync, downloads, launches, save sync and a clean exit on GNOME, KDE and SteamOS (desktop and Game Mode); `flatpak-builder-lint` clean; offline CI build for x86_64 and aarch64; Flathub PR merged.

Plan (`DEVLOG.md`, Phase 5 plan): 1 wizard wording, 2 Close Argosy, 3 thumbnails, 4 Linux desktop and Steam Game Mode compatibility (done); 5 upstream v2.31 merge and Linux adaptations (in progress); 6 Flathub submission with the IceBreaker rename.

## Step 5: units

Triage of every upstream commit: `docs/desktop/upstream-2.31-triage.md` (uncommitted until owner says; read it first). Owner decisions: `DEVLOG.md`, 2026-11-21.

- **5a. Merge v2.31.0 (in progress, parked for usage 2026-11-21).**
  - Worktree `../argosy-desktop-merge` (next to this checkout), branch `merge/upstream-2.31`, WIP merge commit `c4f1433b1` (parents `main` 56b74f508 and v2.31.0). Amend it as work continues; fast-forward `main` only once the gates pass. Submodules are initialised there (Sigil at upstream's 794d99f).
  - Done: all 200 conflicts resolved; upstream's new files placed (`app/src/main` → `shared`, 6 Android-only ones in `androidMain`); seams for `BackgroundGames`, `HardwareStatsReader`, `LidSleepGuard`, `RadioController`, `PrivilegedAccess`, `GameLaunchOptions`, `padBattery()`, `hostIsChangingConfigurations()`, `RomScanActivity`; `SoundFeedbackManager` implements `AppEventFeedback`/`SfxCacheCleaner`; desktop binds those plus `LibrarySyncNotifier`; tokens regenerated; new Interface/Navigation rows (screen orientation, quick panel in emulators, touchpad) hidden on desktop.
  - Caught: rename detection had paired 11 upstream files with the byte-identical v2.24 copies under `spikes/compose-desktop-library/src/upstream/`; re-merged into the real files with `git merge-file`, spikes restored. `scripts/desktop/merge/landing_check.py 98562a1e7 origin/upstream` now reports only `UserPreferencesRepository` (expected: its block lives in `UserPreferences.kt`/`PreferenceEnums.kt`).
  - **Next:** `:shared:compileKotlinDesktop` has 226 errors, almost all "expect drift": upstream added members to Android classes our `expect` declarations mirror (`DisplayAffinityHelper` usableDisplayId/launchDisplayId/bothScreensDisplayId/panelMirroredToExternal, `PlatformIntent.getBooleanExtra`, StatFs `blockSizeLong`, `PlatformKeyEvent` constructor/flags, `SOURCE_MOUSE`/`SOURCE_TOUCHPAD`/`supportsSource`, `SteamPathResolver` without `SteamInstallTarget`, `isImeVisible`, `SocialOverlayService`, `FocusAccessibilityService`, Coil 2 → Coil 3 in `ScreenshotImage`/`ShowcaseMotionLayers`, `isPackageMissing`, `userScreenshotTargetFile` args, `DesktopFileAccessLayer.prepareSaveAccess` return type). Add each to the `expect` plus a desktop actual; then the Android compile, the push gate and unit 5b.
  - Helpers and notes: `scripts/desktop/merge/README.md` (in the merge branch).
- **5b. Verify the merge:** push gate (`:shared:desktopTest`, `testDebugUnitTest`), host walk, testbed save-sync live check (`GET /api/saves`, negotiate no_op twice), and the session-end rework (launch, quit, relaunch during upload).
- **5c. Catalog and Eden:**
  - Catalog (data, served from GitHub): Vita3K, ScummVM, PICO-8, xemu; Flycast VMU, Azahar/Lime3DS and PPSSPP save paths; m3u multi-disc launch per emulator.
  - Eden: Switch keys and firmware installed into Eden's folders.
- **5d. RetroArch core installs:** fetch the core a launch needs from the libretro buildbot into the RetroArch Flatpak's `cores/` folder. Check the Flatpak folder grant first.
- **5e. Screenshot upload:** watch each emulator's screenshot folder (new catalog field) during the supervised session, then hand off to upstream's shared upload queue.
- **5f. Live hardware readings** (`/proc`, `/sys/class/hwmon`, `power_supply`, `gpu_busy_percent`) and the **Connections page as status only**.
- **5g. Handheld seat picker:** SDL3 built-in pad detection (Deck 28de:1205, Legion Go, ROG Ally, MSI Claw).
- **5h. Games kept running and Resume, everywhere including Game Mode; the Home combo** (SDL3 combo raises the launcher and offers quit or keep).
- **Not in step 5:** Brightness stays out. Connections control, Wayland overlays and v3.0 are not part of this step.
- **Step close:** `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --step-close 5.5`.

## Step 6 (IceBreaker rename and Flathub)

- **Rename:**
  - App id `io.github.stealthx3.IceBreaker`; repo renamed to `Stealthx3/IceBreaker` (owner, 2026-11-21).
  - Work list: `IceSuite/IceBreaker/rebrand/RENAME-CHECKLIST.md`. Art: `IceSuite/IceBreaker/brand/deployment/`.
  - Update IceBreaker `DECISIONS.md` D-004 to decided when the rename starts.
- **Order:**
  1. Rename in code (desktop only).
  2. Move beta users with an end-of-life rebase.
  3. Update links.
  4. **Add to Steam** (`docs/standards/STEAM-SHORTCUT-STANDARD.md`): vendor the helper unchanged, copy the five Steam PNGs, write `steam-shortcut.json`, add the manifest module and the three Steam `finish-args`, build the first-run offer and Settings rows in Compose, run the fake-root test, then test with real Steam in the VM lab and in Game Mode.
  5. Screenshots, lint, `--step-close 5.6`, Flathub PR (owner go-ahead).
- The test shortcut "Argosy Desktop (test)" in the owner's Steam (backup `build/gamemode/shortcuts.vdf.bak-20261108`) is removed or replaced once the real shortcut works; edit only with Steam closed.

## Phase close: history rewrite (owner decision 2026-11-21)

Before the Phase 5 push: `node scripts/pii-scan.mjs history --write-rewrite-files` (inputs go to the private config folder, never the repo); in a **fresh clone**, `git filter-repo --replace-text … --replace-message … --mailmap …` over all branches; re-run `history` in the clone until our commits are clean (upstream's own commits keep their content); force-push `main` once, with the owner's confirmation. Pages (`gh-pages`) is not rewritten. PII Redaction Standard: `docs/standards/PII-REDACTION-STANDARD.md`.

## Carried from step 4 (not verified)

- `HostGameProcess.stopAll` ending a Flatpak emulator on exit.
- EmuDeck detection on the owner's host.
- Assign BIOS with real RomM firmware.
- The folder picker's Back row on screen.
- Snap emulators render black under the tagger.
- Steam Game Recording stalling audio in nested gamescope.

## Gotchas

- Never run two Gradle or bundle builds at once (they share `desktopApp/build`).
- A filtered test run must target `:app:testDebugUnitTest`; `quiet.sh` can print exit 0 on a failed build. Check for BUILD SUCCESSFUL and the test XML.
- Nested Game Mode: `gamescope -W 1920 -H 1080 -w 1920 -h 1080 -e -- steam -gamepadui`. Screenshots only through `gamescopectl screenshot`, never full-desktop captures.
- Walks: `scripts/desktop/check/pad-walk.sh flatpak <out> ...`; run log `build/desktop-logs/*desktop-run.log`.

## Where things are

- Desktop code `shared/src/desktopMain`, app `desktopApp/`, Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Upstream refs: `origin/upstream` (v2.31.0), `rommapp/main` (fetched from rommapp; v3 betas).
