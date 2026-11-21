package com.nendo.argosy.data.access

import com.nendo.argosy.data.model.PrivilegedAccessState
import com.nendo.argosy.data.model.PrivilegedRoute
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
actual class PrivilegedAccess @Inject constructor() {
    actual val state: StateFlow<PrivilegedAccessState> = MutableStateFlow(PrivilegedAccessState()).asStateFlow()

    actual val prompt: StateFlow<PrivilegedRoute?> = MutableStateFlow<PrivilegedRoute?>(null).asStateFlow()

    actual suspend fun start(scope: CoroutineScope) = Unit

    actual suspend fun isMagiskApproved(): Boolean = false

    actual suspend fun recordMagiskApproval(granted: Boolean) = Unit

    actual fun answerPrompt(enable: Boolean) = Unit

    actual suspend fun enable(route: PrivilegedRoute) = Unit

    actual fun enableOffered() = Unit
}
