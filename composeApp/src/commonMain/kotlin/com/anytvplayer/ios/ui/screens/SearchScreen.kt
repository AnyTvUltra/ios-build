package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.viewmodel.gradientForType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce

object SearchScreen : Screen {
    override val key = "search"

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        var query by remember { mutableStateOf(viewModel.searchQuery) }
        val searchFlow = remember { MutableStateFlow("") }

        LaunchedEffect(query) {
            searchFlow.value = query
        }

        LaunchedEffect(searchFlow) {
            searchFlow
                .debounce(400)
                .collect { q ->
                    if (q.isNotBlank()) {
                        viewModel.search(q)
                    } else {
                        viewModel.searchResults = emptyList()
                    }
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search (${viewModel.searchResults.size})") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search channels, movies, series...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") }
                )

                Spacer(Modifier.height(12.dp))

                if (viewModel.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }

                if (!viewModel.isLoading && query.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Start typing to search your content.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!viewModel.isLoading && query.isNotBlank() && viewModel.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(viewModel.searchResults, key = { it.id }) { channel ->
                            ContentCard(
                                title = channel.name,
                                subtitle = channel.categoryName,
                                imageUrl = channel.coverUrl.ifBlank { channel.streamIcon },
                                gradient = channel.gradientForType(),
                                onClick = { openChannel(navigator, viewModel, channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}
