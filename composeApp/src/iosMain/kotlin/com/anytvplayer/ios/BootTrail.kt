package com.anytvplayer.ios

import platform.Foundation.NSUserDefaults

/**
 * Records how far the app got during startup. The trail from the previous launch is
 * preserved so a hard crash (signal, dyld, runtime init) can still be diagnosed on
 * the next launch, where an exception hook would never have run.
 */
object BootTrail {

    private const val CURRENT_KEY = "anytv.boot_trail"
    private const val PREVIOUS_KEY = "anytv.prev_boot_trail"

    const val COMPLETE_MARKER = "compose:firstFrame"

    fun mark(stage: String) {
        val defaults = NSUserDefaults.standardUserDefaults
        val current = defaults.stringForKey(CURRENT_KEY) ?: ""
        val updated = if (current.isEmpty()) stage else "$current\n$stage"
        defaults.setObject(updated, CURRENT_KEY)
        defaults.synchronize()
    }

    fun previous(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(PREVIOUS_KEY)?.takeIf { it.isNotBlank() }

    /** True when the previous launch never reached the first rendered frame. */
    fun previousLaunchFailed(): Boolean {
        val previous = previous() ?: return false
        return !previous.contains(COMPLETE_MARKER)
    }

    fun clearPrevious() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(PREVIOUS_KEY)
        defaults.synchronize()
    }
}
