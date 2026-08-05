package com.anytvplayer.ios.data.network

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient

val appHttpClient: HttpClient by lazy { createHttpClient() }
