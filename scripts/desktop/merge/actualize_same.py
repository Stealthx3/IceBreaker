import re
import sys
from pathlib import Path

ANDROID = Path("shared/src/androidMain/kotlin")
SAME = {
    "DualScreenManager.kt": ["focusableDisplays", "installedAppLabels", "registerCompanionHost"],
    "data/cache/ImageCacheManager.kt": ["pruneStaleUserScreenshots"],
    "data/download/MediaDownloadManager.kt": ["clearCompletedDownloads", "removeFromCompleted"],
    "data/emulator/PlaySessionTracker.kt": ["checkOrphanedSession"],
    "data/remote/jellyfin/JellyfinDeviceProfileBuilder.kt": ["DEFAULT_MAX_AUDIO_CHANNELS"],
    "data/steam/SteamService.kt": ["EXTRA_CONNECT_FOR_AUTH", "EXTRA_FORCE_CONNECT", "getCallbackManager", "getLibraryManager", "getSteamApps", "getSteamClient", "getSteamUser"],
    "data/storage/AndroidDataAccessor.kt": ["transformPath"],
    "data/storage/AndroidDataMirror.kt": ["isActive"],
    "hardware/AmbientLedManager.kt": ["setUiColors"],
    "libretro/SaveStateManager.kt": ["deleteSlot", "performSlotSave"],
    "ui/screens/settings/delegates/SteamSettingsDelegate.kt": ["installSteamLauncher", "loadSteamSettings", "startQrAuth"],
    "util/DisplayAffinityHelper.kt": ["getDisplayTargetId", "getEmulatorDisplayId", "hasSecondaryDisplay", "largestDisplayId"],
    "util/PermissionHelper.kt": ["getSessionDurations", "lastForegroundTimestamp"],
    "util/RootShell.kt": ["execute", "isAvailable", "run"],
}


def strip_defaults(params):
    out, depth, cur = [], 0, ""
    for c in params:
        if c in "(<[{":
            depth += 1
        elif c in ")>]}":
            depth -= 1
        if c == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += c
    out.append(cur)
    cleaned = []
    for p in out:
        d, i = 0, None
        for k, c in enumerate(p):
            if c in "(<[{":
                d += 1
            elif c in ")>]}":
                d -= 1
            elif c == "=" and d == 0:
                i = k
                break
        cleaned.append(p[:i].rstrip() if i is not None else p)
    return ",".join(cleaned)


for rel, names in SAME.items():
    p = ANDROID / "com/nendo/argosy" / rel
    t = p.read_text(encoding="utf-8")
    for n in names:
        m = re.search(r"^(\s+)((?:(?:override|internal|suspend|const|open|inline)\s+)*(?:fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>?]+\.)?" + re.escape(n) + r"\b)", t, re.M)
        assert m, (rel, n)
        if "actual " in t[m.start():m.start(2)]:
            continue
        t = t[:m.start(2)] + "actual " + t[m.start(2):]
        if "fun " in m.group(2):
            s = t.index("(", m.start(2))
            depth, j = 0, s
            while True:
                c = t[j]
                if c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
                    if depth == 0:
                        break
                j += 1
            t = t[:s + 1] + strip_defaults(t[s + 1:j]) + t[j:]
    p.write_text(t, encoding="utf-8")
    print("actualized", rel, len(names))
