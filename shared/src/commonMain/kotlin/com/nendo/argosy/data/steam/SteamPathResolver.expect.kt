package com.nendo.argosy.data.steam

expect class SteamPathResolver {
    suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean

    fun findGnStoragePath(): String?

    fun findAllGnStoragePaths(): List<String>
}
