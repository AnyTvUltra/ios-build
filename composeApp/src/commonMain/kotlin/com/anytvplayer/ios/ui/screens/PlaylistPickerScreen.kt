package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FeaturedPlayList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.PlaylistInfo
import com.anytvplayer.ios.ui.components.GlassCard
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.TwitiMint

object PlaylistPickerScreen : Screen {
    override val key = "playlist_picker"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val playlists = viewModel.availablePlaylists
        val currentPlaylist = viewModel.currentPlaylist
        val isActivated = viewModel.activationState?.isActivated == true
        var showAdd by remember { mutableStateOf(false) }
        var deleteTarget by remember { mutableStateOf<PlaylistInfo?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("${playlists.size} playlists", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { showAdd = true },
                    enabled = isActivated && !viewModel.isPlaylistActionBusy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, null)
                    Text("Add", modifier = Modifier.padding(start = 5.dp))
                }
            }

            if (!isActivated) {
                Text(
                    "Device not activated. Activate to manage playlists.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiveRed.copy(alpha = 0.08f))
                        .padding(12.dp)
                )
            }

            viewModel.playlistActionMessage?.let { message ->
                Text(
                    message,
                    color = if (message.startsWith("Done", ignoreCase = true)) TwitiMint else LiveRed,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }

            if (viewModel.isPlaylistActionBusy) {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TwitiMint, modifier = Modifier.size(24.dp))
                }
            }

            if (playlists.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Outlined.FeaturedPlayList, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(58.dp))
                        Text("No Playlists", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(if (isActivated) "Tap Add to create a playlist" else "Activate your device first", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistPickerItem(
                            playlist = playlist,
                            isSelected = playlist.id == currentPlaylist?.id,
                            onClick = { viewModel.selectPlaylist(playlist) },
                            onDelete = { deleteTarget = playlist }
                        )
                    }
                }
            }
        }

        if (showAdd) {
            AddPlaylistDialog(
                busy = viewModel.isPlaylistActionBusy,
                onDismiss = { showAdd = false },
                onAdd = { name, url ->
                    viewModel.addPlaylist(name, url)
                    showAdd = false
                }
            )
        }

        deleteTarget?.let { playlist ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete Playlist?") },
                text = { Text("Are you sure you want to delete \"${playlist.name}\"?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deletePlaylist(playlist); deleteTarget = null }) {
                        Text("Delete", color = LiveRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistPickerItem(
    playlist: PlaylistInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 16.dp,
        glassColor = if (isSelected) TwitiMint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        borderColor = if (isSelected) TwitiMint.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(
                    if (isSelected) TwitiMint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.FeaturedPlayList, null, tint = if (isSelected) TwitiMint else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    color = if (isSelected) TwitiMint else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (playlist.createdAt.isNotBlank()) "Added ${playlist.createdAt.take(10)}" else "Local playlist",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (playlist.isProtected) {
                Icon(Icons.Outlined.Lock, "Protected", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, "Active", tint = TwitiMint, modifier = Modifier.padding(start = 8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = LiveRed)
            }
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the playlist name and M3U URL.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U Link") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && name.isNotBlank() && url.startsWith("http"), onClick = { onAdd(name, url) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
