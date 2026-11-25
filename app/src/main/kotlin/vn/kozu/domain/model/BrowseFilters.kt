package vn.kozu.domain.model

enum class BrowseStatus(
    val label: String,
    val apiValue: String?,
    val requiresLogin: Boolean = false,
) {
    HAS_LEADERBOARD("Has leaderboard", null),
    ANY("Any", "any"),
    RANKED("Ranked", "ranked"),
    QUALIFIED("Qualified", "qualified"),
    LOVED("Loved", "loved"),
    FAVOURITES("Favourites", "favourites", requiresLogin = true),
    PENDING("Pending", "pending"),
    GRAVEYARD("Graveyard", "graveyard"),
    MINE("My maps", "mine", requiresLogin = true),
    ;

    fun toStatusFilter(): StatusFilter? =
        when (this) {
            HAS_LEADERBOARD -> StatusFilter.DEFAULT
            RANKED -> StatusFilter.RANKED
            LOVED -> StatusFilter.LOVED
            ANY -> StatusFilter.ANY
            else -> null
        }

    companion object {
        fun from(filter: StatusFilter): BrowseStatus =
            when (filter) {
                StatusFilter.DEFAULT -> HAS_LEADERBOARD
                StatusFilter.RANKED -> RANKED
                StatusFilter.LOVED -> LOVED
                StatusFilter.ANY -> ANY
            }
    }
}

enum class BrowseGenre(
    val id: Int?,
    val label: String,
) {
    ANY(null, "Any"),
    UNSPECIFIED(1, "Unspecified"),
    VIDEO_GAME(2, "Video game"),
    ANIME(3, "Anime"),
    ROCK(4, "Rock"),
    POP(5, "Pop"),
    OTHER(6, "Other"),
    NOVELTY(7, "Novelty"),
    HIP_HOP(9, "Hip hop"),
    ELECTRONIC(10, "Electronic"),
    METAL(11, "Metal"),
    CLASSICAL(12, "Classical"),
    FOLK(13, "Folk"),
    JAZZ(14, "Jazz"),
    ;

    companion object {
        fun labelFor(id: Int): String? = entries.firstOrNull { it.id == id }?.label
    }
}

enum class BrowseLanguage(
    val id: Int?,
    val label: String,
) {
    ANY(null, "Any"),
    UNSPECIFIED(1, "Unspecified"),
    ENGLISH(2, "English"),
    JAPANESE(3, "Japanese"),
    CHINESE(4, "Chinese"),
    INSTRUMENTAL(5, "Instrumental"),
    KOREAN(6, "Korean"),
    FRENCH(7, "French"),
    GERMAN(8, "German"),
    SWEDISH(9, "Swedish"),
    SPANISH(10, "Spanish"),
    ITALIAN(11, "Italian"),
    RUSSIAN(12, "Russian"),
    POLISH(13, "Polish"),
    OTHER(14, "Other"),
    ;

    companion object {
        fun labelFor(id: Int): String? = entries.firstOrNull { it.id == id }?.label
    }
}

enum class BrowseExtra(
    val apiValue: String,
    val label: String,
) {
    VIDEO("video", "Has video"),
    STORYBOARD("storyboard", "Has storyboard"),
}

enum class BrowseGeneral(
    val apiValue: String,
    val label: String,
    val requiresLogin: Boolean = false,
) {
    RECOMMENDED("recommended", "Recommended difficulty", requiresLogin = true),
    CONVERTS("converts", "Include converted beatmaps"),
    FOLLOWS("follows", "Subscribed mappers", requiresLogin = true),
    SPOTLIGHTS("spotlights", "Spotlight beatmaps"),
    FEATURED_ARTISTS("featured_artists", "Featured artists"),
}

enum class BrowsePlayed(
    val apiValue: String?,
    val label: String,
) {
    ANY(null, "Any"),
    PLAYED("played", "Played"),
    UNPLAYED("unplayed", "Unplayed"),
}

enum class BrowseRank(
    val apiValue: String,
    val label: String,
) {
    XH("XH", "Silver SS"),
    X("X", "SS"),
    SH("SH", "Silver S"),
    S("S", "S"),
    A("A", "A"),
    B("B", "B"),
    C("C", "C"),
    D("D", "D"),
}

enum class BrowseSortField(
    val key: String,
    val label: String,
) {
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    DIFFICULTY("difficulty", "Difficulty"),
    RANKED("ranked", "Ranked date"),
    RATING("rating", "Rating"),
    PLAYS("plays", "Plays"),
    FAVOURITES("favourites", "Favourites"),
    RELEVANCE("relevance", "Relevance"),
    UPDATED("updated", "Updated"),
}

data class BrowseSort(
    val option: BrowseSortField,
    val ascending: Boolean,
) {
    val apiValue: String get() = option.key + if (ascending) "_asc" else "_desc"
    val label: String
        get() = "${option.label}, ${if (ascending) "ascending" else "descending"}"
}

data class ActiveBrowseFilter(
    val label: String,
    val clear: (BrowseFilters) -> BrowseFilters,
)

data class BrowseFilters(
    val query: String = "",
    val mode: ModeFilter = ModeFilter.ANY,
    val status: BrowseStatus = BrowseStatus.HAS_LEADERBOARD,
    val genre: BrowseGenre = BrowseGenre.ANY,
    val language: BrowseLanguage = BrowseLanguage.ANY,
    val extras: Set<BrowseExtra> = emptySet(),
    val general: Set<BrowseGeneral> = emptySet(),
    val played: BrowsePlayed = BrowsePlayed.ANY,
    val ranks: Set<BrowseRank> = emptySet(),
    val sortField: BrowseSortField? = null,
    val ascending: Boolean = false,
    val explicit: Boolean = false,
) {
    val trimmedQuery: String get() = query.trim()

    val sort: BrowseSort
        get() = BrowseSort(sortField ?: defaultSortField(), ascending)

    val modeParam: Int? get() = mode.apiValue
    val statusParam: String? get() = status.apiValue
    val genreParam: Int? get() = genre.id
    val languageParam: Int? get() = language.id
    val extraParam: String? get() = extras.joinParam { it.apiValue }
    val generalParam: String? get() = general.joinParam { it.apiValue }
    val rankParam: String? get() = ranks.joinParam { it.apiValue }
    val playedParam: String? get() = played.apiValue
    val sortParam: String get() = sort.apiValue

    val usesUserScopedOptions: Boolean
        get() =
            status.requiresLogin ||
                general.any { it.requiresLogin } ||
                played != BrowsePlayed.ANY ||
                ranks.isNotEmpty()

    val activeFilters: List<ActiveBrowseFilter>
        get() = activeFilters(BrowseStatus.HAS_LEADERBOARD)

    fun activeFilters(defaultStatus: BrowseStatus): List<ActiveBrowseFilter> =
        buildList {
            if (mode != ModeFilter.ANY) add(ActiveBrowseFilter("Mode: ${mode.label}") { it.copy(mode = ModeFilter.ANY) })
            if (status != defaultStatus) {
                add(ActiveBrowseFilter("Status: ${status.label}") { it.copy(status = defaultStatus) })
            }
            if (genre != BrowseGenre.ANY) add(ActiveBrowseFilter("Genre: ${genre.label}") { it.copy(genre = BrowseGenre.ANY) })
            if (language != BrowseLanguage.ANY) {
                add(ActiveBrowseFilter("Language: ${language.label}") { it.copy(language = BrowseLanguage.ANY) })
            }
            extras.sortedBy { it.ordinal }.forEach { extra ->
                add(ActiveBrowseFilter(extra.label) { it.copy(extras = it.extras - extra) })
            }
            general.sortedBy { it.ordinal }.forEach { option ->
                add(ActiveBrowseFilter(option.label) { it.copy(general = it.general - option) })
            }
            if (played != BrowsePlayed.ANY) add(ActiveBrowseFilter(played.label) { it.copy(played = BrowsePlayed.ANY) })
            ranks.sortedBy { it.ordinal }.forEach { rank ->
                add(ActiveBrowseFilter("Rank: ${rank.label}") { it.copy(ranks = it.ranks - rank) })
            }
            if (explicit) add(ActiveBrowseFilter("Explicit content") { it.copy(explicit = false) })
            if (sortField != null || ascending) {
                add(ActiveBrowseFilter("Sort: ${sort.label}") { it.copy(sortField = null, ascending = false) })
            }
        }

    val hasActiveFilters: Boolean get() = activeFilters.isNotEmpty()

    fun hasActiveFilters(defaultStatus: BrowseStatus): Boolean = activeFilters(defaultStatus).isNotEmpty()

    fun resetFilters(): BrowseFilters = BrowseFilters(query = query)

    fun withNormalizedSort(): BrowseFilters =
        if (sortField != null && sortField == defaultSortField() && !ascending) copy(sortField = null) else this

    fun withoutUserScoped(): BrowseFilters =
        copy(
            status = if (status.requiresLogin) BrowseStatus.HAS_LEADERBOARD else status,
            general = general.filterNot { it.requiresLogin }.toSet(),
            played = BrowsePlayed.ANY,
            ranks = emptySet(),
        )

    private fun defaultSortField(): BrowseSortField =
        when {
            trimmedQuery.isNotEmpty() -> BrowseSortField.RELEVANCE

            status == BrowseStatus.PENDING ||
                status == BrowseStatus.GRAVEYARD ||
                status == BrowseStatus.MINE -> BrowseSortField.UPDATED

            else -> BrowseSortField.RANKED
        }

    private inline fun <T : Enum<T>> Set<T>.joinParam(crossinline value: (T) -> String): String? =
        if (isEmpty()) null else sortedBy { it.ordinal }.joinToString(".") { value(it) }
}
