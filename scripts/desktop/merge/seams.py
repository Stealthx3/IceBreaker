import re
import subprocess
from pathlib import Path

ROOT = "com/nendo/argosy"
C = Path("shared/src/commonMain/kotlin") / ROOT
A = Path("shared/src/androidMain/kotlin") / ROOT
D = Path("shared/src/desktopMain/kotlin") / ROOT


def git_mv(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "mv", str(src), str(dst)], check=True)


def take_decls(text, names):
    out = []
    for name in names:
        m = re.search(
            r"(\n(?:/\*\*(?:.|\n)*?\*/\n)?(?:data class|enum class|class) " + re.escape(name) + r"\b[^\n]*(?:\n(?!\S)[^\n]*|\n\))*\n?)",
            text,
        )
        if not m:
            raise SystemExit(f"declaration {name} not found")
        block = m.group(1)
        if block.rstrip().endswith("(") or block.count("(") > block.count(")"):
            raise SystemExit(f"declaration {name} not cut cleanly")
        out.append(block.strip("\n"))
        text = text.replace(m.group(1), "\n", 1)
    return text, out


def mark_actual(text, cls, members):
    text, n = re.subn(r"^(@Singleton\n)?class " + cls + r"\b", lambda m: (m.group(1) or "") + "actual class " + cls, text, count=1, flags=re.M)
    assert n == 1, cls
    for prefix in members:
        pat = re.compile(r"^    " + re.escape(prefix), re.M)
        text, n = pat.subn("    actual " + prefix, text, count=1)
        assert n == 1, (cls, prefix)
    return text


def seam(rel, cls, members, keep, types_imports, expect_body, expect_imports, desktop_body, desktop_imports):
    src = C / rel
    text = src.read_text(encoding="utf-8")
    pkg = re.search(r"^package (\S+)", text, re.M).group(1)
    text, kept = take_decls(text, keep)
    if kept:
        types = f"package {pkg}\n\n" + "".join(f"import {i}\n" for i in types_imports) + ("\n" if types_imports else "") + "\n\n".join(kept) + "\n"
        (C / rel).with_name(cls + "Types.kt").write_text(types, encoding="utf-8")
    text = mark_actual(text, cls, members)
    dst = A / rel
    src.write_text(text, encoding="utf-8")
    git_mv(src, dst)
    exp = f"package {pkg}\n\n" + "".join(f"import {i}\n" for i in expect_imports) + "\n" + expect_body.strip("\n") + "\n"
    (C / rel).with_name(cls + ".expect.kt").write_text(exp, encoding="utf-8")
    desk = f"package {pkg}\n\n" + "".join(f"import {i}\n" for i in desktop_imports) + "\n" + desktop_body.strip("\n") + "\n"
    out = (D / rel).with_name(cls + ".desktop.kt")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(desk, encoding="utf-8")
    print("seam", cls, "kept", keep)


seam(
    "data/emulator/background/BackgroundGames.kt", "BackgroundGames",
    ["val isSupported", "suspend fun keepsAlive(", "fun start()", "suspend fun remember(", "suspend fun refresh(",
     "suspend fun liveGame(", "suspend fun liveApp(", "fun bringToFront(", "suspend fun close("],
    ["RunningApp"], [],
    """
expect class BackgroundGames {
    val isSupported: Boolean

    suspend fun keepsAlive(emulatorPackage: String): Boolean

    fun start()

    suspend fun remember(session: ActiveSession)

    suspend fun refresh(): List<RunningApp>

    suspend fun liveGame(gameId: Long, variantFileId: Long?, emulatorPackage: String): RunningApp?

    suspend fun liveApp(packageName: String): RunningApp?

    fun bringToFront(taskId: Int): Boolean

    suspend fun close(packageName: String)
}
""", ["com.nendo.argosy.data.emulator.ActiveSession"],
    """
@Singleton
actual class BackgroundGames @Inject constructor() {
    actual val isSupported: Boolean get() = false

    actual suspend fun keepsAlive(emulatorPackage: String): Boolean = false

    actual fun start() = Unit

    actual suspend fun remember(session: ActiveSession) = Unit

    actual suspend fun refresh(): List<RunningApp> = emptyList()

    actual suspend fun liveGame(gameId: Long, variantFileId: Long?, emulatorPackage: String): RunningApp? = null

    actual suspend fun liveApp(packageName: String): RunningApp? = null

    actual fun bringToFront(taskId: Int): Boolean = false

    actual suspend fun close(packageName: String) = Unit
}
""", ["com.nendo.argosy.data.emulator.ActiveSession", "javax.inject.Inject", "javax.inject.Singleton"],
)

seam(
    "hardware/HardwareStatsReader.kt", "HardwareStatsReader", ["fun read()"], ["HardwareSample"], [],
    """
expect class HardwareStatsReader {
    fun read(): HardwareSample
}
""", [],
    """
@Singleton
actual class HardwareStatsReader @Inject constructor() {
    actual fun read(): HardwareSample = HardwareSample(
        cpuKhz = null,
        cpuMaxKhz = null,
        gpuHz = null,
        gpuMaxHz = null,
        gpuBusyPercent = null,
        cpuTempC = null,
        gpuTempC = null,
        fanRpm = null,
        fanFraction = null,
        batteryTempC = null,
        batteryLevel = null
    )
}
""", ["javax.inject.Inject", "javax.inject.Singleton"],
)

seam(
    "hardware/LidSleepGuard.kt", "LidSleepGuard", ["val isSupported", "fun start()"], [], [],
    """
expect class LidSleepGuard {
    val isSupported: Boolean

    fun start()
}
""", [],
    """
@Singleton
actual class LidSleepGuard @Inject constructor() {
    actual val isSupported: Boolean get() = false

    actual fun start() = Unit
}
""", ["javax.inject.Inject", "javax.inject.Singleton"],
)

seam(
    "hardware/RadioController.kt", "RadioController",
    ["val wifi:", "val canToggle", "fun status()", "fun setWifiEnabled(", "fun setBluetoothEnabled(",
     "fun hasBluetoothPermission()", "fun pairedDevices()"],
    ["RadioStatus", "BluetoothDeviceKind", "PairedBluetoothDevice"], [],
    """
expect class RadioController {
    val wifi: WifiNetworks

    val canToggle: Boolean

    fun status(): RadioStatus

    fun setWifiEnabled(enabled: Boolean): Boolean

    fun setBluetoothEnabled(enabled: Boolean): Boolean

    fun hasBluetoothPermission(): Boolean

    fun pairedDevices(): List<PairedBluetoothDevice>
}
""", [],
    """
@Singleton
actual class RadioController @Inject constructor() {
    actual val wifi: WifiNetworks = WifiNetworks { null }

    actual val canToggle: Boolean get() = false

    actual fun status(): RadioStatus = RadioStatus()

    actual fun setWifiEnabled(enabled: Boolean): Boolean = false

    actual fun setBluetoothEnabled(enabled: Boolean): Boolean = false

    actual fun hasBluetoothPermission(): Boolean = false

    actual fun pairedDevices(): List<PairedBluetoothDevice> = emptyList()
}
""", ["javax.inject.Inject", "javax.inject.Singleton"],
)

seam(
    "data/access/PrivilegedAccess.kt", "PrivilegedAccess",
    ["val state:", "val prompt:", "suspend fun start(", "suspend fun isMagiskApproved(", "suspend fun recordMagiskApproval(",
     "fun answerPrompt(", "suspend fun enable(", "fun enableOffered("],
    [], [],
    """
expect class PrivilegedAccess {
    val state: StateFlow<PrivilegedAccessState>

    val prompt: StateFlow<PrivilegedRoute?>

    suspend fun start(scope: CoroutineScope)

    suspend fun isMagiskApproved(): Boolean

    suspend fun recordMagiskApproval(granted: Boolean)

    fun answerPrompt(enable: Boolean)

    suspend fun enable(route: PrivilegedRoute)

    fun enableOffered()
}
""", ["com.nendo.argosy.data.model.PrivilegedAccessState", "com.nendo.argosy.data.model.PrivilegedRoute",
      "kotlinx.coroutines.CoroutineScope", "kotlinx.coroutines.flow.StateFlow"],
    """
@Singleton
actual class PrivilegedAccess @Inject constructor() {
    actual val state: StateFlow<PrivilegedAccessState> = MutableStateFlow(PrivilegedAccessState()).asStateFlow()

    actual val prompt: StateFlow<PrivilegedRoute?> = MutableStateFlow<PrivilegedRoute?>(null).asStateFlow()

    actual suspend fun start(scope: CoroutineScope) = Unit

    actual suspend fun isMagiskApproved(): Boolean = false

    actual suspend fun recordMagiskApproval(granted: Boolean) = Unit

    actual fun answerPrompt(enable: Boolean) = Unit

    actual suspend fun enable(route: PrivilegedRoute) = Unit

    actual fun enableOffered() = Unit
}
""", ["com.nendo.argosy.data.model.PrivilegedAccessState", "com.nendo.argosy.data.model.PrivilegedRoute",
      "javax.inject.Inject", "javax.inject.Singleton", "kotlinx.coroutines.CoroutineScope",
      "kotlinx.coroutines.flow.MutableStateFlow", "kotlinx.coroutines.flow.StateFlow", "kotlinx.coroutines.flow.asStateFlow"],
)

for rel in ["libretro/GyroAim.kt", "ui/quickpanel/QuickPanelOverlay.kt", "util/ScreenOrientation.kt",
            "util/ShizukuExecutor.kt", "util/ShizukuShellService.kt"]:
    git_mv(C / rel, A / rel)
    print("moved", rel)
