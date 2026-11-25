package vn.kozu.data.lazer

enum class LazerStatusGroup {
    RANKED,
    LOVED,
    PENDING,
    GRAVEYARD,
    LOCAL,
    ;

    companion object {
        fun of(status: Int): LazerStatusGroup =
            when (status) {
                1, 2, 3 -> RANKED
                4 -> LOVED
                0, -1 -> PENDING
                -2 -> GRAVEYARD
                else -> LOCAL
            }
    }
}

enum class LazerOrigin { ALL, ONLINE, LOCAL }

enum class LazerLibraryScope { ALL, NEW, OWNED }

enum class LazerSort { ARTIST, TITLE, ADDED, STARS }

data class LazerLibraryFilter(
    val query: String = "",
    val modes: Set<Int> = emptySet(),
    val statuses: Set<LazerStatusGroup> = emptySet(),
    val origin: LazerOrigin = LazerOrigin.ALL,
    val scope: LazerLibraryScope = LazerLibraryScope.ALL,
    val collection: Int = -1,
    val minStars: Int = 0,
    val sort: LazerSort = LazerSort.ARTIST,
    val descending: Boolean = false,
) {
    val activeCount: Int
        get() =
            listOf(
                modes.isNotEmpty(),
                statuses.isNotEmpty(),
                origin != LazerOrigin.ALL,
                scope != LazerLibraryScope.ALL,
                collection >= 0,
                minStars > 0,
            ).count { it }

    fun apply(
        sets: List<LazerLibrarySet>,
        collections: List<LazerCollection>,
        existing: Set<Long>,
    ): List<LazerLibrarySet> {
        val needle = query.trim().lowercase()
        val members =
            collections.getOrNull(collection)?.sets?.mapTo(HashSet()) { it.key }
        val filtered =
            sets.filter { set ->
                (members == null || set.key in members) &&
                    (
                        needle.isEmpty() ||
                            set.title.lowercase().contains(needle) ||
                            set.titleUnicode.lowercase().contains(needle) ||
                            set.artist.lowercase().contains(needle) ||
                            set.artistUnicode.lowercase().contains(needle) ||
                            set.author.lowercase().contains(needle) ||
                            set.source.lowercase().contains(needle) ||
                            set.tags.lowercase().contains(needle)
                    ) &&
                    (modes.isEmpty() || set.rulesets.any { it in modes }) &&
                    (statuses.isEmpty() || LazerStatusGroup.of(set.status) in statuses) &&
                    when (origin) {
                        LazerOrigin.ALL -> true
                        LazerOrigin.ONLINE -> set.onlineId > 0L
                        LazerOrigin.LOCAL -> set.onlineId <= 0L
                    } &&
                    set.stars >= minStars &&
                    when (scope) {
                        LazerLibraryScope.ALL -> true
                        LazerLibraryScope.NEW -> set.expectedId !in existing
                        LazerLibraryScope.OWNED -> set.expectedId in existing
                    }
            }
        val comparator: Comparator<LazerLibrarySet> =
            when (sort) {
                LazerSort.ARTIST -> compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
                LazerSort.TITLE -> compareBy({ it.title.lowercase() }, { it.artist.lowercase() })
                LazerSort.ADDED -> compareBy { it.addedAt }
                LazerSort.STARS -> compareBy { it.stars }
            }
        return filtered.sortedWith(if (descending) comparator.reversed() else comparator)
    }
}
