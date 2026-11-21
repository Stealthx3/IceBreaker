import re
from pathlib import Path

C = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
A = Path("shared/src/androidMain/kotlin/com/nendo/argosy")
D = Path("shared/src/desktopMain/kotlin/com/nendo/argosy")


def edit(path, pairs, regex=False):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if regex:
            t, n = re.subn(a, b, t, count=1, flags=re.M)
            assert n == 1, (path, a)
        else:
            assert a in t, (path, a[:70])
            t = t.replace(a, b, 1)
    p.write_text(t, encoding="utf-8")


def add_to_class_end(path, text):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    m = re.search(r"^(?:@\w+\s+)*(?:expect |actual )?(?:object|class) \w+", t, re.M)
    i = m.end()
    paren = 0
    while True:
        c = t[i]
        if c == "(":
            paren += 1
        elif c == ")":
            paren -= 1
        elif c == "{" and paren == 0:
            break
        i += 1
    depth = 0
    j = i
    while True:
        c = t[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                break
        j += 1
    body = t[:j].rstrip()
    p.write_text(body + "\n\n" + text.strip("\n") + "\n" + t[j:], encoding="utf-8")


def ensure_import(path, imp):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    if f"import {imp}\n" in t:
        return
    t = re.sub(r"^(package [^\n]+\n)", r"\1\nimport " + imp + "\n", t, count=1, flags=re.M)
    p.write_text(t, encoding="utf-8")


def actualize(path, prefixes):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for pre in prefixes:
        pat = re.compile(r"^(\s{4})(?!actual )(" + re.escape(pre) + r")", re.M)
        t, n = pat.subn(r"\1actual \2", t, count=1)
        assert n == 1, (path, pre)
    p.write_text(t, encoding="utf-8")


add_to_class_end(D / "data/emulator/PlaySessionTracker.desktop.kt", """
    actual val sessionEnding: kotlinx.coroutines.flow.StateFlow<Boolean> =
        kotlinx.coroutines.flow.MutableStateFlow(false)""")

# AppsRepository
add_to_class_end(C / "data/repository/AppsRepository.expect.kt", """
    fun isPackageUpdating(packageName: String): Boolean

    fun isPackageMissing(packageName: String): Boolean""")
actualize(A / "data/repository/AppsRepository.kt", ["fun isPackageUpdating(", "fun isPackageMissing("])
add_to_class_end(D / "data/repository/AppsRepository.desktop.kt", """
    actual fun isPackageUpdating(packageName: String): Boolean = false

    actual fun isPackageMissing(packageName: String): Boolean = false""")

# ApkInstallManager
add_to_class_end(C / "data/update/ApkInstallManager.expect.kt", """
    val pushedPrompt: kotlinx.coroutines.flow.StateFlow<com.nendo.argosy.data.model.PushedApkPrompt?>

    fun confirmPushedInstall()

    fun dismissPushedInstall()""")
actualize(A / "data/update/ApkInstallManager.kt", ["val pushedPrompt", "fun confirmPushedInstall(", "fun dismissPushedInstall("])
add_to_class_end(D / "data/update/ApkInstallManager.desktop.kt", """
    actual val pushedPrompt: kotlinx.coroutines.flow.StateFlow<com.nendo.argosy.data.model.PushedApkPrompt?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun confirmPushedInstall() = Unit

    actual fun dismissPushedInstall() = Unit""")

# ImageCacheManager.userScreenshotTargetFile gains the extension
for f, pre in [(C / "data/cache/ImageCacheManager.expect.kt", "    "), (D / "data/cache/ImageCacheManager.desktop.kt", "    actual ")]:
    t = f.read_text(encoding="utf-8")
    t, n = re.subn(r"suspend fun userScreenshotTargetFile\(rommId: Long, screenshotId: Long, version: String\)",
                   "suspend fun userScreenshotTargetFile(rommId: Long, screenshotId: Long, version: String, extension: String)", t)
    assert n == 1, f
    f.write_text(t, encoding="utf-8")
actualize(A / "data/cache/ImageCacheManager.kt", ["suspend fun userScreenshotTargetFile("])

# SteamLaunchers.launchCandidates
add_to_class_end(C / "data/launcher/SteamLaunchers.expect.kt", """
    fun launchCandidates(
        storedPackage: String?,
        pinned: Boolean,
        reporting: List<String>,
        isInstalled: (String) -> Boolean
    ): List<String>""")
actualize(A / "data/launcher/SteamLaunchers.kt", ["fun launchCandidates("])
add_to_class_end(D / "data/launcher/SteamLaunchers.desktop.kt", """
    actual fun launchCandidates(
        storedPackage: String?,
        pinned: Boolean,
        reporting: List<String>,
        isInstalled: (String) -> Boolean
    ): List<String> = emptyList()""")

# SocialRepository methods now report whether the request was sent
for name in ["requestUserProfile(", "sendFriendRequest(", "toggleFavoriteFriend(", "reportEvent(", "commentEvent("]:
    t = (C / "data/social/SocialRepository.expect.kt").read_text(encoding="utf-8")
    t, n = re.subn(r"(    fun " + re.escape(name) + r"[^\n]*\))\n", r"\1: Boolean\n", t, count=1)
    assert n == 1, name
    (C / "data/social/SocialRepository.expect.kt").write_text(t, encoding="utf-8")
    d = (D / "data/social/SocialRepository.desktop.kt").read_text(encoding="utf-8")
    d, n = re.subn(r"(    actual fun " + re.escape(name) + r"[^\n]*\))( = Unit|\s*\{\s*\})\n", r"\1: Boolean = false\n", d, count=1)
    assert n == 1, ("desktop", name)
    (D / "data/social/SocialRepository.desktop.kt").write_text(d, encoding="utf-8")
actualize(A / "data/social/SocialRepository.kt", ["fun requestUserProfile(", "fun sendFriendRequest(", "fun toggleFavoriteFriend(", "fun reportEvent(", "fun commentEvent("])

# InstallerFailure lives in shared code; upstream added WRONG_PACKAGE to its own copy
edit(C / "data/installer/InstallerJobs.kt", [("    NOT_FOUND\n}", "    NOT_FOUND,\n    WRONG_PACKAGE\n}")])
edit(A / "data/installer/ManagedInstallerManager.kt", [(
    "enum class InstallerFailure {\n    NO_RELEASE,\n    NO_APK,\n    DOWNLOAD,\n    RATE_LIMITED,\n    NOT_FOUND,\n    WRONG_PACKAGE\n}\n", "")])

# REFRESH_SYNC_KEY moves to shared code next to the expect declaration
edit(A / "ui/screens/settings/delegates/SteamSettingsDelegate.kt", [("internal const val REFRESH_SYNC_KEY = \"refresh\"\n", "")])
t = (C / "ui/screens/settings/delegates/SteamSettingsDelegate.expect.kt").read_text(encoding="utf-8")
t = re.sub(r"^(expect class SteamSettingsDelegate)", "internal const val REFRESH_SYNC_KEY = \"refresh\"\n\n\\1", t, count=1, flags=re.M)
(C / "ui/screens/settings/delegates/SteamSettingsDelegate.expect.kt").write_text(t, encoding="utf-8")

# RootShell.Result is constructed in shared code
edit(C / "util/RootShell.expect.kt", [("    class Result {\n", "    class Result(exitCode: Int?, output: List<String>) {\n")])

# ScreenCatalog: shared code counts built-in screens through the companion
edit(C / "util/ScreenCatalog.expect.kt", [("    companion object {}", "    companion object {\n        fun builtInScreenCount(context: com.nendo.argosy.platform.PlatformContext): Int\n    }")])
edit(D / "util/ScreenCatalog.desktop.kt", [("    actual companion object {}", "    actual companion object {\n        actual fun builtInScreenCount(context: com.nendo.argosy.platform.PlatformContext): Int = 1\n    }")])
edit(C / "ui/common/ControllerArt.kt", [(
    "ScreenCatalog(context.applicationContext).attachedScreens().count { it.builtIn }",
    "ScreenCatalog.builtInScreenCount(context)")])

# Platform input: sources upstream now reads
edit(C / "platform/PlatformInputDevice.kt", [("const val SOURCE_KEYBOARD: Int = 257\n",
    "const val SOURCE_KEYBOARD: Int = 257\n\nconst val SOURCE_MOUSE: Int = 8194\n\nconst val SOURCE_MOUSE_RELATIVE: Int = 131076\n\nconst val SOURCE_TOUCHPAD: Int = 1048584\n\nexpect fun PlatformInputDevice.supportsSource(source: Int): Boolean\n")])

# Library list style field
ensure_import(C / "ui/screens/library/LibraryUiState.kt", "com.nendo.argosy.data.preferences.LibraryListStyle")

# FileAccessLayer.prepareSaveAccess now reports success
edit(D / "data/storage/DesktopFileAccessLayer.kt", [("override fun prepareSaveAccess(vararg paths: String?) = Unit", "override fun prepareSaveAccess(vararg paths: String?): Boolean = true")])
print("batch1 done")
