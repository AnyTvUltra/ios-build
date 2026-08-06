package com.anytvplayer.ios

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSUserDefaults

private const val CRASH_KEY = "anytv.last_crash"

object CrashReporter {

    @OptIn(ExperimentalNativeApi::class)
    fun install() {
        setUnhandledExceptionHook { throwable ->
            record(throwable)
        }
    }

    fun record(throwable: Throwable) {
        val text = buildString {
            append(throwable::class.simpleName ?: "Throwable")
            append(": ")
            append(throwable.message ?: "no message")
            append("\n\n")
            append(throwable.stackTraceToString())
        }
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(text, CRASH_KEY)
        defaults.synchronize()
    }

    fun lastCrash(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(CRASH_KEY)

    fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(CRASH_KEY)
        defaults.synchronize()
    }
}
