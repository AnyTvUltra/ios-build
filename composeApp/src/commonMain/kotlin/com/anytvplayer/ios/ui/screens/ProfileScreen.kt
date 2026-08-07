package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.SupportTicket
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.IptvViewModel

private enum class ProfileDialog { EDIT_PROFILE, LANGUAGE, PRIVACY, SUPPORT, ABOUT, DELETE, NONE }

object ProfileScreen : Screen {
    override val key = "profile"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        var dialog by remember { mutableStateOf(ProfileDialog.NONE) }
        val active = viewModel.activationState?.isActivated == true
        val activation = viewModel.activationState?.activation
        val brandColor = viewModel.brandColor

        LaunchedEffect(Unit) { viewModel.loadUserContent() }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Text(
                        "My Account",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    AccountHero(
                        name = when {
                            viewModel.userAccount.isLoggedIn -> viewModel.userAccount.name
                            else -> viewModel.profileName
                        }.ifBlank { "User" },
                        profileId = viewModel.profileId,
                        avatarUri = viewModel.profileAvatarUri,
                        active = active,
                        brandColor = brandColor,
                        onEdit = { dialog = ProfileDialog.EDIT_PROFILE }
                    )
                }

                item {
                    SubscriptionCard(
                        active = active,
                        status = activation?.status.orEmpty(),
                        packageName = activation?.packageName.orEmpty(),
                        startedAt = activation?.activatedAt.orEmpty(),
                        expiresAt = activation?.expiresAt.orEmpty()
                    )
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileStat("Live", viewModel.liveChannels.size, Modifier.weight(1f))
                        ProfileStat("Movies", viewModel.vodChannels.size, Modifier.weight(1f))
                        ProfileStat("Series", viewModel.seriesChannels.size, Modifier.weight(1f))
                    }
                }

                item {
                    UserContentSections(
                        navigator = navigator,
                        libraryCount = viewModel.libraryItems.size,
                        watchingCount = viewModel.watchProgressItems.size,
                        subscriptionsCount = viewModel.subscriptions.size
                    )
                }

                item {
                    SectionLabel("Content")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                            title = "Playlists",
                            subtitle = viewModel.currentPlaylist?.name ?: if (active) "Add playlist" else "Requires activation",
                            onClick = { navigator.push(PlaylistPickerScreen) }
                        )
                    }
                }

                item {
                    SectionLabel("Preferences")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            title = "App Language",
                            subtitle = when (viewModel.languageTag) {
                                "ar" -> "العربية"
                                "en" -> "English"
                                "ckb" -> "کوردیی سۆرانی"
                                else -> "العربية"
                            },
                            onClick = { dialog = ProfileDialog.LANGUAGE }
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            subtitle = "Push notifications",
                            trailing = {
                                Switch(
                                    checked = viewModel.notificationsEnabled,
                                    onCheckedChange = viewModel::updateNotificationsEnabled,
                                    colors = streamingSwitchColors()
                                )
                            }
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Outlined.PlayCircle,
                            title = "Auto-Play",
                            subtitle = "Play content automatically",
                            trailing = {
                                Switch(
                                    checked = viewModel.autoplayEnabled,
                                    onCheckedChange = viewModel::updateAutoplayEnabled,
                                    colors = streamingSwitchColors()
                                )
                            }
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = if (viewModel.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            title = "Dark Mode",
                            subtitle = if (viewModel.isDarkTheme) "Enabled" else "Light mode",
                            onClick = { viewModel.toggleDarkTheme(!viewModel.isDarkTheme) },
                            trailing = {
                                Switch(
                                    checked = viewModel.isDarkTheme,
                                    onCheckedChange = { viewModel.toggleDarkTheme(it) },
                                    colors = streamingSwitchColors()
                                )
                            }
                        )
                    }
                }

                item {
                    SectionLabel("Identity")
                    SettingsGroup {
                        val account = viewModel.userAccount
                        if (account.isLoggedIn) {
                            SettingsRow(
                                icon = Icons.Outlined.AccountCircle,
                                title = account.name,
                                subtitle = account.email.ifBlank { "Account via ${account.provider}" },
                                onClick = { navigator.push(LoginScreen) },
                                trailing = {
                                    TextButton(onClick = { viewModel.signOut() }) {
                                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = LiveRed, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sign Out", color = LiveRed)
                                    }
                                }
                            )
                            GroupDivider()
                        } else {
                            SettingsRow(
                                icon = Icons.Outlined.AccountCircle,
                                title = "Sign In",
                                subtitle = "Access your account",
                                onClick = { navigator.push(LoginScreen) }
                            )
                            GroupDivider()
                        }
                        SettingsRow(
                            icon = Icons.Outlined.AccountCircle,
                            title = "Profile ID",
                            subtitle = viewModel.profileId
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Devices,
                            title = "Device ID",
                            subtitle = viewModel.displayDeviceId
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Security,
                            title = "Privacy Policy",
                            subtitle = "View privacy details",
                            onClick = { dialog = ProfileDialog.PRIVACY }
                        )
                        if (viewModel.userAccount.isLoggedIn) {
                            GroupDivider()
                            SettingsRow(
                                icon = Icons.Outlined.Delete,
                                title = "Delete Account",
                                subtitle = "Permanently remove your data",
                                onClick = { dialog = ProfileDialog.DELETE }
                            )
                        }
                    }
                }

                item {
                    SectionLabel("Help")
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Support",
                            subtitle = "Get help or report issues",
                            onClick = { navigator.push(SupportScreen) }
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Info,
                            title = "About",
                            subtitle = "v1.0.0",
                            onClick = { dialog = ProfileDialog.ABOUT }
                        )
                    }
                }
            }

            if (!viewModel.userAccount.isLoggedIn) {
                LoginPrompt(navigator = navigator, brandColor = brandColor)
            }
        }

        when (dialog) {
            ProfileDialog.EDIT_PROFILE -> EditProfileDialog(
                currentName = viewModel.profileName,
                onDismiss = { dialog = ProfileDialog.NONE },
                onSave = { name -> viewModel.updateProfile(name); dialog = ProfileDialog.NONE }
            )
            ProfileDialog.LANGUAGE -> LanguageDialog(
                selected = viewModel.languageTag,
                onDismiss = { dialog = ProfileDialog.NONE },
                onSelect = { tag -> viewModel.setLanguage(tag); dialog = ProfileDialog.NONE }
            )
            ProfileDialog.PRIVACY -> InfoDialog(
                title = "Privacy Policy",
                body = "Your privacy is important to us. We collect minimal data necessary for the application to function. Your personal data is stored securely and is never shared with third parties without your explicit consent.",
                onDismiss = { dialog = ProfileDialog.NONE }
            )
            ProfileDialog.ABOUT -> InfoDialog(
                title = viewModel.brandingConfig.appName,
                body = "AnyTV is an IPTV player application that allows you to watch live TV, movies, and series from your IPTV provider. Version 1.0.0",
                onDismiss = { dialog = ProfileDialog.NONE }
            )
            ProfileDialog.DELETE -> DeleteAccountDialog(
                onConfirm = {
                    dialog = ProfileDialog.NONE
                    viewModel.deleteAccount(onSuccess = { navigator.pop() }, onError = { })
                },
                onDismiss = { dialog = ProfileDialog.NONE }
            )
            ProfileDialog.SUPPORT, ProfileDialog.NONE -> Unit
        }
    }
}

@Composable
private fun LoginPrompt(navigator: cafe.adriel.voyager.navigator.Navigator, brandColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(26.dp)).background(brandColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(48.dp), tint = brandColor)
            }
            Spacer(Modifier.height(22.dp))
            Text("Welcome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("Sign in to access your profile", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { navigator.push(LoginScreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = brandColor, contentColor = Color.White)
            ) { Text("Sign In", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { navigator.push(LoginScreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) { Text("Create Account", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AccountHero(
    name: String,
    profileId: String,
    avatarUri: String,
    active: Boolean,
    brandColor: Color,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(brandColor, TwitiCyan.copy(alpha = 0.78f))))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri.isNotBlank()) {
                AsyncImage(model = avatarUri, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.AccountCircle, null, tint = Color.White, modifier = Modifier.size(54.dp))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(profileId, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodySmall)
            Text(
                if (active) "Active" else "Pending Activation",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 7.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        TextButton(onClick = onEdit) { Text("Edit", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SubscriptionCard(active: Boolean, status: String, packageName: String, startedAt: String, expiresAt: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Active Subscription", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold)
                Text(packageName.ifBlank { "AnyTV" }, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                when {
                    active -> "Active"
                    status.equals("expired", true) -> "Expired"
                    else -> "Inactive"
                },
                color = if (active) TwitiMint else LiveRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background((if (active) TwitiMint else LiveRed).copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Start Date", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(startedAt.toDisplayDate(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }
            Column {
                Text("End Date", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(expiresAt.toDisplayDate(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, count: Int, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TwitiMint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}




@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TwitiMint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun streamingSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = TwitiMint,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
)

@Composable
private fun EditProfileDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun LanguageDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val languages = listOf("ar" to "العربية", "en" to "English", "ckb" to "کوردیی سۆرانی")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                languages.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(tag) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tag == selected, onClick = { onSelect(tag) }, colors = RadioButtonDefaults.colors(selectedColor = TwitiMint))
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = MaterialTheme.colorScheme.onBackground, fontWeight = if (tag == selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, color = MaterialTheme.colorScheme.onSurface) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account", color = LiveRed) },
        text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurface) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = LiveRed, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun String.toDisplayDate(): String {
    if (isBlank() || this == "null") return "—"
    return take(10)
}
