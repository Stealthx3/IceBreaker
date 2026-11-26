# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md` (Phase 5 section). Earlier phases: `phase-0.md` … `phase-4.md`.

**Updated:** 2026-11-26. Steps 1–4 done; step 5 in progress (units 5a and 5b done; 5c next, after step 6).

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

- **5a/5b. Upstream v2.31.0 merged and verified (done 2026-11-21).** `main` contains it. Details and known desktop gaps: DEVLOG 2026-11-21 ("Units 5a and 5b done"). Merge helpers: `scripts/desktop/merge/` (README). After each upstream merge, regenerate desktop migrations: `python3 -I scripts/desktop/gen_desktop_migrations.py`. Worktrees `../argosy-desktop-merge` and `../argosy-upstream-2.31` can be removed once 5c starts.
- **5c next. Catalog and Eden:**
  - Catalog (data, served from GitHub): Vita3K, ScummVM, PICO-8, xemu; Flycast VMU, Azahar/Lime3DS and PPSSPP save paths; m3u multi-disc launch per emulator.
  - Eden: Switch keys and firmware installed into Eden's folders.
- **5d. RetroArch core installs:** fetch the core a launch needs from the libretro buildbot into the RetroArch Flatpak's `cores/` folder. Check the Flatpak folder grant first.
- **5e. Screenshot upload:** watch each emulator's screenshot folder (new catalog field) during the supervised session, then hand off to upstream's shared upload queue.
- **5f. Live hardware readings** (`/proc`, `/sys/class/hwmon`, `power_supply`, `gpu_busy_percent`) and the **Connections page as status only**.
- **5g. Handheld seat picker:** SDL3 built-in pad detection (Deck 28de:1205, Legion Go, ROG Ally, MSI Claw).
- **5h. Games kept running and Resume, everywhere including Game Mode; the Home combo** (SDL3 combo raises the launcher and offers quit or keep).
- **Not in step 5:** Brightness stays out. Connections control, Wayland overlays and v3.0 are not part of this step.
- **Step close:** build, then publish to the IceSuite test repo (owner rule 2026-11-21; recipe in `gotchas.md`).

## Step 6 (IceBreaker rename and Flathub)

**Started 2026-11-21 (owner: "Begin branding conversion"), ahead of 5c–5h.** Unit 1 (identity) and 2a (desktop strings: `desktopBrand` filter in `shared/build.gradle.kts`) done. Units 2b and 3 done (DEVLOG 2026-11-26): splash, About credit and lockup; `BrandRelocation` moves the old Flatpak data and merges `~/Games/Argosy` into `~/Games/IceBreaker` with stored paths rewritten; the old-repo end-of-life build (`eol-old-repo.sh`) is tested but **not pushed: push it right after the first icesuite-test publish in unit 6** (recipe in `gotchas.md`). The README move notice must stay at the top when the README is rebuilt at the phase close. Unit 4 done (Add to Steam; DEVLOG 2026-11-26): **real Steam Add/Remove, Flatpak Steam, Game Mode and the wizard offer on screen are not checked yet** (Steam was running); do them before the Flathub PR. Next: unit 5 links and repo rename, unit 6 screenshots, lint and the first icesuite-test publish (PR #4 adds the catalog entry; the owner merges it).

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
- Walks: `scripts/desktop/check/pad-walk.sh flatpak <out> ...`; run log `build/desktop-logs/*desktop-run.log`. Shots go through the key pipe now. On Home, A is Play (it launches the game); use X for Details.
- VM lab (if reopened for IceBreaker): new guests and base volumes go in libvirt pool `VMDisks` (`LAB_POOL=VMDisks`), per the master VM lab standard (2026-11-21).

## Where things are

- Desktop code `shared/src/desktopMain`, app `desktopApp/`, Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/icebreaker` (never delete without the owner; `alauncher.db.before-relocation` is the pre-move backup); Flatpak data `~/.var/app/io.github.stealthx3.IceBreaker`; library `~/Games/IceBreaker`.
- Upstream refs: `origin/upstream` (v2.31.0), `rommapp/main` (fetched from rommapp; v3 betas).
