package com.nendo.argosy.data.model

enum class SteamShortcutAvailability { UNSUPPORTED, CHECKING, NOT_FOUND, FOUND }

enum class SteamShortcutOperation { ADD, UPDATE_ART, REMOVE }

data class SteamShortcutStatus(
    val availability: SteamShortcutAvailability = SteamShortcutAvailability.UNSUPPORTED,
    val present: Boolean = false,
    val flatpakSteam: Boolean = false,
    val accounts: Int = 0,
)

data class SteamShortcutOutcome(
    val operation: SteamShortcutOperation,
    val success: Boolean,
    val accounts: Int = 0,
)
