package com.anytvplayer.ios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(): platform.UIKit.UIViewController {
    CrashReporter.install()
    return ComposeUIViewController {
        var pendingCrash by remember { mutableStateOf(CrashReporter.lastCrash()) }
        val crash = pendingCrash
        if (crash != null) {
            CrashScreen(details = crash) {
                CrashReporter.clear()
                pendingCrash = null
            }
        } else {
            App()
        }
    }
}
