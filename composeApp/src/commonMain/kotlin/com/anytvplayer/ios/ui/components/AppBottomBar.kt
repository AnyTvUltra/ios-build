package com.anytvplayer.ios.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.anytvplayer.ios.ui.screens.*

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable (Boolean) -> Unit
)

@Composable
fun AppBottomBar(navigator: Navigator, currentRoute: String?) {
    val bottomNavItems = listOf(
        BottomNavItem("home", "Home") { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = "Home"
            )
        },
        BottomNavItem("live", "Live") { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
                contentDescription = "Live"
            )
        },
        BottomNavItem("shorts", "Twiti") { _ ->
            Icon(
                imageVector = Icons.Filled.SmartDisplay,
                contentDescription = "Twiti",
                modifier = Modifier.size(26.dp)
            )
        },
        BottomNavItem("vod", "VOD") { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                contentDescription = "VOD"
            )
        },
        BottomNavItem("profile", "Profile") { selected ->
            Icon(
                imageVector = if (selected) Icons.Filled.Person else Icons.Outlined.Person,
                contentDescription = "Profile"
            )
        }
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        when (item.route) {
                            "home" -> navigator.replaceAll(HomeScreen())
                            "live" -> navigator.push(LiveHubScreen())
                            "shorts" -> navigator.push(ShortsScreen())
                            "vod" -> navigator.push(VodHubScreen())
                            "profile" -> navigator.push(ProfileScreen())
                        }
                    }
                },
                icon = { item.icon(selected) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
