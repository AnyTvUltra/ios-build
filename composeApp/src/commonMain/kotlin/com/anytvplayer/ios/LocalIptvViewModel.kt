package com.anytvplayer.ios

import androidx.compose.runtime.compositionLocalOf
import com.anytvplayer.ios.viewmodel.IptvViewModel

val LocalIptvViewModel = compositionLocalOf<IptvViewModel> {
    error("IptvViewModel not provided")
}
