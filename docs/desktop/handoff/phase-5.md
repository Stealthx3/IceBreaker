# Phase 5 · Beta and Flathub — closed summary

**Closed 2026-11-28** (owner: "Close now"). Details and reasoning: `DEVLOG.md`, Phase 5 section.

## Outcome

- **Steps 1–6 done.** Wizard wording, Close in Quick Settings, thumbnails from sync, Linux desktop and Steam Game Mode compatibility, the upstream v2.31.0 merge, and the IceBreaker rename with the Flathub preparation.
- **IceBreaker 1.0.0** is the first stable release: tag `icebreaker-v1.0.0`, runtime 26.08, CI green on x86_64 and aarch64. It's published on IceBreaker's own Pages Flatpak repo and as a GitHub Release (owner, 2026-11-28). icesuite-test still serves 0.5.0 until the owner reorganizes it into testing and stable branches.
- **The repo is `Stealthx3/IceBreaker`.** Beta users on the old Argosy Desktop repo get the end-of-life rebase from the user site (`Stealthx3/stealthx3.github.io`, folder `argosy-desktop/`).
- **The Flathub submission is the owner's** (Flathub's Generative AI policy): their own manifest, PR and replies. The help for it is in the repo: `docs/desktop/flathub-submission-guide.md`, `AI-DISCLOSURE.md` and the generator (`scripts/desktop/flatpak/regen-gradle-sources.sh`).
- **History rewritten at the close** (owner, 2026-11-28), using the PII Redaction Standard. Commit hashes before the close changed; the tags point at the rewritten commits.

## Not met from the exit criterion

- **Flathub PR merged:** the owner submits by hand. Carried into Phase 6, step 1.
- **Not checked on screen:** Flatpak Steam, Game Mode, Remove from Steam in real Steam, and the wizard's Steam offer.
- **Carried from step 4 (not verified):** `HostGameProcess.stopAll` ending a Flatpak emulator on exit; EmuDeck detection on the owner's host; Assign BIOS with real RomM firmware; the folder picker's Back row; Snap emulators render black under the tagger; Steam Game Recording stalling audio in nested gamescope.

## Carried forward to Phase 6

Units 5c–5h of step 5 (catalog and Eden, RetroArch core installs, screenshot upload, live hardware readings, the handheld seat picker, keep running and Resume), the upstream 3.1.1 merge, and the two open "Owner to confirm" lines in `AI-DISCLOSURE.md` (who ran the generator, and the submission). See `phase-6.md`.
