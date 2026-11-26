package com.nendo.argosy.ui.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nendo.argosy.data.model.SteamShortcutAvailability
import com.nendo.argosy.data.model.SteamShortcutOperation
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.res.pluralStringResource
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.ui.components.ActionPreference
import com.nendo.argosy.ui.components.FocusedScroll
import com.nendo.argosy.ui.screens.settings.SettingsUiState
import com.nendo.argosy.ui.screens.settings.SettingsViewModel
import com.nendo.argosy.ui.screens.settings.components.LocalSettingsTouchFocus
import com.nendo.argosy.ui.screens.settings.components.settingsRowItems
import com.nendo.argosy.ui.screens.settings.delegates.SteamShortcutState
import com.nendo.argosy.ui.screens.settings.menu.SettingsLayout
import com.nendo.argosy.ui.theme.Dimens

const val FLATPAK_STEAM_LAUNCH_GRANT = "flatpak override --user --talk-name=org.freedesktop.Flatpak com.valvesoftware.Steam"

internal sealed class SteamShortcutItem(
    val key: String,
    val visibleWhen: (SteamShortcutState) -> Boolean = { true }
) {
    val isFocusable: Boolean get() = this is Action

    data object Intro : SteamShortcutItem("intro")
    data object Status : SteamShortcutItem("status")
    data object FlatpakNotice : SteamShortcutItem(
        "flatpakNotice",
        visibleWhen = { it.status.availability == SteamShortcutAvailability.FOUND && it.status.flatpakSteam }
    )
    class Action(val operation: SteamShortcutOperation) : SteamShortcutItem(
        "action_${operation.name}",
        visibleWhen = { state ->
            state.status.availability == SteamShortcutAvailability.FOUND &&
                state.status.present == (operation != SteamShortcutOperation.ADD)
        }
    )
    data object Result : SteamShortcutItem("result", visibleWhen = { it.busy || it.lastOutcome != null })

    companion object {
        val ALL: List<SteamShortcutItem> = listOf(Intro, Status, FlatpakNotice) +
            SteamShortcutOperation.entries.map(::Action) + Result
    }
}

private val steamShortcutLayout = SettingsLayout<SteamShortcutItem, SteamShortcutState>(
    allItems = SteamShortcutItem.ALL,
    isFocusable = { it.isFocusable },
    visibleWhen = { item, state -> item.visibleWhen(state) }
)

internal fun steamShortcutMaxFocusIndex(state: SteamShortcutState): Int = steamShortcutLayout.maxFocusIndex(state)

internal fun steamShortcutConfirm(viewModel: SettingsViewModel, focusIndex: Int, state: SteamShortcutState) {
    val item = steamShortcutLayout.itemAtFocusIndex(focusIndex, state) as? SteamShortcutItem.Action ?: return
    viewModel.steamShortcutDelegate.request(item.operation)
}

@Composable
fun SteamShortcutSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val listState = rememberLazyListState()
    val state = uiState.steamShortcut
    val visibleItems = remember(state) { steamShortcutLayout.visibleItems(state) }

    FocusedScroll(
        listState = listState,
        focusedIndex = steamShortcutLayout.focusToListIndex(uiState.focusedIndex, state),
        isTouchMode = LocalSettingsTouchFocus.current.isTouchMode
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        settingsRowItems(
            visibleItems,
            key = { it.key },
            focusIndexOf = { steamShortcutLayout.focusIndexOf(it, state) }
        ) { item ->
            when (item) {
                SteamShortcutItem.Intro -> BodyText(stringResource(R.string.settings_steam_shortcut_intro))
                SteamShortcutItem.Status -> BodyText(statusText(state))
                SteamShortcutItem.FlatpakNotice -> BodyText(
                    stringResource(R.string.settings_steam_shortcut_flatpak_notice, FLATPAK_STEAM_LAUNCH_GRANT)
                )
                is SteamShortcutItem.Action -> ActionPreference(
                    title = stringResource(item.operation.titleRes),
                    subtitle = stringResource(item.operation.descriptionRes),
                    icon = item.operation.icon,
                    isDangerous = item.operation == SteamShortcutOperation.REMOVE,
                    isEnabled = !state.busy,
                    isFocused = uiState.focusedIndex == steamShortcutLayout.focusIndexOf(item, state),
                    onClick = { viewModel.steamShortcutDelegate.request(item.operation) }
                )
                SteamShortcutItem.Result -> BodyText(resultText(state))
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Dimens.spacingXs)
    )
}

@Composable
private fun statusText(state: SteamShortcutState): String = when (state.status.availability) {
    SteamShortcutAvailability.UNSUPPORTED, SteamShortcutAvailability.CHECKING ->
        stringResource(R.string.settings_steam_shortcut_status_checking)
    SteamShortcutAvailability.NOT_FOUND -> stringResource(R.string.settings_steam_shortcut_status_not_found)
    SteamShortcutAvailability.FOUND -> if (state.status.present) {
        pluralStringResource(R.plurals.settings_steam_shortcut_status_present, state.status.accounts, state.status.accounts)
    } else {
        stringResource(R.string.settings_steam_shortcut_status_absent)
    }
}

@Composable
private fun resultText(state: SteamShortcutState): String {
    val outcome = state.lastOutcome
    return when {
        state.busy || outcome == null -> stringResource(R.string.settings_steam_shortcut_working)
        !outcome.success -> stringResource(R.string.settings_steam_shortcut_result_failed)
        outcome.operation == SteamShortcutOperation.ADD ->
            pluralStringResource(R.plurals.settings_steam_shortcut_result_added, outcome.accounts, outcome.accounts)
        outcome.operation == SteamShortcutOperation.REMOVE ->
            pluralStringResource(R.plurals.settings_steam_shortcut_result_removed, outcome.accounts, outcome.accounts)
        else -> stringResource(R.string.settings_steam_shortcut_result_art)
    }
}

private val SteamShortcutOperation.titleRes: Int
    get() = when (this) {
        SteamShortcutOperation.ADD -> R.string.settings_steam_shortcut_add_title
        SteamShortcutOperation.UPDATE_ART -> R.string.settings_steam_shortcut_update_art_title
        SteamShortcutOperation.REMOVE -> R.string.settings_steam_shortcut_remove_title
    }

private val SteamShortcutOperation.descriptionRes: Int
    get() = when (this) {
        SteamShortcutOperation.ADD -> R.string.settings_steam_shortcut_add_description
        SteamShortcutOperation.UPDATE_ART -> R.string.settings_steam_shortcut_update_art_description
        SteamShortcutOperation.REMOVE -> R.string.settings_steam_shortcut_remove_description
    }

private val SteamShortcutOperation.icon
    get() = when (this) {
        SteamShortcutOperation.ADD -> Icons.Default.SportsEsports
        SteamShortcutOperation.UPDATE_ART -> Icons.Default.Image
        SteamShortcutOperation.REMOVE -> Icons.Default.Delete
    }
