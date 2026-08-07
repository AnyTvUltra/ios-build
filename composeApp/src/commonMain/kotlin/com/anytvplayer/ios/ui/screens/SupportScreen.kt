package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.SupportTicket
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint

object SupportScreen : Screen {
    override val key = "support"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        val activeTicket = viewModel.supportTickets.firstOrNull { it.status != "closed" }

        LaunchedEffect(Unit) { viewModel.loadSupportTickets() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Support Center", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("We're here to help", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Status banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(TwitiMint.copy(alpha = 0.22f), TwitiCyan.copy(alpha = 0.08f))))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.HeadsetMic, null, tint = TwitiMint, modifier = Modifier.size(28.dp))
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                if (viewModel.canCreateSupportTicket) "How can we help?" else "Ticket in progress",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (viewModel.canCreateSupportTicket) "We typically respond within 24 hours" else "We're working on your request",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Active ticket
            if (activeTicket != null) {
                item {
                    Text("Active Ticket", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                item { SupportTicketCard(activeTicket, highlighted = true) }
            }

            // New ticket form
            if (viewModel.canCreateSupportTicket) {
                item {
                    Text("New Request", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it.take(120) },
                            label = { Text("Subject") },
                            placeholder = { Text("What do you need help with?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = supportFieldColors()
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it.take(4000) },
                            label = { Text("Message") },
                            placeholder = { Text("Describe your issue in detail...") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = supportFieldColors()
                        )
                        Button(
                            enabled = !viewModel.isSupportBusy && subject.isNotBlank() && message.isNotBlank(),
                            onClick = {
                                viewModel.submitSupportTicket(subject, message)
                                subject = ""
                                message = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TwitiMint)
                        ) {
                            if (viewModel.isSupportBusy) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Outlined.Send, null)
                            }
                            Text(
                                if (viewModel.isSupportBusy) "Sending..." else "Send Message",
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFFB020).copy(alpha = 0.09f))
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LockClock, null, tint = Color(0xFFFFB020))
                        Text(
                            "Please wait until your current ticket is resolved before submitting a new one.",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }

            // Result message
            viewModel.supportMessage?.takeIf(String::isNotBlank)?.let { result ->
                item {
                    Text(
                        result,
                        color = if (result.contains("sent", ignoreCase = true) || result.contains("Done", ignoreCase = true)) TwitiMint else Color(0xFFFFB020),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(TwitiMint.copy(alpha = 0.06f)).padding(14.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Ticket history
            if (viewModel.supportTickets.isNotEmpty()) {
                item {
                    Text("History", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                items(viewModel.supportTickets, key = { it.id }) { ticket ->
                    SupportTicketCard(ticket, highlighted = false)
                }
            }
        }
    }
}

@Composable
private fun SupportTicketCard(ticket: SupportTicket, highlighted: Boolean) {
    val statusColor = when (ticket.status.lowercase()) {
        "open" -> TwitiMint
        "in_progress", "pending" -> TwitiCyan
        "closed" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when (ticket.status.lowercase()) {
        "open" -> Icons.Outlined.History
        "in_progress", "pending" -> Icons.Outlined.History
        "closed" -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.History
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (highlighted) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
            Text(
                ticket.subject,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Text(
                ticket.status.replaceFirstChar { it.uppercase() },
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (ticket.message.isNotBlank()) {
            Text(
                ticket.message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (ticket.createdAt.isNotBlank()) {
            Text(
                ticket.createdAt.take(10),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun supportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TwitiMint,
    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = TwitiMint
)
