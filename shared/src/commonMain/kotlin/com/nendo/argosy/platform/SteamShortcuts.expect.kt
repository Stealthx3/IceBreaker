package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SteamShortcutOperation
import com.nendo.argosy.data.model.SteamShortcutOutcome
import com.nendo.argosy.data.model.SteamShortcutStatus

/**
 * The app's own non-Steam shortcut, with its art, in every Steam install and account on this
 * computer. [isSupported] is false where the app is not a Flatpak carrying the Steam helper.
 */
expect class SteamShortcuts {
    val isSupported: Boolean

    suspend fun status(): SteamShortcutStatus

    suspend fun run(operation: SteamShortcutOperation): SteamShortcutOutcome
}
