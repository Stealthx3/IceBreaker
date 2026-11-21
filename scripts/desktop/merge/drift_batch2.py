import re
from pathlib import Path

C = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
A = Path("shared/src/androidMain/kotlin/com/nendo/argosy")
D = Path("shared/src/desktopMain/kotlin/com/nendo/argosy")


def edit(path, pairs, regex=False, count=1):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if regex:
            t, n = re.subn(a, b, t, count=count, flags=re.M)
            assert n >= 1, (str(path), a[:80])
        else:
            assert a in t, (str(path), a[:80])
            t = t.replace(a, b, 1)
    p.write_text(t, encoding="utf-8")


def drop_member(path, name):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    m = re.search(r"^(\s{4})(?:actual\s+)?(?:(?:override|internal|suspend|const)\s+)*(?:fun|val|var)\s+" + re.escape(name) + r"\b", t, re.M)
    assert m, (str(path), name)
    start = t.rfind("\n", 0, m.start()) + 1
    i = m.end()
    depth = 0
    paren = 0
    while i < len(t):
        c = t[i]
        if c == "(":
            paren += 1
        elif c == ")":
            paren -= 1
        elif c == "{" and paren == 0:
            depth += 1
        elif c == "}" and paren == 0:
            depth -= 1
            if depth == 0:
                i += 1
                break
        elif c == "\n" and depth == 0 and paren == 0:
            nxt = t[i + 1:i + 9]
            if not nxt.startswith("        ") and not nxt.strip().startswith((".", "?:", "=")):
                break
        i += 1
    end = i
    while end < len(t) and t[end] == "\n":
        end += 1
    t = t[:start] + t[end:]
    p.write_text(t, encoding="utf-8")


def actual(path, prefix):
    edit(path, [(r"^(\s{4})(?!actual )(" + re.escape(prefix) + ")", r"\1actual \2")], regex=True)


# DualScreenManager
for n in ["mediaSignedIn", "companionMediaVisible", "toggleCompanionMediaView", "setCompanionMediaVisible"]:
    drop_member(C / "DualScreenManager.expect.kt", n)
    drop_member(D / "DualScreenManager.desktop.kt", n)
edit(C / "DualScreenManager.expect.kt", [("suspend fun emulatorLeftScreen(context: Context, launcherStillInFront: () -> Boolean): Boolean",
                                          "suspend fun emulatorLeftScreen(context: Context, onGameDisplay: Boolean, launcherStillInFront: () -> Boolean): Boolean")])
edit(D / "DualScreenManager.desktop.kt", [("actual suspend fun emulatorLeftScreen(context: Context, launcherStillInFront: () -> Boolean): Boolean",
                                           "actual suspend fun emulatorLeftScreen(context: Context, onGameDisplay: Boolean, launcherStillInFront: () -> Boolean): Boolean")])
actual(A / "DualScreenManager.kt", "suspend fun emulatorLeftScreen(")

# ImageCacheManager.gameArt is a snapshot
edit(C / "data/cache/ImageCacheManager.expect.kt", [("val gameArt: StateFlow<Map<Long, ResolvedGameArt>>", "val gameArt: StateFlow<GameArtSnapshot>")])
actual(A / "data/cache/ImageCacheManager.kt", "val gameArt:")
edit(D / "data/cache/ImageCacheManager.desktop.kt", [
    ("actual val gameArt: StateFlow<Map<Long, ResolvedGameArt>> = gameArtDao.observeRowCount()", "actual val gameArt: StateFlow<GameArtSnapshot> = gameArtDao.observeRowCount()"),
    ("emit(gameArtDao.getAll().toResolvedArtByGame())", "emit(GameArtSnapshot(gameArtDao.getAll().toResolvedArtByGame()))"),
])
t = (D / "data/cache/ImageCacheManager.desktop.kt").read_text(encoding="utf-8")
t = re.sub(r"(actual val gameArt: StateFlow<GameArtSnapshot> = [^\n]*\n(?:[^\n]*\n){0,8}?[^\n]*stateIn\([^,]+, [^,]+, )emptyMap\(\)\)", r"\1GameArtSnapshot.EMPTY)", t, count=1)
(D / "data/cache/ImageCacheManager.desktop.kt").write_text(t, encoding="utf-8")

# PlaySessionTracker.externalSessionClosed carries the emulator package
edit(C / "data/emulator/PlaySessionTracker.expect.kt", [("val externalSessionClosed: SharedFlow<Unit>", "val externalSessionClosed: SharedFlow<String>")])
actual(A / "data/emulator/PlaySessionTracker.kt", "val externalSessionClosed")
edit(D / "data/emulator/PlaySessionTracker.desktop.kt", [
    ("private val _externalSessionClosed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)", "private val _externalSessionClosed = MutableSharedFlow<String>(extraBufferCapacity = 1)"),
    ("actual val externalSessionClosed: SharedFlow<Unit> = _externalSessionClosed.asSharedFlow()", "actual val externalSessionClosed: SharedFlow<String> = _externalSessionClosed.asSharedFlow()"),
])
edit(D / "data/emulator/PlaySessionTracker.desktop.kt", [(r"_externalSessionClosed\.tryEmit\(Unit\)", "_externalSessionClosed.tryEmit(session.emulatorPackage)")], regex=True, count=0)

# MusicDirectoryManager.relocate reports what moved and what stayed
edit(C / "data/music/MusicDirectoryManager.expect.kt", [("suspend fun relocate(source: File, destination: File)\n", "suspend fun relocate(source: File, destination: File): com.nendo.argosy.util.TreeRelocator.Result\n")])
actual(A / "data/music/MusicDirectoryManager.kt", "suspend fun relocate(")
edit(D / "data/music/MusicDirectoryManager.desktop.kt", [(
    "    actual suspend fun relocate(source: File, destination: File): Unit = withContext(Dispatchers.IO) {\n        if (!source.exists() || source.absolutePath == destination.absolutePath) return@withContext\n        moveTree(source, destination)\n    }",
    "    actual suspend fun relocate(source: File, destination: File): com.nendo.argosy.util.TreeRelocator.Result =\n        withContext(Dispatchers.IO) {\n            if (!source.exists() || source.absolutePath == destination.absolutePath) {\n                com.nendo.argosy.util.TreeRelocator.Result(emptyList(), emptyList())\n            } else {\n                com.nendo.argosy.util.TreeRelocator.relocate(source, destination)\n            }\n        }")])

# SocialRepository: requests report whether they were sent
SOCIAL = ["acceptFriend(", "addFriendByCode(", "followCommunity(", "hideEvent(", "likeEvent(", "markAllNotificationsRead(",
          "markNotificationRead(", "regenerateFriendCode(", "removeFriend(", "toggleGameVisibility(", "unfollowCommunity(",
          "updateCommunityFollow(", "updateEventVisibility(", "updateUserSettings("]
for name in SOCIAL:
    edit(C / "data/social/SocialRepository.expect.kt", [(r"^(    fun " + re.escape(name) + r"[^\n]*\))\n", r"\1: Boolean\n")], regex=True)
    d = (D / "data/social/SocialRepository.desktop.kt").read_text(encoding="utf-8")
    d, n = re.subn(r"^(    actual fun " + re.escape(name) + r"[^\n]*\))( = Unit|\s*\{\s*\}|: Unit = Unit)\n", r"\1: Boolean = false\n", d, count=1, flags=re.M)
    assert n == 1, ("desktop social", name)
    (D / "data/social/SocialRepository.desktop.kt").write_text(d, encoding="utf-8")
    actual(A / "data/social/SocialRepository.kt", "fun " + name)
edit(C / "data/social/SocialRepository.expect.kt", [(
    "fun sendPresence(status: PresenceStatus, gameIgdbId: Int? = null, gameTitle: String? = null, media: MediaPresence? = null): Boolean",
    "fun sendPresence(\n        status: PresenceStatus,\n        gameIgdbId: Int? = null,\n        gameTitle: String? = null,\n        media: MediaPresence? = null,\n        externalNetplay: com.nendo.argosy.data.netplay.external.ExternalNetplay? = null\n    ): Boolean")])
d = (D / "data/social/SocialRepository.desktop.kt").read_text(encoding="utf-8")
d, n = re.subn(r"actual fun sendPresence\(([^)]*?)media: MediaPresence\?\)", r"actual fun sendPresence(\1media: MediaPresence?, externalNetplay: com.nendo.argosy.data.netplay.external.ExternalNetplay?)", d, count=1, flags=re.S)
assert n == 1, "desktop sendPresence"
(D / "data/social/SocialRepository.desktop.kt").write_text(d, encoding="utf-8")
t = (A / "data/social/SocialRepository.kt").read_text(encoding="utf-8")
m = re.search(r"^    fun sendPresence\(", t, re.M)
t = t[:m.start()] + "    actual fun sendPresence(" + t[m.end():]
s = m.start()
e = t.index("): Boolean", s)
seg = re.sub(r"\s*=\s*null", "", t[s:e])
t = t[:s] + seg + t[e:]
(A / "data/social/SocialRepository.kt").write_text(t, encoding="utf-8")
for name in ["followCommunity(", "toggleGameVisibility(", "updateUserSettings("]:
    t = (A / "data/social/SocialRepository.kt").read_text(encoding="utf-8")
    m = re.search(r"actual fun " + re.escape(name), t)
    e = t.index("): Boolean", m.start())
    t = t[:m.start()] + re.sub(r"\s*=\s*(null|false|true)", "", t[m.start():e]) + t[e:]
    (A / "data/social/SocialRepository.kt").write_text(t, encoding="utf-8")

# SteamService and SteamLaunchers: members upstream removed
for n in ["EXTRA_AUTO_CONNECT", "getContentManager"]:
    drop_member(C / "data/steam/SteamService.expect.kt", n)
    drop_member(D / "data/steam/SteamService.desktop.kt", n)
drop_member(C / "data/launcher/SteamLaunchers.expect.kt", "launchChoices")
drop_member(D / "data/launcher/SteamLaunchers.desktop.kt", "launchChoices")

# SteamSettingsDelegate follows upstream's launcher flow
SSD_E = C / "ui/screens/settings/delegates/SteamSettingsDelegate.expect.kt"
SSD_D = D / "ui/screens/settings/delegates/SteamSettingsDelegate.desktop.kt"
SSD_A = A / "ui/screens/settings/delegates/SteamSettingsDelegate.kt"
drop_member(SSD_E, "cycleSteamInstallVolume")
drop_member(SSD_D, "cycleSteamInstallVolume")
edit(SSD_E, [(r"fun showAddSteamGameDialog\(\)", "fun showAddSteamGameDialog(launcherPackage: String?)"),
             (r"fun confirmAddSteamGame\(context: Context, scope: CoroutineScope, launcherPackage: String = [^\n]*\)", "fun confirmAddSteamGame(context: Context, scope: CoroutineScope)")], regex=True)
edit(SSD_D, [(r"actual fun showAddSteamGameDialog\(\)", "actual fun showAddSteamGameDialog(launcherPackage: String?)"),
             (r"actual fun confirmAddSteamGame\(context: Context, scope: CoroutineScope, launcherPackage: String\)", "actual fun confirmAddSteamGame(context: Context, scope: CoroutineScope)")], regex=True)
for pre in ["fun showAddSteamGameDialog(", "fun confirmAddSteamGame(", "suspend fun refreshLauncherAvailability(", "fun refreshMetadata(",
            "fun moveVariantFocus(", "fun confirmVariant(", "fun dismissVariantPicker(", "fun selectVariantAt("]:
    actual(SSD_A, pre)
for path, body in [(SSD_E, """
    suspend fun refreshLauncherAvailability(context: Context)

    fun refreshMetadata(scope: CoroutineScope)

    fun moveVariantFocus(delta: Int)

    fun confirmVariant()

    fun dismissVariantPicker()

    fun selectVariantAt(index: Int)"""), (SSD_D, """
    actual suspend fun refreshLauncherAvailability(context: Context) = Unit

    actual fun refreshMetadata(scope: CoroutineScope) = Unit

    actual fun moveVariantFocus(delta: Int) = Unit

    actual fun confirmVariant() = Unit

    actual fun dismissVariantPicker() = Unit

    actual fun selectVariantAt(index: Int) = Unit""")]:
    t = path.read_text(encoding="utf-8").rstrip()
    assert t.endswith("}")
    path.write_text(t[:-1].rstrip() + "\n" + body + "\n}\n", encoding="utf-8")

# PermissionHelper.isPackageOnScreen lost its look-back parameter
edit(C / "util/PermissionHelper.expect.kt", [(r"fun isPackageOnScreen\(context: Context, packageName: String, lookbackMs: Long = [^)]*\): Boolean\?", "fun isPackageOnScreen(context: Context, packageName: String): Boolean?")], regex=True)
edit(D / "util/PermissionHelper.desktop.kt", [(r"actual fun isPackageOnScreen\(context: Context, packageName: String, lookbackMs: Long\)", "actual fun isPackageOnScreen(context: Context, packageName: String)")], regex=True)
actual(A / "util/PermissionHelper.kt", "fun isPackageOnScreen(")

# ScreenCatalog companion on Android
t = (A / "util/ScreenCatalog.kt").read_text(encoding="utf-8")
m = re.search(r"actual companion object\s*\{\s*\}", t)
repl = "actual companion object {\n        actual fun builtInScreenCount(context: Context): Int =\n            ScreenCatalog(context.applicationContext).attachedScreens().count { it.builtIn }\n    }"
if m:
    t = t[:m.start()] + repl + t[m.end():]
else:
    m = re.search(r"actual companion object\s*\{", t)
    assert m, "android ScreenCatalog companion"
    t = t[:m.end()] + "\n        actual fun builtInScreenCount(context: Context): Int =\n            ScreenCatalog(context.applicationContext).attachedScreens().count { it.builtIn }\n" + t[m.end():]
(A / "util/ScreenCatalog.kt").write_text(t, encoding="utf-8")
print("batch2 done")
