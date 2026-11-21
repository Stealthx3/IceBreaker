package com.nendo.argosy.data.emulator.background

import android.app.ActivityManager
import com.nendo.argosy.platform.PlatformApplication as Application
import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformBuild as Build
import com.nendo.argosy.platform.PlatformProcess as Process
import android.view.Display
import com.nendo.argosy.data.emulator.ActiveSession
import com.nendo.argosy.data.emulator.EmulatorRegistry
import com.nendo.argosy.data.emulator.GameLauncher
import com.nendo.argosy.data.emulator.PlaySessionTracker
import com.nendo.argosy.hardware.builtInGameAppTask
import com.nendo.argosy.libretro.LibretroActivity
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.RootShell
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The apps and games left running. External apps are listed only on a device with a root shell,
 * where the task list and process table can be read; the built-in game left open in Argosy's own
 * task is listed everywhere. [KeepAlivePolicy] decides which games stay alive.
 */
@Singleton
actual class BackgroundGames @Inject constructor(
    private val application: Application,
    private val store: BackgroundGameStore,
    private val playSessionTracker: PlaySessionTracker,
    private val keepAlivePolicy: KeepAlivePolicy,
    private val gameLauncher: GameLauncher
) {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "BackgroundGames")
    private var started = false

    actual val isSupported: Boolean get() = keepAlivePolicy.isSupported

    actual suspend fun keepsAlive(emulatorPackage: String): Boolean = keepAlivePolicy.keepsAlive(emulatorPackage)

    actual fun start() {
        if (started) return
        started = true
        scope.launch { keepAlivePolicy.grantUsageAccessOnRoot() }
        scope.launch {
            var previous: ActiveSession? = null
            playSessionTracker.activeSession.collect { current ->
                val ended = previous
                previous = current
                if (current == null && ended != null) remember(ended)
                if (current != null) store.remove(current.emulatorPackage)
            }
        }
    }

    /**
     * Records the game [session] leaves running in its emulator, when that emulator stays alive.
     * Every session end is recorded on its own; a caller that needs the record at once can call
     * this directly.
     */
    actual suspend fun remember(session: ActiveSession) {
        if (session.emulatorPackage == EmulatorRegistry.BUILTIN_PACKAGE) return
        if (!keepsAlive(session.emulatorPackage)) return
        val pid = read()?.processes?.get(session.emulatorPackage)?.pid ?: return
        store.put(
            BackgroundGame(
                emulatorPackage = session.emulatorPackage,
                gameId = session.gameId,
                variantFileId = session.variantFileId,
                pid = pid,
                endedAtMillis = System.currentTimeMillis()
            )
        )
        Logger.debug(TAG, "kept ${session.emulatorPackage} pid=$pid for gameId=${session.gameId}")
    }

    /**
     * Every app with a task, Argosy excluded, joined to the game its emulator holds, then the
     * built-in game left open in Argosy's own task. Off root only the built-in game is listed.
     * Records whose emulator process is gone or was restarted are dropped from the store here.
     */
    actual suspend fun refresh(): List<RunningApp> = externalApps() + listOfNotNull(builtInApp())

    private suspend fun externalApps(): List<RunningApp> {
        if (!isSupported) return emptyList()
        val snapshot = read() ?: return emptyList()
        val games = liveRecords(snapshot)
        return snapshot.tasks
            .filter { it.packageName != application.packageName }
            .distinctBy { it.packageName }
            .map { task ->
                RunningApp(
                    taskId = task.taskId,
                    packageName = task.packageName,
                    displayId = task.displayId,
                    rssKb = snapshot.processes[task.packageName]?.rssKb,
                    game = games[task.packageName]
                )
            }
    }

    private fun builtInApp(): RunningApp? {
        val live = LibretroActivity.liveGame ?: return null
        val task = builtInTask() ?: return null
        val taskId = runCatching { task.taskInfo.idCompat() }.getOrNull() ?: return null
        return RunningApp(
            taskId = taskId,
            packageName = EmulatorRegistry.BUILTIN_PACKAGE,
            displayId = Display.INVALID_DISPLAY,
            rssKb = null,
            game = BackgroundGame(
                emulatorPackage = EmulatorRegistry.BUILTIN_PACKAGE,
                gameId = live.gameId,
                variantFileId = live.variantFileId,
                pid = Process.myPid(),
                endedAtMillis = 0L
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun ActivityManager.RecentTaskInfo.idCompat(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) taskId else persistentId

    private fun builtInTask(): ActivityManager.AppTask? {
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return builtInGameAppTask(activityManager)
    }

    /**
     * The running app holding [gameId] (and [variantFileId]) in [emulatorPackage], or null when
     * that emulator is not running it.
     */
    actual suspend fun liveGame(gameId: Long, variantFileId: Long?, emulatorPackage: String): RunningApp? =
        liveApp(emulatorPackage)?.takeIf { it.game?.gameId == gameId && it.game.variantFileId == variantFileId }

    actual suspend fun liveApp(packageName: String): RunningApp? =
        if (packageName == EmulatorRegistry.BUILTIN_PACKAGE) builtInApp()
        else if (!isSupported) null
        else externalApps().firstOrNull { it.packageName == packageName }

    /**
     * Brings [taskId] to the front without sending its app an intent, so the app resumes where it
     * was. False when the system refused.
     */
    actual fun bringToFront(taskId: Int): Boolean = try {
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
        true
    } catch (e: SecurityException) {
        Logger.warn(TAG, "moveTaskToFront refused for task $taskId", e)
        false
    }

    actual suspend fun close(packageName: String) {
        store.remove(packageName)
        if (packageName == EmulatorRegistry.BUILTIN_PACKAGE) {
            withContext(Dispatchers.Main) { builtInTask()?.let { runCatching { it.finishAndRemoveTask() } } }
            return
        }
        gameLauncher.forceStopEmulator(packageName)
    }

    private fun liveRecords(snapshot: Snapshot): Map<String, BackgroundGame> {
        val live = mutableMapOf<String, BackgroundGame>()
        for (record in store.all()) {
            if (snapshot.processes[record.emulatorPackage]?.pid == record.pid) {
                live[record.emulatorPackage] = record
            } else {
                store.remove(record.emulatorPackage)
            }
        }
        return live
    }

    private data class Snapshot(val tasks: List<RunningTask>, val processes: Map<String, ProcessStat>)

    private suspend fun read(): Snapshot? = withContext(Dispatchers.IO) {
        val result = RootShell.run(application, "am stack list\necho $PS_MARKER\nps -A -o PID,RSS,NAME")
            ?.takeIf { it.succeeded }
            ?: return@withContext null
        val split = result.output.indexOf(PS_MARKER)
        if (split < 0) return@withContext null
        Snapshot(
            tasks = RunningTaskParser.parseTasks(result.output.subList(0, split)),
            processes = RunningTaskParser.parseProcesses(result.output.subList(split + 1, result.output.size))
        )
    }

    private companion object {
        const val TAG = "BackgroundGames"
        const val PS_MARKER = "--argosy-ps--"
    }
}
