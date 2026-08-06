package com.anytvplayer.ios.data.admin

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual object PlatformDevice {

    actual fun vendorId(): String =
        UIDevice.currentDevice.identifierForVendor?.UUIDString.orEmpty()

    actual fun deviceName(): String {
        val name = UIDevice.currentDevice.name
        return name.ifBlank { UIDevice.currentDevice.model }
    }

    actual fun model(): String = "iOS"

    actual fun modelVersion(): String = UIDevice.currentDevice.systemVersion

    actual fun appVersion(): String {
        val info = NSBundle.mainBundle.infoDictionary
        val short = info?.get("CFBundleShortVersionString") as? String
        return short?.takeIf { it.isNotBlank() } ?: "1.0"
    }

    actual fun bundleId(): String =
        NSBundle.mainBundle.bundleIdentifier ?: "com.anytvplayer.ios"
}
