package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import com.anytvplayer.ios.data.admin.SubscriptionContact
import com.anytvplayer.ios.data.admin.SubscriptionItem
import com.anytvplayer.ios.ui.theme.TwitiMint

@Composable
fun UserContentSections(navigator: Navigator, libraryCount: Int, watchingCount: Int, subscriptionsCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UserContentSectionCard(
            title = "My Library",
            icon = Icons.Filled.Favorite,
            count = libraryCount,
            emptyText = "No items saved",
            onClick = { navigator.push(UserContentListScreen("favorites")) }
        )

        UserContentSectionCard(
            title = "Continue Watching",
            icon = Icons.Filled.PlayCircle,
            count = watchingCount,
            emptyText = "No watch history",
            onClick = { navigator.push(UserContentListScreen("continue")) }
        )

        UserContentSectionCard(
            title = "My Subscriptions",
            icon = Icons.Filled.Subscriptions,
            count = subscriptionsCount,
            emptyText = "No subscriptions",
            onClick = { navigator.push(UserContentListScreen("subscriptions")) }
        )
    }
}

@Composable
private fun UserContentSectionCard(
    title: String,
    icon: ImageVector,
    count: Int,
    emptyText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (count > 0) count.toString() else emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UserContentCard(
    imageUrl: String,
    title: String,
    subTitle: String?,
    progress: Float? = null,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF20232A))
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = title.take(2).uppercase(),
                    color = TwitiMint,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        if (!subTitle.isNullOrBlank()) {
            Text(
                text = subTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = TwitiMint,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    item: SubscriptionItem,
    onContactClick: (com.anytvplayer.ios.data.admin.SubscriptionContact) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (item.price.isNotBlank()) {
            Text(
                text = "${item.price} ${item.currency}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (item.description.isNotBlank()) {
            Text(
                text = item.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (item.contacts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.contacts.forEach { contact ->
                    IconButton(
                        onClick = { onContactClick(contact) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = platformIcon(contact.platform),
                            contentDescription = contact.label.ifBlank { contact.platform },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun platformIcon(platform: String): ImageVector = when (platform) {
    "whatsapp", "facebook", "message" -> Icons.Default.Message
    "telegram", "twitter" -> Icons.Default.Send
    "instagram" -> Icons.Default.PhotoCamera
    "url", "link", "website" -> Icons.Default.Link
    else -> Icons.Default.Link
}

fun formatDuration(position: Long, duration: Long): String {
    return if (duration > 0) {
        "${formatWatchTime(position)} / ${formatWatchTime(duration)}"
    } else {
        formatWatchTime(position)
    }
}

private fun formatWatchTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
