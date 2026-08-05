package com.anytvplayer.ios.data

import androidx.compose.ui.graphics.Color

data class ContentItem(
    val id: Int,
    val title: String,
    val category: String,
    val rating: Float = 0f,
    val year: String = "",
    val duration: String = "",
    val description: String = "",
    val isLive: Boolean = false,
    val progress: Float = 0f,
    val gradientColors: List<Color> = emptyList()
)

data class LiveMatch(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val matchTime: String = "",
    val league: String = "",
    val isLive: Boolean = true,
    val homeColor: Color = Color.White,
    val awayColor: Color = Color.White
)

data class SearchCategory(
    val name: String,
    val gradientColors: List<Color>
)

object SampleData {
    val heroItem = ContentItem(
        id = 0,
        title = "House of the Dragon",
        category = "Fantasy & Drama",
        rating = 8.5f,
        year = "2024",
        duration = "S2",
        description = "The story of the Targaryen civil war, known as the Dance of the Dragons, which tore apart the greatest dynasty in Westeros."
    )

    val liveMatches = listOf(
        LiveMatch(1, "Real Madrid", "Barcelona", 2, 1, "67'", "La Liga",
            homeColor = Color(0xFFFEBE10), awayColor = Color(0xFFA50044)),
        LiveMatch(2, "Man City", "Arsenal", 0, 0, "34'", "Premier League",
            homeColor = Color(0xFF6CABDD), awayColor = Color(0xFFEF0107)),
        LiveMatch(3, "Al Hilal", "Al Nassr", 1, 0, "52'", "SPL",
            homeColor = Color(0xFF1C3F94), awayColor = Color(0xFFFFEB3B)),
        LiveMatch(4, "PSG", "Bayern", 3, 2, "88'", "UCL",
            homeColor = Color(0xFF004170), awayColor = Color(0xFFDC052D)),
    )

    val trendingMovies = listOf(
        ContentItem(1, "Inception", "Sci-Fi", 8.8f, "2010", "2h 28m",
            "A thief who steals corporate secrets through dream-sharing technology.",
            gradientColors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1))),
        ContentItem(2, "The Dark Knight", "Action", 9.0f, "2008", "2h 32m",
            "Batman must accept one of the greatest tests to fight injustice.",
            gradientColors = listOf(Color(0xFF212121), Color(0xFF424242))),
        ContentItem(3, "Interstellar", "Drama", 8.7f, "2014", "2h 49m",
            "A team of explorers travel through a wormhole in space.",
            gradientColors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))),
        ContentItem(4, "Gladiator II", "Action", 7.5f, "2024", "2h 28m",
            "Lucius must fight in the Colosseum after his home is conquered.",
            gradientColors = listOf(Color(0xFF3E2723), Color(0xFF5D4037))),
        ContentItem(5, "Dune: Part Two", "Sci-Fi", 8.6f, "2024", "2h 46m",
            "Paul Atreides unites with the Fremen to fight House Harkonnen.",
            gradientColors = listOf(Color(0xFFBF360C), Color(0xFFE64A19))),
    )

    val topSeries = listOf(
        ContentItem(6, "The Crown", "Drama", 8.3f, "2023", "S6",
            "The reign of Queen Elizabeth II from the 1940s to modern times.",
            gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457))),
        ContentItem(7, "Stranger Things", "Fantasy", 8.7f, "2025", "S5",
            "A group of young friends witness supernatural forces in Hawkins.",
            gradientColors = listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))),
        ContentItem(8, "The Last of Us", "Thriller", 8.8f, "2025", "S2",
            "Joel and Ellie's journey across a post-apocalyptic United States.",
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))),
        ContentItem(9, "Succession", "Drama", 8.9f, "2023", "S4",
            "The Roy family controls one of the biggest media empires.",
            gradientColors = listOf(Color(0xFF1A237E), Color(0xFF283593))),
    )

    val continueWatching = listOf(
        ContentItem(10, "Breaking Bad", "Drama", 9.5f, "2013", "S5 E8",
            progress = 0.65f,
            gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF33691E))),
        ContentItem(11, "The Witcher", "Fantasy", 8.1f, "2023", "S3 E4",
            progress = 0.3f,
            gradientColors = listOf(Color(0xFF37474F), Color(0xFF455A64))),
        ContentItem(12, "Money Heist", "Thriller", 8.2f, "2021", "S5 E2",
            progress = 0.8f,
            gradientColors = listOf(Color(0xFFB71C1C), Color(0xFFC62828))),
    )

    val searchCategories = listOf(
        SearchCategory("Action", listOf(Color(0xFFFF6D00), Color(0xFFFF9100))),
        SearchCategory("Comedy", listOf(Color(0xFFFFD600), Color(0xFFFFC400))),
        SearchCategory("Drama", listOf(Color(0xFF6200EA), Color(0xFF7C4DFF))),
        SearchCategory("Sci-Fi", listOf(Color(0xFF0091EA), Color(0xFF00B0FF))),
        SearchCategory("Horror", listOf(Color(0xFFD50000), Color(0xFFFF1744))),
        SearchCategory("Romance", listOf(Color(0xFFC51162), Color(0xFFFF4081))),
        SearchCategory("Thriller", listOf(Color(0xFF263238), Color(0xFF37474F))),
        SearchCategory("Documentary", listOf(Color(0xFF00695C), Color(0xFF00897B))),
        SearchCategory("Animation", listOf(Color(0xFFAA00FF), Color(0xFFD500F9))),
        SearchCategory("Sports", listOf(Color(0xFF2E7D32), Color(0xFF43A047))),
        SearchCategory("Fantasy", listOf(Color(0xFF1A237E), Color(0xFF3F51B5))),
        SearchCategory("Crime", listOf(Color(0xFF4E342E), Color(0xFF6D4C41))),
    )

    val upcomingMatches = listOf(
        LiveMatch(5, "Liverpool", "Chelsea", matchTime = "Tomorrow 20:00", league = "Premier League", isLive = false,
            homeColor = Color(0xFFC8102E), awayColor = Color(0xFF034694)),
        LiveMatch(6, "Juventus", "AC Milan", matchTime = "Fri 21:45", league = "Serie A", isLive = false,
            homeColor = Color(0xFF000000), awayColor = Color(0xFFAC1B2F)),
        LiveMatch(7, "Al Ahli", "Al Ittihad", matchTime = "Sat 19:30", league = "SPL", isLive = false,
            homeColor = Color(0xFF006633), awayColor = Color(0xFFFFEB3B)),
    )
}
