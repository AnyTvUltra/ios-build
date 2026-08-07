package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.ShortClip
import com.anytvplayer.ios.data.admin.ShortComment
import com.anytvplayer.ios.data.admin.ShortSocialState
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.IptvViewModel

object ShortsScreen : Screen {
    override val key = "shorts"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        LaunchedEffect(Unit) {
            if (!viewModel.isConnected) {
                navigator.push(LoginScreen)
            } else {
                viewModel.loadAllContent()
            }
        }

        val clips = viewModel.shortClips
        if (clips.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.SmartDisplay, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(58.dp))
                Spacer(Modifier.size(14.dp))
                Text("No Shorts", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Check back later for short clips", color = MaterialTheme.colorScheme.onSurface)
            }
            return
        }

        val pagerState = rememberPagerState(pageCount = { clips.size })

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            beyondViewportPageCount = 1
        ) { page ->
            ShortVideoPage(
                clip = clips[page],
                isActive = pagerState.currentPage == page,
                viewModel = viewModel,
                onPlay = { navigator.push(PlayerScreen(clips[page].videoUrl)) }
            )
        }
    }
}

@Composable
private fun ShortVideoPage(
    clip: ShortClip,
    isActive: Boolean,
    viewModel: IptvViewModel,
    onPlay: () -> Unit
) {
    val social = viewModel.shortSocialStates[clip.id] ?: ShortSocialState()
    var commentsOpen by remember(clip.id) { mutableStateOf(false) }

    LaunchedEffect(clip.id) { viewModel.loadShortSocial(clip.id) }
    LaunchedEffect(isActive) { if (isActive) viewModel.recordShortView(clip.id) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onPlay)
    ) {
        // Video thumbnail / background
        if (clip.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = clip.thumbnailUrl,
                contentDescription = clip.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Play button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(68.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp))
        }

        // Creator info & title (bottom-left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 84.dp, bottom = 112.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    imageUrl = clip.creatorAvatarUrl,
                    displayName = clip.creator.ifBlank { "AnyTV" },
                    size = 40
                )
                Text(
                    "@${clip.creator.ifBlank { "AnyTV" }}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                clip.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (clip.description.isNotBlank()) {
                Text(
                    clip.description,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                "${compactCount(clip.views + social.viewsCount)} views",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Social actions (right side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 112.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShortAction(
                icon = {
                    Icon(
                        if (social.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint = if (social.liked) Color(0xFFFF3158) else Color.White
                    )
                },
                label = compactCount(clip.likes + social.likesCount),
                onClick = { viewModel.toggleShortLike(clip.id) }
            )
            ShortAction(
                icon = { Icon(Icons.Filled.ChatBubble, null, tint = Color.White) },
                label = compactCount(social.commentsCount),
                onClick = { commentsOpen = true }
            )
            ShortAction(
                icon = {
                    Icon(
                        if (social.saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        null,
                        tint = if (social.saved) TwitiCyan else Color.White
                    )
                },
                label = compactCount(social.savesCount),
                onClick = { viewModel.toggleShortSave(clip.id) }
            )
            ShortAction(
                icon = { Icon(Icons.Filled.Share, null, tint = Color.White) },
                label = "Share",
                onClick = { /* Platform share not yet implemented */ }
            )
        }
    }

    if (commentsOpen) {
        CommentsSheet(
            comments = social.comments,
            currentUserName = viewModel.profileName.ifBlank { "User" },
            currentUserAvatar = viewModel.profileAvatarUri,
            onDismiss = { commentsOpen = false },
            onSend = { text, parentId -> viewModel.addShortComment(clip.id, text, parentId) },
            onEdit = { commentId, text -> viewModel.editShortComment(clip.id, commentId, text) },
            onDelete = { commentId -> viewModel.deleteShortComment(clip.id, commentId) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(
    comments: List<ShortComment>,
    currentUserName: String,
    currentUserAvatar: String,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ShortComment?>(null) }
    var editing by remember { mutableStateOf<ShortComment?>(null) }
    val arrangedComments = remember(comments) { arrangeComments(comments) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF15171C),
        contentColor = Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(bottom = 12.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(TwitiMint.copy(alpha = 0.45f), TwitiCyan.copy(alpha = 0.45f))))
                    .align(Alignment.CenterHorizontally)
            )
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(TwitiMint.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(comments.size.toString(), color = TwitiMint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            if (comments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.ChatBubble, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No comments yet", color = Color.White.copy(alpha = 0.6f))
                    Text("Be the first to comment!", color = TwitiMint, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(arrangedComments, key = { it.id }) { comment ->
                        val isReply = comment.parentId.isNotBlank()
                        CommentItem(
                            comment = comment,
                            isReply = isReply,
                            onReply = { replyTo = comment; text = "" },
                            onEdit = { editing = comment; text = comment.body },
                            onDelete = { onDelete(comment.id) }
                        )
                    }
                }
            }

            // Reply/edit indicator
            if (replyTo != null || editing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (editing != null) "Editing..." else "Replying to @${replyTo?.displayName}",
                        color = TwitiMint, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { replyTo = null; editing = null; text = "" }) {
                        Icon(Icons.Filled.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Input
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(imageUrl = currentUserAvatar, displayName = currentUserName, size = 36)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Add a comment...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TwitiMint,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = TwitiMint
                    ),
                    shape = RoundedCornerShape(50)
                )
                IconButton(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotBlank()) {
                            if (editing != null) {
                                onEdit(editing!!.id, trimmed)
                                editing = null
                            } else {
                                onSend(trimmed, replyTo?.id.orEmpty())
                                replyTo = null
                            }
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(
                            if (text.isNotBlank()) Brush.horizontalGradient(listOf(TwitiMint, TwitiCyan))
                            else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: ShortComment,
    isReply: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 32.dp else 0.dp, top = 8.dp, bottom = 8.dp)
    ) {
        ProfileAvatar(imageUrl = comment.avatarUrl, displayName = comment.displayName, size = if (isReply) 28 else 36)
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.displayName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                if (comment.createdAt.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(comment.createdAt.take(10), color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(comment.body, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(top = 2.dp))
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reply", color = TwitiMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onReply))
                if (comment.isMine) {
                    Text("Edit", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onEdit))
                    Text("Delete", color = Color(0xFFFF3158), style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
        }
    }
}

@Composable
fun ProfileAvatar(imageUrl: String, displayName: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(TwitiMint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(model = imageUrl, contentDescription = displayName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                displayName.take(1).uppercase(),
                color = TwitiMint,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ShortAction(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

private fun compactCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}

private fun arrangeComments(comments: List<ShortComment>): List<ShortComment> {
    val topLevel = comments.filter { it.parentId.isBlank() }
    val replies = comments.filter { it.parentId.isNotBlank() }.groupBy { it.parentId }
    return topLevel.flatMap { parent -> listOf(parent) + (replies[parent.id] ?: emptyList()) }
}
