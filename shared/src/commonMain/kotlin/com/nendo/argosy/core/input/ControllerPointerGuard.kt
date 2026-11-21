package com.nendo.argosy.core.input

import com.nendo.argosy.util.PlatformLog as Log
import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.controllerNumber
import com.nendo.argosy.platform.descriptor
import com.nendo.argosy.platform.id
import com.nendo.argosy.platform.isEnabled
import com.nendo.argosy.platform.isExternal
import com.nendo.argosy.platform.isVirtual
import com.nendo.argosy.platform.name
import com.nendo.argosy.platform.productId
import com.nendo.argosy.platform.sources
import com.nendo.argosy.platform.vendorId
import com.nendo.argosy.platform.SOURCE_MOUSE
import com.nendo.argosy.platform.SOURCE_MOUSE_RELATIVE
import com.nendo.argosy.platform.SOURCE_TOUCHPAD
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.actionMasked
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.platform.isFromSource
import com.nendo.argosy.platform.source
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.data.emulator.PlaySessionTracker
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "ControllerPointerGuard"

/**
 * Keeps a game controller's touchpad pointer from moving input focus to one launcher window while
 * [follow]'s flow is true. Such a pointer reaching the window while another activity is in front
 * makes the window not touchable for [PASS_THROUGH_MS].
 */
class ControllerPointerGuard(
    private val windowName: String,
    private val scope: CoroutineScope,
    private val setWindowTouchable: (Boolean) -> Unit,
    private val isControllerPointer: (MotionEvent) -> Boolean = ::isControllerTouchpadEvent
) {
    @Volatile
    private var guarding = false
    private var inFront = false
    private var passThrough: Job? = null
    private var following: Job? = null

    fun follow(guardingFlow: Flow<Boolean>) {
        following?.cancel()
        following = scope.launch {
            guardingFlow.distinctUntilChanged().collect { active ->
                guarding = active
                if (!active) release()
            }
        }
    }

    fun consumes(event: MotionEvent): Boolean {
        if (!guarding || !isControllerPointer(event)) return false
        if (!inFront && passThrough == null) passPointersThrough()
        return true
    }

    fun onInFrontChanged(isInFront: Boolean) {
        inFront = isInFront
        if (isInFront) release()
    }

    fun release() {
        val job = passThrough ?: return
        job.cancel()
        endPassThrough()
    }

    private fun passPointersThrough() {
        Log.i(TAG, "Controller pointer over the $windowName during a game session, pointer input passes through it")
        setWindowTouchable(false)
        passThrough = scope.launch {
            delay(PASS_THROUGH_MS)
            endPassThrough()
        }
    }

    private fun endPassThrough() {
        passThrough = null
        setWindowTouchable(true)
    }

    companion object {
        const val PASS_THROUGH_MS = 3_000L
    }
}

fun controllerTouchpadGuarding(
    sessions: PlaySessionTracker,
    preferences: UserPreferencesRepository
): Flow<Boolean> = combine(sessions.activeSession, preferences.userPreferences) { session, prefs ->
    session != null && prefs.ignoreControllerTouchpad
}

fun isControllerTouchpadEvent(event: MotionEvent): Boolean {
    val pointerEvent = event.isFromSource(SOURCE_MOUSE) ||
        event.isFromSource(SOURCE_MOUSE_RELATIVE) ||
        event.isFromSource(SOURCE_TOUCHPAD)
    return pointerEvent && event.device?.isControllerTouchpad() == true
}
