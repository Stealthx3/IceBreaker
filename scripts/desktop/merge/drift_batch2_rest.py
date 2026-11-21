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
    m = re.search(r"^(\s{4,8})(?:actual\s+)?(?:(?:override|internal|suspend|const)\s+)*(?:fun|val|var)\s+" + re.escape(name) + r"\b", t, re.M)
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
            if not nxt.startswith(m.group(1) + "    ") and not nxt.strip().startswith((".", "?:", "=")):
                break
        i += 1
    end = i
    while end < len(t) and t[end] == "\n":
        end += 1
    t = t[:start] + t[end:]
    p.write_text(t, encoding="utf-8")


def actual(path, prefix):
    edit(path, [(r"^(\s{4})(?!actual )(" + re.escape(prefix) + ")", r"\1actual \2")], regex=True)


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
