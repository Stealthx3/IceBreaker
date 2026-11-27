# AI disclosure

IceBreaker is a Linux desktop port of [Argosy](https://github.com/rommapp/argosy-launcher), the
Android game launcher for RomM. Most of the port, meaning the code, packaging and documentation that
make Argosy run on the Linux desktop, was written with an AI coding assistant, Anthropic's Claude
(through Claude Code). This page says what Claude produced, roughly how much, what it did not
produce, and how the work was directed and checked.

Figures are as of 2026-11-27. The port began on 2026-10-16.

> **Owner: before linking this page, fill in or confirm every line marked "Owner to confirm", then
> remove this note.** This page was drafted by Claude from the repository's history and logs.

## How the port was made

- **The owner directed the work.** The plan was split into phases and steps (`PORT_PLAN.md`), with
  every decision and its reasoning recorded in `DEVLOG.md`. The owner set the scope, made the product
  and design decisions, approved each phase, and decided what shipped.
- **Claude wrote the code, scripts, packaging and documentation**, ran the builds and tests, drove
  the app on test machines to check behaviour, and recorded the results.
- **Checks:** automated tests (unit tests for the desktop code, upstream's Android tests, lint and
  CI), runs on a local RomM test server, and testing on real hardware by the owner and beta testers.
- **Owner to confirm:** how much of the code you read and reviewed yourself, for example "spot
  checks", "every change to save sync", or "all of it".

## What Claude produced

### Port history

379 of the port's 381 commits (not counting merges of upstream releases) were made in Claude Code
sessions and carry a `Co-Authored-By: Claude` trailer. The other 2 are README edits made outside Claude
Code, in GitHub's web editor.

### Application code

| Part | Rough size | What it is |
|---|---|---|
| Desktop code: `shared/src/desktopMain`, `desktopApp/src` | about 13,600 lines of Kotlin (all of it) | The Linux side of the app, written new for the port: emulator detection and launch, Flatpak sandbox handling, desktop file access, save-folder layouts, windows and input, Steam Game Mode support, Add to Steam, data migration for the rename. |
| Changes to Argosy's shared code: `shared/src/commonMain`, `shared/src/androidMain` | about 31,000 added or changed lines, in a codebase of about 410,000 | Upstream's code moved into a Kotlin Multiplatform module and adapted to build for desktop too: platform seams, desktop fallbacks, and some fixes. Most of that codebase is unchanged upstream code. |
| Desktop tests: `shared/src/desktopTest` | about 2,800 lines | Unit tests for the desktop code. |

Put another way: of about 424,000 lines of Kotlin in the shipped app, roughly 45,000 (about 10%) were
written or changed by Claude. The rest is Argosy's own code (see "Upstream" below).

### Text the user sees

- **English:** 260 interface strings added for the desktop, the desktop file, the AppStream metainfo
  (description and release notes), and the setup and settings text specific to the desktop.
- **Translations:** 157 of those strings were translated by Claude into Chinese (Simplified and
  Traditional), German, Spanish, French, Hindi and Russian. They are machine translations and have not
  been checked by native speakers. Brazilian Portuguese doesn't have them, so they show in English
  there.
- **Screenshots:** the four store screenshots were captured by Claude from the running app. They show
  the real app and were not generated as images.

### Packaging and tooling (in this repository)

- **This repository's Flatpak manifests**, used for test builds on the IceSuite test repository.
  These are not the Flathub manifest (see below).
- The launcher scripts, the offline Gradle setup (`desktopApp/flatpak/offline.init.gradle.kts`),
  and the generator for the Gradle dependency list together with its output
  (`desktopApp/flatpak/generated/gradle-sources.json`).
- CI workflow changes, build, test and release scripts (`scripts/desktop/`), and prototypes
  (`spikes/`, not part of the app).

### Documentation

`README.md` (except the 2 web-editor edits), `PORT_PLAN.md`, `DEVLOG.md`, the `docs/desktop/` folder
(including the Flathub submission guide), and this page.

## Where Claude's involvement ends

- **The Flathub submission:** the owner wrote the Flathub manifest by hand and opened the pull request
  themself, with its description and all replies to reviewers. Claude didn't write or open any of it.
  The owner wrote the manifest with a Claude-written guide (`docs/desktop/flathub-submission-guide.md`)
  as reference; no text was copied from it or from this repository's manifests.
  **Owner to confirm:** that this matches what you did. If you generated the Gradle dependency list with
  a community tool instead of the repository's file, say which one.
- **Upstream Argosy:** most of the app's code is Argosy's, written by its developers, and is not
  covered by the figures above. Upstream also uses AI tools: its repository has agent instructions
  (`AGENTS.md`, `.claude/`), and 98 of its 3,665 commits carry AI co-author trailers. We don't know how
  much of Argosy is AI-written. Argosy's maintainers are the source for that.
- **Third-party components** built into the Flatpak (libass, libplacebo, mpv, sigil, the Java
  libraries) are their projects' own work.
- **Artwork** (app icon, splash, About logo, Steam artwork) comes from the owner's IceBreaker brand
  kit. **Owner to confirm:** who made it, and whether any of it was made with AI image tools.
- **Steam helper** (`packaging/steam/add_to_steam.py`) was copied unchanged from the owner's shared
  Steam shortcut standard, which is also used by the owner's other apps. Claude wrote IceBreaker's
  integration of it. **Owner to confirm:** who wrote the helper itself.
- **Bug reports and one fix:** beta testers reported issues. One supplied a patch for play-session
  sync (issue #4), which Claude applied and extended.

## Questions

Open an issue at https://github.com/Stealthx3/IceBreaker/issues.
