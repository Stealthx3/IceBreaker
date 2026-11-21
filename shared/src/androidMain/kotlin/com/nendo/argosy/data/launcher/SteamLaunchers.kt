package com.nendo.argosy.data.launcher

import android.content.Context
import com.nendo.argosy.domain.model.LaunchPromptOption

actual object SteamLaunchers {
    actual val all: List<SteamLauncher> = listOf(
        GameHubLauncher,
        GameHubLiteLauncher,
        GameHubLiteAntutuLauncher,
        GameHubLiteLudashiLauncher,
        GameHubLitePubgLauncher,
        GameNativeLauncher,
        DroidDeckLauncher
    )

    private val gameHubVariants: List<SteamLauncher> = listOf(
        GameHubLauncher,
        GameHubLiteLauncher,
        GameHubLiteAntutuLauncher,
        GameHubLiteLudashiLauncher,
        GameHubLitePubgLauncher
    )

    actual fun getInstalled(context: Context): List<SteamLauncher> =
        all.filter { it.isInstalled(context) }

    actual fun getByPackage(packageName: String): SteamLauncher? =
        all.find { it.packageName == packageName }

    actual fun getPreferred(context: Context): SteamLauncher? =
        getInstalled(context).firstOrNull()

    private val promptOptions: Map<SteamLauncher, LaunchPromptOption> = mapOf(
        GameNativeLauncher to LaunchPromptOption.LAUNCH_WITH_GAMENATIVE,
        DroidDeckLauncher to LaunchPromptOption.LAUNCH_WITH_DROIDDECK
    )

    actual fun promptOptionFor(packageName: String): LaunchPromptOption? =
        promptOptions.entries.find { it.key.packageName == packageName }?.value

    actual fun forPromptOption(option: LaunchPromptOption): SteamLauncher? =
        promptOptions.entries.find { it.value == option }?.key

    /**
     * Installed launch candidates. Multiple reports retain the launch-time choice;
     * one candidate can launch directly.
     */
    fun launchCandidates(
        storedPackage: String?,
        pinned: Boolean,
        reporting: List<String>,
        isInstalled: (String) -> Boolean
    ): List<String> {
        val stored = storedPackage?.takeIf { getByPackage(it) != null && isInstalled(it) }
        if (pinned && stored != null) return listOf(stored)
        val reported = reporting.distinct().filter { promptOptionFor(it) != null && isInstalled(it) }
        if (reported.size > 1) return reported
        if (stored != null) return listOf(stored)
        if (storedPackage != null) return reported
        return all.firstOrNull { isInstalled(it.packageName) }?.let { listOf(it.packageName) }.orEmpty()
    }

    actual fun getMarkOptions(context: Context): List<MarkOption> = buildList {
        if (GameNativeLauncher.isInstalled(context)) add(MarkOption.GameNative)
        gameHubVariants.firstOrNull { it.isInstalled(context) }?.let {
            add(MarkOption.GameHub(it.packageName))
        }
        if (DroidDeckLauncher.isInstalled(context)) add(MarkOption.DroidDeck)
    }

    /** Resolve a stored launcher package back to a user-friendly group label. */
    actual fun displayNameForPackage(launcherPackage: String?): String = when {
        launcherPackage == null -> ""
        launcherPackage == GameNativeLauncher.packageName -> GameNativeLauncher.displayName
        gameHubVariants.any { it.packageName == launcherPackage } -> "GameHub"
        else -> getByPackage(launcherPackage)?.displayName ?: launcherPackage
    }
}
