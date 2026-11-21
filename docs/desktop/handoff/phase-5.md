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

- **5a. Merge v2.31.0: merged and building (2026-11-21).**
  - Worktree `../argosy-desktop-merge` (next to this checkout), branch `merge/upstream-2.31`, head `1f2cd7391`. It also contains `main` up to `67c1a03b5` (PII adoption), merged in, so `main` can fast-forward to it.
  - Desktop, Android and desktop-app builds compile; push gate green: 5,691 tests (Android + desktop), 0 failures, 1 skipped. `scripts/desktop/merge/landing_check.py 98562a1e7 origin/upstream` reports only the expected `UserPreferencesRepository` line.
  - How it was done (helpers in `scripts/desktop/merge/`, README there): conflicts resolved; expect/actual drift synced both ways (`expect_drift.py`, `expect_orphans.py`, `sigdiff.py`); blocks Phase 1 had extracted were rehomed (`rehome_blocks.py`, `extracted_deletions.py`); new seams: `BackgroundGames`, `HardwareStatsReader`, `LidSleepGuard`, `RadioController`, `PrivilegedAccess`, `GameLaunchOptions`, `padBattery()`, `hostIsChangingConfigurations()`, `RomScanActivity`, `TransientNotice`, `AccessibilityServiceState`, `isImeVisible()`, `stackedScreenshotTransformation()`; the image wrapper gained `newBuilder`, `memoryCacheKey`, `placeholderMemoryCacheKey`, `transformations`.
  - Known desktop gaps (Android unchanged): stacked DS/3DS screenshot crop and showcase crossfade-from-previous are Android-only; pad battery, hardware readings, radio status and background games are desktop stand-ins until units 5f/5h.
- **5b. Verify the merge (next):** build the merged debug APK (`:app:assembleDebug` in the worktree) and a plain upstream v2.31.0 APK, run `scripts/desktop/check/save-proof.sh` for both on `emulator-5556` (start it per `gotchas.md`) and compare with `proof-diff.sh`; then push gate (`:shared:desktopTest`, `testDebugUnitTest`), host walk, testbed save-sync live check (`GET /api/saves`, negotiate no_op twice), and the session-end rework (launch, quit, relaunch during upload).
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
