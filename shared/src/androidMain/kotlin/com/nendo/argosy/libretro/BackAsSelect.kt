package com.nendo.argosy.libretro

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
import com.nendo.argosy.platform.SOURCE_GAMEPAD
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.downTime
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.isFromSource
import com.nendo.argosy.platform.keyCode
import com.nendo.argosy.platform.metaState
import com.nendo.argosy.platform.repeatCount
import com.nendo.argosy.platform.scanCode
import com.nendo.argosy.platform.source
import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.data.local.entity.SelectAsBackMode

/**
 * Whether a Back press plays as Select in game: never with [SelectAsBackMode.OFF], from any
 * gamepad with [SelectAsBackMode.ON], and with [SelectAsBackMode.AUTO] only from a gamepad that
 * reports no Select button of its own.
 */
fun backPlaysAsSelect(mode: SelectAsBackMode, fromGamepad: Boolean, deviceHasSelect: Boolean): Boolean =
    when (mode) {
        SelectAsBackMode.OFF -> false
        SelectAsBackMode.ON -> fromGamepad
        SelectAsBackMode.AUTO -> fromGamepad && !deviceHasSelect
    }

/**
 * [event] as a Select press when it is a Back press that [backPlaysAsSelect] hands to the game,
 * else [event] unchanged.
 */
class BackAsSelectTranslator(private val mode: () -> SelectAsBackMode) {
    private val hasSelectByDevice = mutableMapOf<Int, Boolean>()

    fun translate(event: KeyEvent): KeyEvent {
        if (event.keyCode != AndroidKeyCodes.KEYCODE_BACK) return event
        val device = event.device ?: return event
        val fromGamepad = !device.isVirtual &&
            (device.sources and (SOURCE_GAMEPAD or SOURCE_JOYSTICK)) != 0
        val hasSelect = hasSelectByDevice.getOrPut(device.id) {
            device.hasKeys(AndroidKeyCodes.KEYCODE_BUTTON_SELECT).firstOrNull() == true
        }
        if (!backPlaysAsSelect(mode(), fromGamepad, hasSelect)) return event
        return KeyEvent(
            event.downTime, event.eventTime, event.action, AndroidKeyCodes.KEYCODE_BUTTON_SELECT,
            event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source
        )
    }
}
