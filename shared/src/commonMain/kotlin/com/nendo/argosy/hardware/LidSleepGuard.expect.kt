package com.nendo.argosy.hardware


expect class LidSleepGuard {
    val isSupported: Boolean

    fun start()
}
