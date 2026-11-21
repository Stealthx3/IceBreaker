package com.nendo.argosy.hardware

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
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.PlatformIntent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nendo.argosy.di.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads [HardwareSample]s from sysfs and the battery broadcast. The CPU reading is the fastest
 * cluster's current clock; each temperature is the hottest zone of its kind.
 */
@Singleton
actual class HardwareStatsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fanController: FanController
) {
    private val cpuPolicies: List<File> by lazy {
        File(CPUFREQ_DIR).listFiles { file -> file.name.startsWith("policy") }?.sortedBy { it.name }.orEmpty()
    }

    private val thermalZones: List<Pair<String, File>> by lazy {
        File(THERMAL_DIR).listFiles { file -> file.name.startsWith("thermal_zone") }
            ?.mapNotNull { zone ->
                val type = runCatching { File(zone, "type").readText().trim() }.getOrNull() ?: return@mapNotNull null
                type to File(zone, "temp")
            }
            .orEmpty()
    }

    private val cpuZones by lazy { thermalZones.filter { (type, _) -> CPU_ZONE.matches(type) }.map { it.second } }
    private val gpuZones by lazy { thermalZones.filter { (type, _) -> GPU_ZONE.matches(type) }.map { it.second } }

    actual fun read(): HardwareSample {
        val clusters = cpuPolicies.mapNotNull { policy ->
            val current = readLong(File(policy, "scaling_cur_freq"))?.toInt() ?: return@mapNotNull null
            current to readLong(File(policy, "cpuinfo_max_freq"))?.toInt()
        }
        val fastest = clusters.maxByOrNull { it.first }
        val battery = context.registerReceiver(null, IntentFilter(PlatformIntent.ACTION_BATTERY_CHANGED))
        val fanAvailable = fanController.isAvailable()
        return HardwareSample(
            cpuKhz = fastest?.first,
            cpuMaxKhz = clusters.mapNotNull { it.second }.maxOrNull(),
            gpuHz = readLong(File(KGSL_DIR, "gpuclk")),
            gpuMaxHz = readLong(File(KGSL_DIR, "max_gpuclk")),
            gpuBusyPercent = readLong(File(KGSL_DIR, "gpu_busy_percentage"))?.toInt(),
            cpuTempC = hottest(cpuZones),
            gpuTempC = hottest(gpuZones),
            fanRpm = if (fanAvailable) fanController.rpm() else null,
            fanFraction = if (fanAvailable) fanController.dutyFraction() else null,
            batteryTempC = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeIf { it != Int.MIN_VALUE }
                ?.let { it / 10f },
            batteryLevel = battery?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) level * 100 / scale else null
            }
        )
    }

    private fun hottest(zones: List<File>): Float? =
        zones.mapNotNull { readLong(it) }.maxOrNull()?.let { millidegrees -> millidegrees / 1000f }

    private fun readLong(file: File): Long? =
        runCatching { file.readText().trim().filter { it.isDigit() }.toLong() }.getOrNull()

    private companion object {
        const val CPUFREQ_DIR = "/sys/devices/system/cpu/cpufreq"
        const val THERMAL_DIR = "/sys/class/thermal"
        const val KGSL_DIR = "/sys/class/kgsl/kgsl-3d0"
        val CPU_ZONE = Regex("^(cpu|cpuss)[-_].*")
        val GPU_ZONE = Regex("^(gpu|gpuss)[-_].*")
    }
}
