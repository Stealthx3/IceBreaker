package com.nendo.argosy.data.emulator.background

import com.nendo.argosy.data.emulator.ActiveSession
import javax.inject.Inject
import javax.inject.Singleton

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
