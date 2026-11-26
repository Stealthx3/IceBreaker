package com.nendo.argosy.ui.screens.settings.delegates

import com.nendo.argosy.data.model.SteamShortcutAvailability
import com.nendo.argosy.data.model.SteamShortcutOperation
import com.nendo.argosy.data.model.SteamShortcutOutcome
import com.nendo.argosy.data.model.SteamShortcutStatus
import com.nendo.argosy.platform.SteamShortcuts
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SteamShortcutState(
    val supported: Boolean = false,
    val status: SteamShortcutStatus = SteamShortcutStatus(),
    val busy: Boolean = false,
    val pendingOperation: SteamShortcutOperation? = null,
    val confirmButtonIndex: Int = 1,
    val lastOutcome: SteamShortcutOutcome? = null,
)

class SteamShortcutSettingsDelegate @Inject constructor(
    private val steamShortcuts: SteamShortcuts,
) {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "SteamShortcutSettingsDelegate")
    private val _state = MutableStateFlow(SteamShortcutState(supported = steamShortcuts.isSupported))
    val state: StateFlow<SteamShortcutState> = _state.asStateFlow()

    fun refresh() {
        if (!_state.value.supported) return
        _state.update { it.copy(status = it.status.copy(availability = SteamShortcutAvailability.CHECKING)) }
        scope.launch {
            val status = steamShortcuts.status()
            _state.update { it.copy(status = status) }
        }
    }

    fun request(operation: SteamShortcutOperation) {
        if (_state.value.busy) return
        if (operation == SteamShortcutOperation.UPDATE_ART) {
            run(operation)
        } else {
            _state.update { it.copy(pendingOperation = operation, confirmButtonIndex = 1) }
        }
    }

    fun moveConfirmFocus(delta: Int) {
        _state.update { it.copy(confirmButtonIndex = (it.confirmButtonIndex + delta).coerceIn(0, 1)) }
    }

    fun cancel() {
        _state.update { it.copy(pendingOperation = null) }
    }

    fun confirm() {
        val operation = _state.value.pendingOperation ?: return
        _state.update { it.copy(pendingOperation = null) }
        run(operation)
    }

    private fun run(operation: SteamShortcutOperation) {
        _state.update { it.copy(busy = true, lastOutcome = null) }
        scope.launch {
            val outcome = steamShortcuts.run(operation)
            val status = steamShortcuts.status()
            _state.update { it.copy(busy = false, lastOutcome = outcome, status = status) }
        }
    }
}
