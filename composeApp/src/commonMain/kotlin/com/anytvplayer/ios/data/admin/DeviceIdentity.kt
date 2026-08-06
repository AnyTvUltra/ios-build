package com.anytvplayer.ios.data.admin

import com.anytvplayer.ios.data.SecurePreferences
import com.anytvplayer.ios.data.Sha256

/**
 * Manages device identity for the AnyTV device API.
 *
 * iOS has no accessible WiFi MAC, so the vendor identifier is formatted as a
 * MAC address for API compatibility. This mirrors the Android client, which
 * formats ANDROID_ID the same way.
 */
class DeviceIdentity {

    private val securePrefs = SecurePreferences(
        preferencesName = "anytv_device_secure",
        keyAlias = "anytv_device_identity_key"
    )

    /**
     * Returns a stable MAC-formatted device identifier, persisted on first use.
     */
    fun getMacAddress(): String {
        securePrefs.getString(KEY_MAC)?.let { return it }

        val vendorId = PlatformDevice.vendorId().replace("-", "")
        val source = if (vendorId.length >= 12) vendorId else randomHex12()
        val mac = source.take(12).uppercase().chunked(2).joinToString(":")

        securePrefs.putString(KEY_MAC, mac)
        return mac
    }

    /**
     * Returns the device key issued by the backend, or blank before registration.
     */
    fun getDeviceKey(): String = securePrefs.getString(KEY_DEVICE_KEY).orEmpty()

    fun saveDeviceKey(deviceKey: String) {
        if (deviceKey.isNotBlank()) securePrefs.putString(KEY_DEVICE_KEY, deviceKey)
    }

    /**
     * Stable reinstall credential derived from the vendor identifier.
     * Only its SHA-256 digest leaves the device.
     */
    fun getRecoveryToken(): String {
        val vendorId = PlatformDevice.vendorId()
        if (vendorId.isBlank()) return ""
        return Sha256.hexDigest("$vendorId|${PlatformDevice.bundleId()}|anytv-reinstall-v1")
    }

    fun getDeviceName(): String = PlatformDevice.deviceName()

    fun getModel(): String = PlatformDevice.model()

    fun getModelVersion(): String = PlatformDevice.modelVersion()

    fun getAppVersion(): String = PlatformDevice.appVersion()

    fun toRegistration(): DeviceRegistration = DeviceRegistration(
        macAddress = getMacAddress(),
        deviceKey = getDeviceKey(),
        recoveryToken = getRecoveryToken(),
        deviceName = getDeviceName(),
        model = getModel(),
        modelVersion = getModelVersion(),
        appVersion = getAppVersion()
    )

    private fun randomHex12(): String {
        val chars = "0123456789ABCDEF"
        return (1..12).map { chars.random() }.joinToString("")
    }

    private companion object {
        const val KEY_MAC = "device_mac"
        const val KEY_DEVICE_KEY = "device_key"
    }
}
