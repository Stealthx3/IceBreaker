package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformManifest as Manifest
import com.nendo.argosy.platform.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import com.nendo.argosy.platform.CONNECTIVITY_SERVICE
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformPackageManagerConstants as PackageManager
import com.nendo.argosy.platform.PlatformConnectivityManager as ConnectivityManager
import com.nendo.argosy.platform.activeNetwork
import com.nendo.argosy.platform.getNetworkCapabilities
import com.nendo.argosy.platform.registerDefaultNetworkCallback
import com.nendo.argosy.platform.unregisterNetworkCallback
import com.nendo.argosy.platform.hasCapability
import com.nendo.argosy.platform.PlatformNetworkCapabilities as NetworkCapabilities
import android.net.wifi.WifiManager
import com.nendo.argosy.platform.PlatformBuild as Build
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
import com.nendo.argosy.platform.PlatformInputDevices
import com.nendo.argosy.platform.PlatformContextCompat as ContextCompat
import com.nendo.argosy.platform.displayId
import com.nendo.argosy.util.RootShell
import com.nendo.argosy.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Wi-Fi and Bluetooth state for every device, and switching them through the root route where one
 * exists. Android refuses both switches to ordinary apps, so [canToggle] is false without it.
 * Switching and every [wifi] call block on a shell call.
 */
@Singleton
actual class RadioController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager =
        context.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    actual val wifi: WifiNetworks = WifiNetworks { script -> RootShell.run(context, script) }

    actual val canToggle: Boolean
        get() = RootShell.isAvailable

    actual fun status(): RadioStatus = RadioStatus(
        wifiEnabled = wifiManager?.isWifiEnabled == true,
        wifiConnected = isWifiConnected(),
        bluetoothEnabled = isBluetoothEnabled()
    )

    actual fun setWifiEnabled(enabled: Boolean): Boolean {
        val cmdState = if (enabled) "enabled" else "disabled"
        val svcState = if (enabled) "enable" else "disable"
        return runRoot("cmd wifi set-wifi-enabled $cmdState || svc wifi $svcState")
    }

    actual fun setBluetoothEnabled(enabled: Boolean): Boolean {
        val state = if (enabled) "enable" else "disable"
        return runRoot("cmd bluetooth_manager $state || svc bluetooth $state")
    }

    actual fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    actual fun pairedDevices(): List<PairedBluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return try {
            val bonded = bluetoothManager?.adapter?.bondedDevices.orEmpty()
            val inputs = externalInputDevices()
            bonded.map { device ->
                val name = device.name?.takeIf { it.isNotBlank() } ?: device.address
                val input = inputs.firstOrNull { it.name == name }
                PairedBluetoothDevice(
                    address = device.address,
                    name = name,
                    connected = isDeviceConnected(device) ?: (input != null),
                    kind = kindOf(device, input),
                    batteryPercent = input?.let(::batteryPercent)
                )
            }.sortedWith(compareByDescending<PairedBluetoothDevice> { it.connected }.thenBy { it.name.lowercase() })
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun runRoot(script: String): Boolean = RootShell.run(context, script)?.succeeded == true

    private fun isWifiConnected(): Boolean = try {
        val manager = connectivityManager ?: return false
        manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } catch (_: SecurityException) {
        false
    }

    private fun isBluetoothEnabled(): Boolean = try {
        bluetoothManager?.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    private fun externalInputDevices(): List<InputDevice> = PlatformInputDevices.getDeviceIds().toList()
        .mapNotNull { PlatformInputDevices.getDevice(it) }
        .filter { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || it.isExternal }

    @SuppressLint("DiscouragedPrivateApi")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean? = try {
        BluetoothDevice::class.java.getMethod("isConnected").invoke(device) as? Boolean
    } catch (_: Exception) {
        null
    }

    @SuppressLint("MissingPermission")
    private fun kindOf(device: BluetoothDevice, input: InputDevice?): BluetoothDeviceKind {
        if (input != null && input.isGamepad()) return BluetoothDeviceKind.CONTROLLER
        val deviceClass = try {
            device.bluetoothClass
        } catch (_: SecurityException) {
            null
        } ?: return BluetoothDeviceKind.OTHER
        return when (deviceClass.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> BluetoothDeviceKind.AUDIO
            BluetoothClass.Device.Major.PERIPHERAL ->
                if ((deviceClass.deviceClass and PERIPHERAL_MINOR_MASK) in GAME_PERIPHERAL_MINORS) {
                    BluetoothDeviceKind.CONTROLLER
                } else {
                    BluetoothDeviceKind.OTHER
                }
            else -> BluetoothDeviceKind.OTHER
        }
    }

    private fun InputDevice.isGamepad(): Boolean =
        supportsSource(SOURCE_GAMEPAD) || supportsSource(SOURCE_JOYSTICK)

    private fun batteryPercent(input: InputDevice): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val battery = input.batteryState
        if (!battery.isPresent) return null
        val capacity = battery.capacity
        if (capacity.isNaN() || capacity < 0f) return null
        return (capacity * PERCENT).roundToInt().coerceIn(0, PERCENT.toInt())
    }

    private companion object {
        const val PERIPHERAL_MINOR_MASK = 0x3C
        val GAME_PERIPHERAL_MINORS = setOf(0x04, 0x08)
        const val PERCENT = 100f
    }
}
