package com.anytvplayer.ios

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(): platform.UIKit.UIViewController {
    BootTrail.mark("kotlin:mainViewControllerEntered")
    CrashReporter.install()
    BootTrail.mark("kotlin:crashHookInstalled")

    val previousTrail = BootTrail.previous()
    val previousFailed = BootTrail.previousLaunchFailed()
    val previousCrash = CrashReporter.lastCrash()

    BootTrail.mark("kotlin:creatingComposeController")
    return ComposeUIViewController {
        BootTrail.mark("compose:contentEntered")

        var showDiagnostics by remember { mutableStateOf(previousFailed) }

        LaunchedEffect(Unit) {
            BootTrail.mark(BootTrail.COMPLETE_MARKER)
        }

        if (showDiagnostics) {
            CrashScreen(trail = previousTrail, details = previousCrash) {
                CrashReporter.clear()
                BootTrail.clearPrevious()
                showDiagnostics = false
            }
        } else {
            App()
        }
    }
}
