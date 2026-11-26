package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SteamShortcutOperation
import com.nendo.argosy.data.model.SteamShortcutOutcome
import com.nendo.argosy.data.model.SteamShortcutStatus
import javax.inject.Inject

actual class SteamShortcuts @Inject constructor() {
    actual val isSupported: Boolean = false

    actual suspend fun status(): SteamShortcutStatus = SteamShortcutStatus()

    actual suspend fun run(operation: SteamShortcutOperation): SteamShortcutOutcome =
        SteamShortcutOutcome(operation, success = false)
}
