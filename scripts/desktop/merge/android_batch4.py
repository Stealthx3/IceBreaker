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
            t, n = re.subn(a, b, t, count=1, flags=re.M | re.S)
            assert n == 1, (str(path), a[:80])
        else:
            assert a in t, (str(path), a[:80])
            t = t.replace(a, b, 1)
    p.write_text(t, encoding="utf-8")


def strip_defaults_in(text, start):
    s = text.index("(", start)
    depth, j = 0, s
    while True:
        c = text[j]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                break
        j += 1
    params, out, cur, d = text[s + 1:j], [], "", 0
    for c in params:
        if c in "(<[{":
            d += 1
        elif c in ")>]}":
            d -= 1
        if c == "," and d == 0:
            out.append(cur)
            cur = ""
        else:
            cur += c
    out.append(cur)
    cleaned = []
    for p in out:
        d, cut = 0, None
        for k, c in enumerate(p):
            if c in "(<[{":
                d += 1
            elif c in ")>]}":
                d -= 1
            elif c == "=" and d == 0 and p[k + 1:k + 2] != ">":
                cut = k
                break
        cleaned.append(p[:cut].rstrip() if cut is not None else p)
    return text[:s + 1] + ",".join(cleaned) + text[j:]


def strip_actual_defaults(path):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    pos = 0
    while True:
        m = re.compile(r"actual (?:\w+ )*(?:fun [^(]+|constructor)\(").search(t, pos)
        if not m:
            break
        t = strip_defaults_in(t, m.start())
        pos = m.end()
    p.write_text(t, encoding="utf-8")


edit("shared/build.gradle.kts", [("        androidMain.dependencies {\n", "        androidMain.dependencies {\n            implementation(libs.shizuku.api)\n            implementation(libs.shizuku.provider)\n")])
edit(A / "data/emulator/background/BackgroundGames.kt", [("import com.nendo.argosy.platform.PlatformProcess as Process\n", "import android.os.Process\n")])

edit(A / "data/steam/SteamPathResolver.kt", [(r"^class SteamPathResolver @Inject constructor\(", "actual class SteamPathResolver @Inject constructor(")], regex=True)
edit(A / "data/steam/SteamService.kt", [(r"^    companion object \{", "    actual companion object {")], regex=True)
edit(A / "util/PermissionHelper.kt", [(r"^    companion object \{", "    actual companion object {")], regex=True)
edit(A / "hardware/CompanionPanel.kt", [(r"^class CompanionSessionTimer \{", "actual class CompanionSessionTimer {")], regex=True)
edit(A / "hardware/CompanionPanel.kt", [(r"internal fun CompanionInGameState\.withLiveQuickActionState\(\n\s*quickActionsAvailable: Boolean\n\): CompanionInGameState = copy\(quickActionsAvailable = quickActionsAvailable\)\n\n?", "")], regex=True)
edit(A / "domain/usecase/achievement/VerifyRAGameIdUseCase.kt", [
    (r"^class VerifyRAGameIdUseCase @Inject constructor\(", "actual class VerifyRAGameIdUseCase @Inject constructor("),
    (r"^    suspend operator fun invoke\(", "    actual suspend operator fun invoke("),
], regex=True)
edit(C / "domain/usecase/achievement/VerifyRAGameIdUseCase.expect.kt", [("operator suspend fun invoke(gameId: Long, forceRehash: Boolean): Long?", "operator suspend fun invoke(gameId: Long, forceRehash: Boolean = false): Long?")])

# LibretroCoreManager: progress reports bytes too
edit(C / "libretro/LibretroCoreManager.expect.kt", [(r"(suspend fun downloadCoreForPlatform\(platformSlug: String, selectedCoreId: String\?, onProgress: )\(\(Float\) -> Unit\)\?", r"\1((fraction: Float, zipBytes: Long) -> Unit)?")], regex=True)
edit(D / "libretro/LibretroCoreManager.desktop.kt", [(r"(actual suspend fun downloadCoreForPlatform\(platformSlug: String, selectedCoreId: String\?, onProgress: )\(\(Float\) -> Unit\)\?", r"\1((fraction: Float, zipBytes: Long) -> Unit)?")], regex=True)

# SaveStateManager gained the disk judge, secure saves and the save owner
NEW_PARAMS = ("savesDir: File, statesDir: File, romPath: String, gameId: Long, activeSaveRepository: ActiveSaveRepository, "
              "saveCacheManager: SaveCacheManager, diskJudge: com.nendo.argosy.data.repository.ActiveSaveDiskJudge, secureSaves: Boolean, ownerUserId: Long?, ")
edit(C / "libretro/SaveStateManager.expect.kt", [("expect class SaveStateManager(savesDir: File, statesDir: File, romPath: String, gameId: Long, activeSaveRepository: ActiveSaveRepository, saveCacheManager: SaveCacheManager, ",
                                                   "expect class SaveStateManager(" + NEW_PARAMS)])
edit(D / "libretro/SaveStateManager.desktop.kt", [("actual class SaveStateManager actual constructor(savesDir: File, statesDir: File, romPath: String, gameId: Long, activeSaveRepository: ActiveSaveRepository, saveCacheManager: SaveCacheManager, ",
                                                    "actual class SaveStateManager actual constructor(" + NEW_PARAMS)])

for f in ["data/social/SocialRepository.kt", "libretro/LibretroCoreManager.kt", "ui/components/Box3dCover.kt", "util/DisplayAffinityHelper.kt",
          "util/PermissionHelper.kt", "libretro/SaveStateManager.kt", "domain/usecase/achievement/VerifyRAGameIdUseCase.kt"]:
    strip_actual_defaults(A / f)
print("android batch4 done")
