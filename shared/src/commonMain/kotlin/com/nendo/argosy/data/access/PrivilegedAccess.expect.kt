package com.nendo.argosy.data.access

import com.nendo.argosy.data.model.PrivilegedAccessState
import com.nendo.argosy.data.model.PrivilegedRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

expect class PrivilegedAccess {
    val state: StateFlow<PrivilegedAccessState>

    val prompt: StateFlow<PrivilegedRoute?>

    suspend fun start(scope: CoroutineScope)

    suspend fun isMagiskApproved(): Boolean

    suspend fun recordMagiskApproval(granted: Boolean)

    fun answerPrompt(enable: Boolean)

    suspend fun enable(route: PrivilegedRoute)

    fun enableOffered()
}
