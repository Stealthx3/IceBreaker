# Submitting IceBreaker to Flathub: checklist and manual manifest guide

For the owner. Written by Claude (AI) on 2026-11-27, from Flathub's
[Submission](https://docs.flathub.org/docs/for-app-authors/submission) and
[Requirements](https://docs.flathub.org/docs/for-app-authors/requirements) pages as they read on that
date. Flathub's pages are the authority; where this guide and Flathub disagree, follow Flathub.

## Read this first: the AI rules for the submission

Flathub's [Generative AI policy](https://docs.flathub.org/docs/for-app-authors/requirements#generative-ai-policy)
sets these rules:

- **The manifest must not contain AI-generated or AI-assisted content**, and disclosure does not
  exempt it. Write every line of the manifest yourself, and understand each one.
- **The PR is yours:** opening it, its commit messages, its description, and every review reply.
  AI tools must not write any of these.
- **Disclose AI-generated material** in the app and its packaging, naming the parts and roughly how
  much. The disclosure for this project is [`AI-DISCLOSURE.md`](../../AI-DISCLOSURE.md). Check it,
  fill in the parts marked for you, and link it from your PR.

This guide was written by an AI. It contains no manifest text: it tells you what the manifest has to
cover and where to look each value up. Flathub may still count a manifest written with an AI-written
guide as "AI-assisted". So:

- don't copy anything into the manifest from this guide or from the repo's own manifests (both were
  written with Claude);
- say in the disclosure that you used this guide;
- if you're unsure, ask Flathub before you start, in the
  [Matrix room](https://matrix.to/#/#flathub:matrix.org) or a
  [Flathub issue](https://github.com/flathub/flathub/issues).

## Checklist

✅ done and checked · ⚠️ needs a decision or a fix before you submit · ⬜ yours to do

### Inclusion policy

| Item | Status | Notes |
|---|---|---|
| Functional, not a minimal wrapper | ✅ | A full RomM client: library, downloads, save sync, play sessions, controller UI. |
| Host-dependent apps | ⚠️ | Launching emulators goes through `flatpak-spawn --host` (`HostShell.kt`). This is the biggest review risk: see "Review risks". |
| Duplicate or conflicting submissions | ✅ | Argosy isn't on Flathub, and upstream ships no Flatpak. The icesuite-test build is your own. |
| Emulation or translation layers (Wine and similar) | ✅ | Doesn't apply: IceBreaker is a native JVM app. It starts the user's emulators but isn't one itself. |
| Latest runtime | ✅ | Moved to `26.08` (2026-11-27), and the repo's manifests and CI image follow. Tested: the Flathub-style offline build of 1.0.0 builds on 26.08 (17 min), starts into setup, and libmpv picks up the runtime's full codecs (`codecs-extra`, FFmpeg 8). Video playback itself wasn't exercised. |
| Development history | ⚠️ | The port has existed since 2026-10-16, with 381 commits and one tag. Reviewers decide whether that's enough; upstream's long history helps. |
| Generative AI disclosure | ⚠️ | [`AI-DISCLOSURE.md`](../../AI-DISCLOSURE.md) is drafted; you finish and confirm it. |

### App ID, license, metadata (in the app repo: done where marked)

| Item | Status | Notes |
|---|---|---|
| App ID rules | ✅ | `io.github.stealthx3.IceBreaker` has 4 components, matches the metainfo, and maps to `https://github.com/stealthx3/IceBreaker`, which is reachable (GitHub ignores case in user names). Flathub verifies `io.github.` IDs through your GitHub sign-in. |
| License declared | ✅ | `GPL-3.0-only` in the metainfo; `LICENSE` is the GPL v3 text. |
| License files installed for every module | ⚠️ | The 26.08 test build installs libass's, libplacebo's and IceBreaker's. mpv, sigil (MPL-2.0) and the Steam helper install none. Your manifest must install each one under `/app/share/licenses/io.github.stealthx3.IceBreaker/<module>`. |
| Metainfo passes validation | ✅ | `flatpak-builder-lint appstream` and `appstreamcli validate --pedantic` both pass. The only pedantic note is the capital letters in the ID, which is normal for GitHub IDs. |
| Screenshots | ✅ | 4 screenshots with captions, pinned to the tag `icebreaker-v1.0.0`. The links work only once that tag is pushed. |
| Desktop file | ✅ | `desktop-file-validate` passes. |
| Icons | ✅ | Scalable SVG, PNGs up to 512×512, and a symbolic icon. |
| Name and icon are distinct; no implied affiliation | ✅ | The description credits Argosy and the RomM team and says IceBreaker isn't affiliated with or endorsed by rommapp. |
| Metadata lives in the app's own repo | ✅ | `desktopApp/flatpak/` (desktop file, metainfo, icons). |
| Stable release | ✅ | 1.0.0 (owner, 2026-11-27) is a stable release in the metainfo, and the beta line is gone. Earlier releases stay marked `development`, as they were. |
| Localisation | ⚠️ | Flathub bars machine-generated and mixed translations. 157 of the port's strings were translated by Claude into each of 6 languages, and Brazilian Portuguese shows those 157 in English. **Owner decision 2026-11-27: keep them and disclose them** (`AI-DISCLOSURE.md` names them). Risk: this rule is worded as a ban, not left to reviewers' discretion. If a reviewer objects, the fallback is an English-only desktop build, which Claude can make. |

### Build and packaging

| Item | Status | Notes |
|---|---|---|
| No network during the build | ✅ | The offline Gradle build works (CI x86_64 green on `577718149`). |
| aarch64 | ⚠️ | Fixed in `577718149` (arm64 Compose/Skiko files). The CI run was still going when this was written. If aarch64 can't be made to work, add `flathub.json` limiting the build to x86_64. |
| Built from source | ✅ | The app, libass, libplacebo, mpv and sigil build from source. Java libraries come from Maven as jars, the usual practice for JVM apps. The JRE comes from the `openjdk21` SDK extension. |
| Dependency sources file | ⚠️ | `desktopApp/flatpak/generated/gradle-sources.json` is produced by `scripts/desktop/flatpak/gradle-sources.py`, which Claude wrote. Its output goes into the submission, so consider regenerating it with a community tool (see step 4). |
| Patches | ✅ | None. |
| Manifest, sources file, PR | ⬜ | Yours (steps below). |

## Decide these before you write anything

1. **Release version and status: done.** You chose 1.0.0. The version, the stable metainfo release and
   the pinned screenshots are in the repo. Still to do: tag `icebreaker-v1.0.0` once the remaining
   app-side decisions below are settled and aarch64 builds, and point your manifest at that tag.
   `icebreaker-v0.5.0` doesn't have the aarch64 fix.
2. **Translations: decided.** You chose to keep them and disclose them. Mention them in your PR; if a
   reviewer asks, the options are native-speaker review or an English-only desktop build.
3. **Runtime 26.08: done.** Tested and adopted; see the checklist.
4. **Permissions.** Each one needs a reason in your PR. See "Review risks" for the ones to rethink.

## Step by step

### 1. Tools

Install `org.flatpak.Builder` from Flathub and add the Flathub remote user-wide, as described in
Flathub's Submission page, "Build and install".

### 2. Fork and branch

Fork `flathub/flathub` with "Copy the master branch only" **unchecked**. Clone the `new-pr` branch and
make a branch from it for the submission. The PR goes against `new-pr`, never against `master`.

### 3. Write the manifest yourself

Name it `io.github.stealthx3.IceBreaker.yml` (or `.json`) and put it at the top level of the
submission. Follow Flathub's style guide on the Requirements page: two-space YAML indentation, keys
in the order the guide lists, and blank lines between modules.

What the manifest has to describe, in the order of Flathub's key list:

- **ID, runtime and SDK:** the freedesktop Platform and Sdk at the latest version, plus the
  `org.freedesktop.Sdk.Extension.openjdk21` SDK extension. The app runs on a JRE that the extension
  installs into the app.
- **Command:** the launcher script `desktopApp/flatpak/icebreaker-jre.sh`, installed as the app's
  command.
- **Permissions (finish-args):** decide each one yourself.
  - `IceBreaker → Settings → Flatpak permissions` lists what the app checks for at runtime.
  - `docs/desktop/flathub-plan.md` explains each permission.
  - The save-folder list is in `HostSavePaths` / `RetroArchHostLayout` in
    `shared/src/desktopMain/`.
- **Codecs: nothing to add.** Video wallpapers and trailers play through libmpv. Since 25.08 the
  freedesktop Platform brings the full codecs itself, through its own `codecs-extra` extension
  (branch `<version>-extra`). The old `ffmpeg-full` extension has no 25.08 or 26.08 branch, so don't
  add it. The repo's manifests carried a dead `ffmpeg-full` entry until 2026-11-27.
- **Modules, in build order:**
  1. **libass**: subtitle rendering, needed by mpv. Source: the libass release tarball (look up the
     latest release and its checksum).
  2. **libplacebo**: mpv's renderer, built with OpenGL only. Source: the videolan git repository at a
     release tag and commit.
  3. **mpv**: the library only (`libmpv`), with the player, Lua, JavaScript and disc support turned
     off. Source: mpv's git repository at a release tag and commit. Install its license files:
     they're named `LICENSE.GPL` and `LICENSE.LGPL`, which flatpak-builder doesn't pick up by itself.
  4. **sigil JNI library**: sigil is the `sigil/` submodule (MPL-2.0). Build its JNI host library
     with its own CMake (option `SIGIL_BUILD_JNI_HOST`, target `sigil_jni_host`) against the
     openjdk21 headers, and install `libsigil-jni.so` next to the app's jars. Source: the sigil git
     repository at the commit the IceBreaker release pins (`git rev-parse <tag>:sigil`).
  5. **Steam helper**: `packaging/steam/add_to_steam.py`, installed as
     `io.github.stealthx3.IceBreaker-steam`, plus `steam-shortcut.json` and the five PNGs, installed
     under `share/io.github.stealthx3.IceBreaker/steam`. Source: the IceBreaker repo at your release
     tag (subfolder `packaging/steam`). Install the repo's `LICENSE` for this module.
  6. **IceBreaker**:
     - **Source:** the IceBreaker repo at your release tag and commit, with submodules (the build
       needs `sigil`), plus the Gradle 8.9 distribution and the dependency sources file (step 4).
     - **Build:** Gradle runs offline with `--offline`, using the init script
       `desktopApp/flatpak/offline.init.gradle.kts`. Run
       `-Pargosy.desktopOnly=true :desktopApp:createDistributable`. Set `ANDROID_HOME` to an empty
       folder and point `ARGOSY_OFFLINE_REPOSITORY` at the folder the sources file lays out.
     - **Install:**
       - copy the distributable from `desktopApp/build/compose/binaries/main/app/icebreaker/`, leaving
         out its own `bin/` and bundled `lib/runtime`;
       - run the openjdk21 extension's `install.sh`;
       - install the launcher script;
       - install the desktop file, metainfo and icons from `desktopApp/flatpak/`;
       - install `LICENSE`.

### 4. The dependency sources file

The build needs every Maven file listed as a source. You can:

- **Tried 2026-11-27, not complete:** jwharm's plugin, applied through an opt-in init script, covered
  only about 1,080 of the 1,452 files a real offline build needs. It missed:
  - metadata of Android dependencies, which a desktop-only build reads because it still configures
    the Android projects;
  - tools that plugins (KSP, Room, Compose) fetch through private configurations.

  The meshtastic plugin needs Gradle 9, and the build is on 8.9. A complete list from a community
  tool would first need a desktop-only build that never configures Android.
- **Write it with a community tool:**
  [jwharm/flatpak-gradle-generator](https://github.com/jwharm/flatpak-gradle-generator) or
  [meshtastic's gradle-flatpak-sources](https://plugins.gradle.org/plugin/org.meshtastic.flatpak.sources).
  Both are Gradle plugins, so the app's build would need the plugin added, an app-side change.
  Generate it for both x86_64 and aarch64: Compose Desktop and Skiko have per-architecture files
  (`-linux-x64` / `-linux-arm64`).
- **Use the repo's file**, `desktopApp/flatpak/generated/gradle-sources.json` (1,452 sources). It was
  produced by a Claude-written script. If you use it, say so in the disclosure, and expect reviewers
  to weigh it.

Flathub allows a separate sources file because it's generated by a tool.

### 5. Build, run and lint locally

Use the commands from Flathub's Submission page: `flathub-build --install` with your manifest, then
`flatpak run io.github.stealthx3.IceBreaker`, then `flatpak-builder-lint` in `manifest` mode and in
`repo` mode. Check:

- the app starts, signs in to RomM, downloads a game and launches it;
- `/app/share/licenses/io.github.stealthx3.IceBreaker/` has a folder for every module.

Lint findings to expect:

- `finish-args-flatpak-spawn-access` (error): needs an exception from Flathub. Explain it in the PR.
- `appstream-screenshots-not-mirrored-in-ostree` (repo mode): goes away on Flathub's own build.

### 6. The pull request

- Commit only your manifest and the sources file (no source code, no build output), and push.
- Open the PR on github.com against `new-pr`, titled `Add io.github.stealthx3.IceBreaker`.
- Keep Flathub's PR template: PRs that remove it are closed without review.
- In your own words, say why each permission is needed, and link `AI-DISCLOSURE.md`.
- Answer reviewers yourself. Once comments are resolved, you can comment `bot, build` to start a
  test build.

## Review risks

- **Host access (`--talk-name=org.freedesktop.Flatpak`).** Without it IceBreaker can't start
  emulators. Our notes say Lutris and Heroic Games Launcher ship with the same permission. Check that
  on their Flathub manifests before you cite them.
- **`--device=all` with `--device=dri`.** `all` already covers `dri`. Controllers need `all`; drop
  `dri` or explain both.
- **Broad file access:** `/mnt`, `/media` and `/run/media` (ROM libraries on other drives), the
  Steam folders (Add to Steam), and one path per emulator for save sync. Each needs a reason. File
  access through the portal can't cover save sync, which writes in place.
- **Beta status** and the **young port history** (see the checklist).
- **AI share:** most of the desktop port was written with Claude, and Flathub rejects at its
  discretion. The disclosure tells reviewers exactly what that covers.

## After the merge

You'll be invited to the new `flathub/io.github.stealthx3.IceBreaker` repo (2FA required; accept
within a week). Updates go there, not through a new submission. Users on icesuite-test move by
installing from Flathub: an end-of-life rebase only works within one remote.
