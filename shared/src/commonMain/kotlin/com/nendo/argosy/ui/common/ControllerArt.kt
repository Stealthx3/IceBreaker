package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.controllerNumber
import com.nendo.argosy.platform.descriptor
import com.nendo.argosy.platform.id
import com.nendo.argosy.platform.isEnabled
import com.nendo.argosy.platform.isExternal
import com.nendo.argosy.platform.isVirtual
import com.nendo.argosy.platform.name
import com.nendo.argosy.platform.productId
import com.nendo.argosy.platform.sources
import com.nendo.argosy.platform.vendorId
import androidx.annotation.DrawableRes
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.core.input.ControllerDetector
import com.nendo.argosy.util.ScreenCatalog

enum class ControllerKind(@DrawableRes val iconRes: Int) {
    MODERN(R.drawable.ic_pad_modern),
    SYMMETRIC(R.drawable.ic_pad_symmetric),
    RETRO(R.drawable.ic_pad_retro),
    JOYCON(R.drawable.ic_pad_joycon),
    HANDHELD(R.drawable.ic_pad_handheld),
    DUAL_SCREEN(R.drawable.ic_pad_dual_screen)
}

/**
 * Picks the silhouette that stands in for a controller. A pad built into the device shows the
 * handheld itself; an external pad is matched by USB vendor and product id against SDL's
 * controller table, then by name, and falls back to a modern twin-stick pad.
 */
object ControllerArt {
    private const val TABLE_ASSET = "controllers/sdl_controller_list.h"

    private const val NINTENDO = 0x057e

    private val nintendoProducts = mapOf(
        0x2017 to ControllerKind.RETRO,
        0x201e to ControllerKind.RETRO,
        0x0306 to ControllerKind.RETRO,
        0x0330 to ControllerKind.RETRO,
        0x2066 to ControllerKind.JOYCON,
        0x2067 to ControllerKind.JOYCON,
        0x2068 to ControllerKind.JOYCON
    )

    private val kindBySdlType = mapOf(
        "PS3Controller" to ControllerKind.SYMMETRIC,
        "PS4Controller" to ControllerKind.SYMMETRIC,
        "XInputPS4Controller" to ControllerKind.SYMMETRIC,
        "PS5Controller" to ControllerKind.SYMMETRIC,
        "PS5EdgeController" to ControllerKind.SYMMETRIC,
        "SwitchJoyConLeft" to ControllerKind.JOYCON,
        "SwitchJoyConRight" to ControllerKind.JOYCON,
        "SwitchJoyConPair" to ControllerKind.JOYCON
    )

    private val kindByName = listOf(
        "dualsense" to ControllerKind.SYMMETRIC,
        "dualshock" to ControllerKind.SYMMETRIC,
        "playstation" to ControllerKind.SYMMETRIC,
        "joy-con" to ControllerKind.JOYCON,
        "snes" to ControllerKind.RETRO,
        "sn30" to ControllerKind.RETRO,
        "sf30" to ControllerKind.RETRO,
        "genesis" to ControllerKind.RETRO,
        "mega drive" to ControllerKind.RETRO,
        "m30" to ControllerKind.RETRO,
        "nes" to ControllerKind.RETRO,
        "wii remote" to ControllerKind.RETRO,
        "rvl-cnt" to ControllerKind.RETRO
    )

    private val tableLine = Regex(
        """^\s*\{\s*MAKE_CONTROLLER_ID\(\s*0x([0-9a-fA-F]+),\s*0x([0-9a-fA-F]+)\s*\),\s*k_eControllerType_(\w+)"""
    )

    @Volatile private var table: Map<Int, String>? = null

    fun kindOf(context: Context, device: InputDevice?): ControllerKind = when {
        device == null -> ControllerKind.MODERN
        ControllerDetector.isBuiltInPad(device) -> handheldKind(builtInScreenCount(context))
        else -> externalKind(device.vendorId, device.productId, device.name, sdlTable(context))
    }

    fun handheldKind(builtInScreens: Int): ControllerKind =
        if (builtInScreens > 1) ControllerKind.DUAL_SCREEN else ControllerKind.HANDHELD

    private fun builtInScreenCount(context: Context): Int =
        ScreenCatalog(context.applicationContext).attachedScreens().count { it.builtIn }

    fun externalKind(vendorId: Int, productId: Int, name: String?, sdlTypes: Map<Int, String>): ControllerKind {
        if (vendorId == NINTENDO) nintendoProducts[productId]?.let { return it }
        sdlTypes[controllerKey(vendorId, productId)]?.let { type -> kindBySdlType[type]?.let { return it } }
        val words = name?.lowercase().orEmpty()
        return kindByName.firstOrNull { (pattern, _) -> Regex("\\b${Regex.escape(pattern)}\\b").containsMatchIn(words) }
            ?.second ?: ControllerKind.MODERN
    }

    fun parseSdlTable(lines: Sequence<String>): Map<Int, String> =
        lines.mapNotNull { line ->
            tableLine.find(line)?.destructured?.let { (vid, pid, type) ->
                controllerKey(vid.toInt(16), pid.toInt(16)) to type
            }
        }.toMap()

    private fun controllerKey(vendorId: Int, productId: Int): Int = (vendorId shl 16) or (productId and 0xffff)

    private fun sdlTable(context: Context): Map<Int, String> {
        table?.let { return it }
        synchronized(this) {
            table?.let { return it }
            val parsed = runCatching {
                context.assets.open(TABLE_ASSET).bufferedReader().useLines { parseSdlTable(it) }
            }.getOrDefault(emptyMap())
            table = parsed
            return parsed
        }
    }
}
