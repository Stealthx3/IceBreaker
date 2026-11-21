import re
import subprocess
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
            t = t.replace(a, b, 1 if count == 1 else -1)
    p.write_text(t, encoding="utf-8")


def write(path, text):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.lstrip("\n"), encoding="utf-8")


def ensure_import(path, imp):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    if f"import {imp}\n" in t:
        return
    t = re.sub(r"^(package [^\n]+\n)", r"\1\nimport " + imp + "\n", t, count=1, flags=re.M)
    p.write_text(t, encoding="utf-8")


# SteamPathResolver keeps upstream's three members
write(C / "data/steam/SteamPathResolver.expect.kt", """
package com.nendo.argosy.data.steam

expect class SteamPathResolver {
    suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean

    fun findGnStoragePath(): String?

    fun findAllGnStoragePaths(): List<String>
}
""")
write(D / "data/steam/SteamPathResolver.desktop.kt", """
package com.nendo.argosy.data.steam

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SteamPathResolver @Inject constructor() {
    actual suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean = false

    actual fun findGnStoragePath(): String? = null

    actual fun findAllGnStoragePaths(): List<String> = emptyList()
}
""")
edit(A / "data/steam/SteamPathResolver.kt", [(r"^(\s{4})suspend fun isGameInstalled\(game: GameEntity\)", r"\1actual suspend fun isGameInstalled(game: GameEntity)")], regex=True)
subprocess.run(["git", "rm", "-q", str(C / "data/steam/SteamInstallVolume.kt")], check=True)

# supportsSource
t = (A / "platform/PlatformInputDevice.android.kt").read_text(encoding="utf-8").rstrip("\n")
(A / "platform/PlatformInputDevice.android.kt").write_text(t + "\n\nactual fun PlatformInputDevice.supportsSource(source: Int): Boolean = supportsSource(source)\n", encoding="utf-8")
t = (D / "platform/PlatformInputDevice.desktop.kt").read_text(encoding="utf-8").rstrip("\n")
(D / "platform/PlatformInputDevice.desktop.kt").write_text(t + "\n\nactual fun PlatformInputDevice.supportsSource(source: Int): Boolean = (sources and source) == source\n", encoding="utf-8")

# StatFs block sizes
edit(C / "platform/PlatformStatFs.kt", [("    val availableBytes: Long\n", "    val availableBytes: Long\n    val blockSizeLong: Long\n    val blockCountLong: Long\n    val availableBlocksLong: Long\n")])
edit(A / "platform/PlatformStatFs.android.kt", [("    actual val availableBytes: Long get() = stat.availableBytes\n",
    "    actual val availableBytes: Long get() = stat.availableBytes\n    actual val blockSizeLong: Long get() = stat.blockSizeLong\n    actual val blockCountLong: Long get() = stat.blockCountLong\n    actual val availableBlocksLong: Long get() = stat.availableBlocksLong\n")])
edit(D / "platform/PlatformStatFs.desktop.kt", [("    actual val availableBytes: Long get() = file.usableSpace\n",
    "    actual val availableBytes: Long get() = file.usableSpace\n    actual val blockSizeLong: Long get() = BLOCK_SIZE\n    actual val blockCountLong: Long get() = file.totalSpace / BLOCK_SIZE\n    actual val availableBlocksLong: Long get() = file.usableSpace / BLOCK_SIZE\n\n    private companion object {\n        const val BLOCK_SIZE = 4096L\n    }\n")])

# SharedPreferences.getAll
edit(C / "platform/PlatformSharedPreferences.kt", [("    fun contains(key: String?): Boolean\n", "    fun contains(key: String?): Boolean\n    fun getAll(): Map<String, *>\n")])
edit(D / "platform/PlatformSharedPreferences.desktop.kt", [
    ("    actual fun contains(key: String?): Boolean\n", "    actual fun contains(key: String?): Boolean\n    actual fun getAll(): Map<String, *>\n"),
    ("    override fun contains(key: String?): Boolean = stored.containsKey(key.orEmpty())\n",
     "    override fun contains(key: String?): Boolean = stored.containsKey(key.orEmpty())\n"
     "    override fun getAll(): Map<String, *> = stored.mapValues { (_, raw) ->\n"
     "        when {\n"
     "            raw.startsWith(\"i:\") -> raw.removePrefix(\"i:\").toIntOrNull()\n"
     "            raw.startsWith(\"l:\") -> raw.removePrefix(\"l:\").toLongOrNull()\n"
     "            raw.startsWith(\"b:\") -> raw.removePrefix(\"b:\").toBooleanStrictOrNull()\n"
     "            else -> raw.removePrefix(\"s:\")\n"
     "        }\n"
     "    }\n"),
])
edit(C / "data/sync/ScreenshotUploadLedger.kt", [("prefs.all.forEach {", "prefs.getAll().forEach {")])
edit(C / "data/emulator/background/BackgroundGameStore.kt", [("prefs.all.mapNotNull {", "prefs.getAll().mapNotNull {")])

# Intent boolean extras
edit(C / "platform/PlatformIntent.kt", [("expect fun PlatformIntentType.getStringExtra(name: String): String?\n",
    "expect fun PlatformIntentType.getStringExtra(name: String): String?\n\nexpect fun PlatformIntentType.getBooleanExtra(name: String, defaultValue: Boolean): Boolean\n")])
edit(A / "platform/PlatformIntent.android.kt", [("actual fun PlatformIntentType.getStringExtra(name: String): String? = getStringExtra(name)\n",
    "actual fun PlatformIntentType.getStringExtra(name: String): String? = getStringExtra(name)\n\nactual fun PlatformIntentType.getBooleanExtra(name: String, defaultValue: Boolean): Boolean = getBooleanExtra(name, defaultValue)\n")])
edit(D / "platform/PlatformIntent.desktop.kt", [("actual fun PlatformIntentType.getStringExtra(name: String): String? = extras[name] as? String\n",
    "actual fun PlatformIntentType.getStringExtra(name: String): String? = extras[name] as? String\n\nactual fun PlatformIntentType.getBooleanExtra(name: String, defaultValue: Boolean): Boolean = extras[name] as? Boolean ?: defaultValue\n")])
ensure_import(C / "ui/screens/common/LaunchHandoff.kt", "com.nendo.argosy.platform.getBooleanExtra")

# Keyboard visibility
write(C / "ui/util/ImeVisibility.kt", """
package com.nendo.argosy.ui.util

import androidx.compose.runtime.Composable

@Composable
expect fun isImeVisible(): Boolean
""")
write(A / "ui/util/ImeVisibility.android.kt", """
package com.nendo.argosy.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isImeVisible(): Boolean = androidx.compose.foundation.layout.WindowInsets.isImeVisible
""")
write(D / "ui/util/ImeVisibility.desktop.kt", """
package com.nendo.argosy.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun isImeVisible(): Boolean = false
""")
edit(C / "ui/screens/firstrun/FirstRunScreen.kt", [
    ("import androidx.compose.foundation.layout.isImeVisible\n", "import com.nendo.argosy.ui.util.isImeVisible\n"),
    ("WindowInsets.isImeVisible", "isImeVisible()"),
])

# Transient overlay notice
write(C / "platform/TransientNotice.kt", """
package com.nendo.argosy.platform

expect object TransientNotice {
    fun show(context: PlatformContext, title: String, subtitle: String? = null)
}
""")
write(A / "platform/TransientNotice.android.kt", """
package com.nendo.argosy.platform

import com.nendo.argosy.data.social.SocialOverlayService

actual object TransientNotice {
    actual fun show(context: PlatformContext, title: String, subtitle: String?) =
        SocialOverlayService.show(context, title, subtitle)
}
""")
write(D / "platform/TransientNotice.desktop.kt", """
package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger

actual object TransientNotice {
    actual fun show(context: PlatformContext, title: String, subtitle: String?) {
        Logger.info("TransientNotice", listOfNotNull(title, subtitle).joinToString(" | "))
    }
}
""")
edit(C / "data/sync/ScreenshotUploadNotices.kt", [
    ("import com.nendo.argosy.data.social.SocialOverlayService\n", "import com.nendo.argosy.platform.TransientNotice\n"),
])
edit(C / "data/sync/ScreenshotUploadNotices.kt", [(r"SocialOverlayService\.show\(", "TransientNotice.show(")], regex=True, count=0)

# Accessibility service state
write(C / "platform/AccessibilityServiceState.kt", """
package com.nendo.argosy.platform

expect object AccessibilityServiceState {
    fun isEnabled(context: PlatformContext): Boolean
}
""")
write(A / "platform/AccessibilityServiceState.android.kt", """
package com.nendo.argosy.platform

import com.nendo.argosy.hardware.FocusAccessibilityService

actual object AccessibilityServiceState {
    actual fun isEnabled(context: PlatformContext): Boolean = FocusAccessibilityService.isEnabled(context)
}
""")
write(D / "platform/AccessibilityServiceState.desktop.kt", """
package com.nendo.argosy.platform

actual object AccessibilityServiceState {
    actual fun isEnabled(context: PlatformContext): Boolean = false
}
""")
edit(C / "ui/screens/settings/delegates/PermissionsSettingsDelegate.kt", [
    ("import com.nendo.argosy.hardware.FocusAccessibilityService\n", "import com.nendo.argosy.platform.AccessibilityServiceState\n"),
    ("FocusAccessibilityService.isEnabled(application)", "AccessibilityServiceState.isEnabled(application)"),
])
edit(C / "platform/PlatformSettings.kt", [("    val ACTION_MANAGE_WRITE_SETTINGS: String\n", "    val ACTION_MANAGE_WRITE_SETTINGS: String\n    val ACTION_ACCESSIBILITY_SETTINGS: String\n")])
edit(A / "platform/PlatformSettings.android.kt", [(r"^(\s+actual val ACTION_MANAGE_WRITE_SETTINGS: String get\(\) = [^\n]+\n)", r"\1    actual val ACTION_ACCESSIBILITY_SETTINGS: String get() = Settings.ACTION_ACCESSIBILITY_SETTINGS\n")], regex=True)
edit(D / "platform/PlatformSettings.desktop.kt", [(r"^(\s+actual val ACTION_MANAGE_WRITE_SETTINGS: String = [^\n]+\n)", r"\1    actual val ACTION_ACCESSIBILITY_SETTINGS: String = \"android.settings.ACCESSIBILITY_SETTINGS\"\n")], regex=True)

# Desktop storage attribution: upstream removed Steam download staging
edit(D / "data/storage/StorageAttributionRepository.desktop.kt", [("            AppPaths.steamStagingRoot(context.filesDir),\n", "")])

# APPLICATION_ID in the generated build config
edit("shared/build.gradle.kts", [(
    '        "VERSION_CODE" to ("int" to appAndroid.defaultConfig.versionCode.toString()),\n',
    '        "VERSION_CODE" to ("int" to appAndroid.defaultConfig.versionCode.toString()),\n'
    '        "APPLICATION_ID" to ("String" to "\\"${appAndroid.defaultConfig.applicationId}${appAndroid.buildTypes.getByName(type).applicationIdSuffix.orEmpty()}\\""),\n')])
print("batch3 done")
