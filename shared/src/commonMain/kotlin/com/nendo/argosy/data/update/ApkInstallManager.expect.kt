package com.nendo.argosy.data.update

import kotlinx.coroutines.flow.StateFlow

expect class ApkInstallManager {
    val state: StateFlow<ApkInstallState>

    fun canInstallPackages(): Boolean

    fun openInstallPermissionSettings()

    fun resetState()

    suspend fun installApkForGame(gameId: Long): Boolean

    val pushedPrompt: kotlinx.coroutines.flow.StateFlow<com.nendo.argosy.data.model.PushedApkPrompt?>

    fun confirmPushedInstall()

    fun dismissPushedInstall()
}
