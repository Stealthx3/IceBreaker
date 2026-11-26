package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SteamShortcutAvailability
import com.nendo.argosy.data.model.SteamShortcutOperation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SteamHelperOutputTest {

    @Test
    fun noSteamReadsAsNotFound() {
        val status = SteamHelperOutput.status(HelperResult(1, """{"steam": false}"""))
        assertEquals(SteamShortcutAvailability.NOT_FOUND, status.availability)
    }

    @Test
    fun statusCountsAccountsAndSeesFlatpakSteam() {
        val output = """
            {"version": "1", "op": "status", "results": [
              {"user": "1", "present": true, "shortcut_id": 1, "steam": "/s", "flatpak_steam": false},
              {"user": "2", "present": false, "shortcut_id": 1, "steam": "/f", "flatpak_steam": true}
            ]}
        """.trimIndent()
        val status = SteamHelperOutput.status(HelperResult(0, output))
        assertEquals(SteamShortcutAvailability.FOUND, status.availability)
        assertTrue(status.present)
        assertTrue(status.flatpakSteam)
        assertEquals(2, status.accounts)
    }

    @Test
    fun anAddCountsTheAccountsItChanged() {
        val output = """{"version": "1", "op": "add", "results": [{"user": "1", "changed": true}, {"user": "2", "changed": true}]}"""
        val outcome = SteamHelperOutput.outcome(SteamShortcutOperation.ADD, HelperResult(0, output))
        assertTrue(outcome.success)
        assertEquals(2, outcome.accounts)
    }

    @Test
    fun aRefusedOrBrokenRunIsAFailure() {
        assertFalse(SteamHelperOutput.outcome(SteamShortcutOperation.ADD, HelperResult(3, """{"steam_running": true}""")).success)
        assertFalse(SteamHelperOutput.outcome(SteamShortcutOperation.REMOVE, HelperResult(-1, "")).success)
    }
}
