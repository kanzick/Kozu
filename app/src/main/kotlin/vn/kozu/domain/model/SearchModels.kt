package vn.kozu.domain.model

enum class SearchSort(val label: String, val apiValue: String?) {
    RELEVANCE("Relevance", null),
    MOST_PLAYED("Most played", "plays_desc"),
    NEWEST("Newest", "ranked_desc")
}


enum class SearchCategory(val title: String, val genreId: Int, val emoji: String) {
    ANIME("Anime", 3, "🌸"),
    VIDEO_GAME("Video Game", 2, "🎮"),
    ROCK("Rock", 4, "🎸"),
    POP("Pop", 5, "🎤"),
    ELECTRONIC("Electronic", 10, "⚡"),
    METAL("Metal", 11, "🤘"),
    HIP_HOP("Hip Hop", 9, "🎧"),
    JAZZ("Jazz", 14, "🎷"),
    CLASSICAL("Classical", 12, "🎻"),
    FOLK("Folk", 13, "🪕")
}
