package com.anytvplayer.ios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.anytvplayer.ios.ui.components.AppBottomBar
import com.anytvplayer.ios.ui.screens.SplashScreen
import com.anytvplayer.ios.ui.theme.TwitiTheme
import com.anytvplayer.ios.viewmodel.IptvViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@Composable
fun App() {
    val scope = remember { MainScope() }
    val viewModel = remember(scope) { IptvViewModel(scope) }
    val bottomBarRoutes = listOf("home", "live", "vod", "shorts", "profile")

    TwitiTheme(darkTheme = viewModel.isDarkTheme, isCkb = viewModel.languageTag == "ckb") {
        CompositionLocalProvider(LocalIptvViewModel provides viewModel) {
            Navigator(SplashScreen()) { navigator ->
                val current = navigator.lastItem
                val currentRoute = current?.key ?: ""
                val showBottomBar = viewModel.isConnected && currentRoute in bottomBarRoutes

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SlideTransition(navigator)

                    AnimatedVisibility(
                        visible = showBottomBar,
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(180)
                        ) + fadeIn(tween(140)),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(140)
                        ) + fadeOut(tween(100))
                    ) {
                        AppBottomBar(navigator = navigator, currentRoute = currentRoute)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.tryAutoConnect()
        onDispose {
            viewModel.onCleared()
            scope.cancel()
        }
    }
}
