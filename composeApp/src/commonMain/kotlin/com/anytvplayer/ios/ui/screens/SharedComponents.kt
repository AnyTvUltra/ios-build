package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.IptvViewModel
import com.anytvplayer.ios.viewmodel.gradientForType

@Composable
internal fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all")
            }
        }
    }
}

@Composable
internal fun ChannelRow(
    channels: List<IptvChannel>,
    viewModel: IptvViewModel,
    onClick: (IptvChannel) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(channels) { channel ->
            ContentCard(
                title = channel.name,
                subtitle = channel.categoryName,
                imageUrl = channel.coverUrl.ifBlank { channel.streamIcon },
                gradient = channel.gradientForType(),
                onClick = { onClick(channel) }
            )
        }
    }
}

@Composable
fun ContentCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    gradient: List<androidx.compose.ui.graphics.Color> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(140.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (gradient.isNotEmpty()) Brush.linearGradient(gradient)
                else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
