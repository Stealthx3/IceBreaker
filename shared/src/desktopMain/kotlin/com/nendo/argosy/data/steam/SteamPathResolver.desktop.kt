package com.nendo.argosy.data.steam

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SteamPathResolver @Inject constructor() {
    actual suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean = false

    actual fun findGnStoragePath(): String? = null

    actual fun findAllGnStoragePaths(): List<String> = emptyList()
}
