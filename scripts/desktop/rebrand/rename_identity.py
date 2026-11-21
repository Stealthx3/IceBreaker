#!/usr/bin/env python3
"""
IceBreaker rename, unit 1: app identity (owner request 2026-11-21; checklist
IceSuite/IceBreaker/rebrand/RENAME-CHECKLIST.md sections 1 and 4).

Old id io.github.stealthx3.ArgosyDesktop -> io.github.stealthx3.IceBreaker; binary argosy-desktop ->
icebreaker. Stored tokens keep their old value: the device-id digest, PlatformBuild.PRODUCT and the
default library folder ~/Games/Argosy.

usage: python3 -I scripts/desktop/rebrand/rename_identity.py <brand-deployment-dir>
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

OLD_ID, NEW_ID = "io.github.stealthx3.ArgosyDesktop", "io.github.stealthx3.IceBreaker"
OLD_BIN, NEW_BIN = "argosy-desktop", "icebreaker"
BRAND = Path(sys.argv[1])
FP = Path("desktopApp/flatpak")


def git_mv(a, b):
    subprocess.run(["git", "mv", str(a), str(b)], check=True)


def edit(path, pairs, regex=False):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if regex:
            t, n = re.subn(a, b, t, flags=re.M | re.S)
            assert n >= 1, (str(path), a[:70])
        else:
            assert a in t, (str(path), a[:70])
            t = t.replace(a, b)
    p.write_text(t, encoding="utf-8")


# Icons from the brand kit, named after the app id
icons = FP / "icons/hicolor"
for src in sorted((BRAND / "linux/hicolor").rglob("APPID*")):
    rel = src.relative_to(BRAND / "linux/hicolor")
    dst = icons / rel.parent / src.name.replace("APPID", NEW_ID)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
subprocess.run(["git", "add", str(icons)], check=True)

for ext in ["desktop", "metainfo.xml", "yml", "flathub.yml"]:
    git_mv(FP / f"{OLD_ID}.{ext}", FP / f"{NEW_ID}.{ext}")
git_mv(FP / f"{OLD_BIN}.sh", FP / f"{NEW_BIN}.sh")
git_mv(FP / f"{OLD_BIN}-jre.sh", FP / f"{NEW_BIN}-jre.sh")

ICON_BLOCK_OLD = re.compile(
    r"      - install -Dm644 argosy_icon\.svg /app/share/icons/hicolor/scalable/apps/" + re.escape(OLD_ID) + r"\.svg\n"
    r"      - for size in 128 256; do\n(?:.*\n)*?        done\n")
for manifest in [FP / f"{NEW_ID}.yml", FP / f"{NEW_ID}.flathub.yml"]:
    t = manifest.read_text(encoding="utf-8")
    t, n = ICON_BLOCK_OLD.subn("      - cp -r icons/hicolor /app/share/icons/\n", t)
    assert n == 1, (manifest, "icon block")
    t = re.sub(r"      - type: file\n        path: \.\./\.\./argosy_icon\.svg\n",
               "      - type: dir\n        path: icons\n        dest: icons\n", t)
    assert "argosy_icon" not in t, manifest
    t = t.replace(OLD_ID, NEW_ID)
    t = t.replace(f"command: {OLD_BIN}", f"command: {NEW_BIN}")
    t = t.replace(f"  - name: {OLD_BIN}\n", f"  - name: {NEW_BIN}\n")
    t = t.replace(f"/app/lib/{OLD_BIN}", f"/app/lib/{NEW_BIN}")
    t = t.replace(f"{OLD_BIN}.sh /app/bin/{OLD_BIN}", f"{NEW_BIN}.sh /app/bin/{NEW_BIN}")
    t = t.replace(f"{OLD_BIN}-jre.sh /app/bin/{OLD_BIN}", f"{NEW_BIN}-jre.sh /app/bin/{NEW_BIN}")
    t = t.replace(f"path: {OLD_BIN}.sh", f"path: {NEW_BIN}.sh").replace(f"path: {OLD_BIN}-jre.sh", f"path: {NEW_BIN}-jre.sh")
    t = t.replace(f"binaries/main/app/{OLD_BIN}", f"binaries/main/app/{NEW_BIN}")
    t = t.replace("builds Argosy Desktop from source", "builds IceBreaker from source")
    manifest.write_text(t, encoding="utf-8")

for launcher in [FP / f"{NEW_BIN}.sh", FP / f"{NEW_BIN}-jre.sh"]:
    t = launcher.read_text(encoding="utf-8")
    t = t.replace(f"/app/lib/{OLD_BIN}/bin/{OLD_BIN}", f"/app/lib/{NEW_BIN}/bin/{NEW_BIN}")
    t = t.replace(f"/app/lib/{OLD_BIN}/lib/app", f"/app/lib/{NEW_BIN}/lib/app").replace(f"$app/{OLD_BIN}.cfg", f"$app/{NEW_BIN}.cfg")
    launcher.write_text(t, encoding="utf-8")

(FP / f"{NEW_ID}.desktop").write_text(f"""[Desktop Entry]
Type=Application
Name=IceBreaker
GenericName=Game Launcher
Comment=Browse and play your game library
Exec={NEW_BIN}
Icon={NEW_ID}
Terminal=false
Categories=Game;
Keywords=RomM;emulation;emulator;launcher;frontend;retro;ROM;RetroArch;Steam Deck;gamepad;controller;
StartupWMClass=com-nendo-argosy-desktop-MainKt
X-Flatpak-RenamedFrom={OLD_ID}.desktop;
""", encoding="utf-8")

meta = FP / f"{NEW_ID}.metainfo.xml"
t = meta.read_text(encoding="utf-8")
releases = t[t.index("  <releases>"):t.index("</releases>") + len("</releases>")]
meta.write_text(f"""<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>{NEW_ID}</id>
  <metadata_license>CC0-1.0</metadata_license>
  <project_license>GPL-3.0-only</project_license>
  <name>IceBreaker</name>
  <summary>Browse and play your game library</summary>
  <developer id="io.github.stealthx3">
    <name>Stealthx3</name>
  </developer>
  <description>
    <p>
      IceBreaker connects to your RomM server and turns your game collection into a launcher you can
      drive with a controller from the couch. Pick a game and IceBreaker downloads it, starts the
      right emulator and brings you back when you are done. Your saves go back to RomM, so you can
      pick up where you left off on another device.
    </p>
    <p>
      It adapts to where you play: a desktop with mouse and keyboard, a TV, or a handheld in Steam
      Game Mode. IceBreaker finds the emulators you already have and can install the ones you need
      from Flathub.
    </p>
    <p>
      Game launcher for RomM. A port of Argosy for desktop: based on Argosy by the RomM team.
      IceBreaker is an independent port and is not affiliated with or endorsed by rommapp.
    </p>
    <p>This is a beta build.</p>
  </description>
  <launchable type="desktop-id">{NEW_ID}.desktop</launchable>
  <provides>
    <id>{OLD_ID}</id>
  </provides>
  <url type="homepage">https://github.com/Stealthx3/IceBreaker</url>
  <url type="bugtracker">https://github.com/Stealthx3/IceBreaker/issues</url>
  <url type="vcs-browser">https://github.com/Stealthx3/IceBreaker</url>
  <branding>
    <color type="primary" scheme_preference="light">#80e5ff</color>
    <color type="primary" scheme_preference="dark">#06445e</color>
  </branding>
  <categories>
    <category>Game</category>
  </categories>
  <keywords>
    <keyword>RomM</keyword>
    <keyword>emulator</keyword>
    <keyword>launcher</keyword>
    <keyword>frontend</keyword>
    <keyword>retro</keyword>
    <keyword>gamepad</keyword>
  </keywords>
  <supports>
    <control>gamepad</control>
    <control>keyboard</control>
  </supports>
  <content_rating type="oars-1.1"/>
{releases}
</component>
""", encoding="utf-8")

edit("desktopApp/build.gradle.kts", [(f'packageName = "{OLD_BIN}"', f'packageName = "{NEW_BIN}"')])
edit("desktopApp/src/main/kotlin/com/nendo/argosy/desktop/Main.kt", [('            title = "Argosy",', '            title = "IceBreaker",')])

D = Path("shared/src/desktopMain/kotlin/com/nendo/argosy")
edit(D / "platform/PlatformContext.desktop.kt", [
    ('    val data: java.io.File get() = xdg("XDG_DATA_HOME", "/.local/share").resolve("argosy-desktop")',
     '    val data: java.io.File get() = renamed(xdg("XDG_DATA_HOME", "/.local/share"))'),
    ('    val cache: java.io.File get() = xdg("XDG_CACHE_HOME", "/.cache").resolve("argosy-desktop")',
     '    val cache: java.io.File get() = renamed(xdg("XDG_CACHE_HOME", "/.cache"))\n\n'
     '    private fun renamed(base: java.io.File): java.io.File {\n'
     '        val current = base.resolve(APP_FOLDER)\n'
     '        val previous = base.resolve(PREVIOUS_APP_FOLDER)\n'
     '        if (!current.exists() && previous.isDirectory) previous.renameTo(current)\n'
     '        return current\n'
     '    }\n\n'
     '    private const val APP_FOLDER = "icebreaker"\n'
     '    private const val PREVIOUS_APP_FOLDER = "argosy-desktop"'),
])
edit(D / "platform/FactoryResetWipe.kt", [('resolveSibling("argosy-desktop-factory-reset")', 'resolveSibling("icebreaker-factory-reset")')])
edit(D / "data/repository/AppsRepository.desktop.kt", [(f'private const val ARGOSY_DESKTOP_ID = "{OLD_ID}"', f'private const val ARGOSY_DESKTOP_ID = "{NEW_ID}"')])
edit(D / "platform/mpv/MpvProbe.kt", [(f"`{OLD_BIN} --probe-video=<file>`", f"`{NEW_BIN} --probe-video=<file>`")])

for script in ["scripts/desktop/flatpak/build-alpha.sh", "scripts/desktop/check/desktop-drive.sh", "scripts/desktop/check/fresh-walk.sh",
               "scripts/desktop/flatpak/flatpak-shot.sh", "scripts/desktop/testbed/desktop-device.sh", "scripts/desktop/testbed/pair-code.py",
               ".github/workflows/flatpak.yml"]:
    p = Path(script)
    t = p.read_text(encoding="utf-8").replace(OLD_ID, NEW_ID)
    p.write_text(t, encoding="utf-8")
print("identity renamed")
