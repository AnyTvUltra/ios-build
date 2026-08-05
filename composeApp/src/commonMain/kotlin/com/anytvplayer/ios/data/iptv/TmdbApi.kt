package com.anytvplayer.ios.data.iptv

import com.anytvplayer.ios.data.network.appHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.math.abs
import kotlin.math.roundToInt

object TmdbApi {

    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
    private const val BEARER_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1YWFhOWYzYWJjYWI4MmQ0NTdhNTRmY2EyNTI2OWMxOCIsIm5iZiI6MTc4MDI3MDAyNS4yNjMsInN1YiI6IjZhMWNjM2M5OTE5ZjM5NGIxYmRkMDExYiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.8TugTWprIbPxQQhWg4orJxu-pKgqCgzHqdh2SZ2QPwg"

    suspend fun fetchMovie(channel: IptvChannel): TmdbMovie? {
        val query = cleanTitle(channel.name)
        if (query.isBlank()) return null
        val year = channel.year.takeIf { it.length == 4 } ?: ""
        val movieId = searchMovie(query, year) ?: return null
        return fetchMovieDetails(movieId)
    }

    private suspend fun searchMovie(query: String, year: String): Int? {
        val urlAr = "$BASE_URL/search/movie?query=${query.encodeURLQueryComponent()}&language=ar&page=1" +
                if (year.isNotBlank()) "&year=$year" else ""
        val responseAr = getJson(urlAr)

        val urlEn = "$BASE_URL/search/movie?query=${query.encodeURLQueryComponent()}&language=en-US&page=1" +
                if (year.isNotBlank()) "&year=$year" else ""

        val results = responseAr?.getSearchResults()
            ?: getJson(urlEn)?.getSearchResults()
            ?: return null

        if (results.isEmpty()) return null
        var bestIndex = 0
        var bestScore = -1
        results.forEachIndexed { index, item ->
            val title = item.getString("title")
            val itemYear = item.getString("release_date").take(4)
            var score = 0
            if (title.isNotBlank()) score += 1
            if (year.isNotBlank() && itemYear == year) score += 10
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        val result = results.getOrNull(bestIndex) ?: return null
        return result.getInt("id").takeIf { it > 0 }
    }

    private suspend fun fetchMovieDetails(movieId: Int): TmdbMovie? {
        val detailsUrl = "$BASE_URL/movie/$movieId?append_to_response=credits&language=ar"
        val response = getJson(detailsUrl)
            ?: getJson("$BASE_URL/movie/$movieId?append_to_response=credits&language=en-US")
            ?: return null

        val title = response.getString("title")
        val overview = response.getString("overview")
        val posterPath = response.getString("poster_path")
        val backdropPath = response.getString("backdrop_path")
        val releaseDate = response.getString("release_date")
        val voteAverage = response["vote_average"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val runtimeMinutes = response.getInt("runtime")

        val genresArray = response["genres"]?.jsonArray
        val genres = buildString {
            genresArray?.forEach { element ->
                val genre = element.jsonObject.getString("name")
                if (genre.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(genre)
                }
            }
        }

        val credits = response["credits"]?.jsonObject
        val castArray = credits?.get("cast")?.jsonArray
        val cast = buildString {
            val end = minOf(5, castArray?.size ?: 0)
            for (i in 0 until end) {
                val name = castArray?.get(i)?.jsonObject?.getString("name")
                if (!name.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(name)
                }
            }
        }

        val crewArray = credits?.get("crew")?.jsonArray
        val director = buildString {
            crewArray?.forEach { element ->
                val member = element.jsonObject
                if (member.getString("job") == "Director") {
                    val name = member.getString("name")
                    if (name.isNotBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(name)
                    }
                }
            }
        }

        return TmdbMovie(
            title = title,
            overview = overview,
            posterUrl = posterPath.toTmdbImageUrl("w500"),
            backdropUrl = backdropPath.toTmdbImageUrl("w1280"),
            year = releaseDate.take(4),
            rating = if (voteAverage > 0) formatVoteAverage(voteAverage) else "",
            runtime = formatRuntime(runtimeMinutes),
            cast = cast,
            director = director,
            genres = genres
        )
    }

    private suspend fun getJson(url: String): JsonObject? {
        return try {
            val response = appHttpClient.get(url) {
                header("Authorization", BEARER_TOKEN)
            }
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            if (body.isBlank()) return null
            jsonParser.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getSearchResults(): List<JsonObject>? {
        return this["results"]?.jsonArray?.map { it.jsonObject }?.takeIf { it.isNotEmpty() }
    }

    private fun cleanTitle(name: String): String {
        return name
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("\\s*\\[.*?\\]"), "")
            .replace(Regex("\\s*[-–—].*"), "")
            .replace(Regex("\\|.*"), "")
            .replace(Regex("\\d{4}"), "")
            .trim()
    }

    private fun String.toTmdbImageUrl(size: String): String {
        return if (this.isNotBlank()) "$IMAGE_BASE_URL/$size$this" else ""
    }

    private fun formatRuntime(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
}

private fun formatVoteAverage(value: Double): String {
    val scaled = (value * 10).roundToInt()
    val whole = scaled / 10
    val frac = kotlin.math.abs(scaled % 10)
    return if (frac == 0) "$whole" else "$whole.$frac"
}

private fun JsonObject.getString(key: String, default: String = ""): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: default
}

private fun JsonObject.getInt(key: String, default: Int = 0): Int {
    return this[key]?.jsonPrimitive?.intOrNull ?: default
}
