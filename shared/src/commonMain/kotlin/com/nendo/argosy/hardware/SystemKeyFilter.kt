package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.name
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

class SystemKeyFilter(
    private val backGate: BackHoldGate,
    private val hotkeys: SystemHotkeyDetector
) {
    fun onKey(keyCode: Int, action: Int, repeatCount: Int): Boolean {
        val isRepeat = repeatCount > 0
        val isBack = keyCode == AndroidKeyCodes.KEYCODE_BACK
        return when (action) {
            AndroidKeyCodes.ACTION_DOWN -> if (isBack) backGate.onDown(isRepeat) else hotkeys.onKeyDown(keyCode, isRepeat)
            AndroidKeyCodes.ACTION_UP -> if (isBack) backGate.onUp() else hotkeys.onKeyUp(keyCode)
            else -> false
        }
    }

    fun reset() {
        backGate.reset()
        hotkeys.reset()
    }
}
