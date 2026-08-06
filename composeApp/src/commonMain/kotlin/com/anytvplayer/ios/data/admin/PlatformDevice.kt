package com.anytvplayer.ios.data.admin

/**
 * Platform facts required to register the device with the AnyTV backend.
 * The iOS actual is backed by UIDevice and the app bundle.
 */
expect object PlatformDevice {
    /** Stable per-vendor installation identifier. iOS analogue of ANDROID_ID. */
    fun vendorId(): String

    /** Human readable device name, e.g. "iPhone 16 Pro". */
    fun deviceName(): String

    /** Platform identifier reported to the backend. */
    fun model(): String

    /** OS version, e.g. "26.1". */
    fun modelVersion(): String

    /** Short app version from the bundle, e.g. "1.0". */
    fun appVersion(): String

    /** Bundle identifier, used when deriving the recovery token. */
    fun bundleId(): String
}
