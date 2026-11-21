package com.nendo.argosy.data.emulator.background

import com.nendo.argosy.data.emulator.ActiveSession

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
