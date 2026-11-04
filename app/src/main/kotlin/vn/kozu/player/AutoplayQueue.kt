package vn.kozu.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.History
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.domain.model.Beatmap
import java.util.Locale

/**
 * Picks songs to append when the queue runs out.
 *
 * Order of preference:
 *  1. Online "related" songs (same artist, same mapper, trending, new releases), fetched in parallel
 *     so one slow or failing source does not hold up the others, with a per-source cap so the queue
 *     is varied instead of ten songs by the same artist.
 *  2. If the network gave (almost) nothing: the listener's own library, i.e. favourites, downloads
 *     and most played songs. These also work offline.
 *  3. [replay] as the very last resort, to loop what was already played instead of going silent.
 */
class AutoplayQueue(
    private val catalog: BeatmapRepository,
    private val history: HistoryStore,
    private val favorites: FavoritesStore? = null,
    private val downloads: DownloadsStore? = null,
) {
    suspend fun fetch(
        current: Beatmap,
        queue: List<Beatmap>,
        avoidRepeats: Boolean,
        limit: Int = DEFAULT_LIMIT,
    ): List<Beatmap> =
        withContext(Dispatchers.IO) {
            val queueIds = HashSet<Long>()
            val blockedIds = HashSet<Long>()
            val blockedKeys = HashSet<String>()
            val queueKeys = HashSet<String>()
            (queue + current).forEach { beatmap ->
                queueIds.add(beatmap.id)
                queueKeys.add(keyOf(beatmap))
                blockedIds.add(beatmap.id)
                blockedKeys.add(keyOf(beatmap))
            }

            val snapshot = safely { history.snapshot() }
            val recentIds =
                snapshot
                    ?.events
                    ?.asReversed()
                    ?.map { it.beatmapId }
                    ?.distinct()
                    ?.take(RECENT_LIMIT)
                    ?.toSet()
                    .orEmpty()
            if (avoidRepeats && snapshot != null) {
                recentIds.forEach { id ->
                    blockedIds.add(id)
                    snapshot.tracks[id]?.let { blockedKeys.add(keyOf(it)) }
                }
            }

            val picked = ArrayList<Beatmap>(limit)

            fun consume(
                items: List<Beatmap>,
                cap: Int,
                requirePlayable: Boolean,
            ) {
                var taken = 0
                for (beatmap in items) {
                    if (picked.size >= limit || taken >= cap) return
                    if (beatmap.id <= 0L || beatmap.id in blockedIds) continue
                    if (requirePlayable && !isPlayable(beatmap)) continue
                    if (!blockedKeys.add(keyOf(beatmap))) continue
                    blockedIds.add(beatmap.id)
                    picked.add(beatmap)
                    taken++
                }
            }

            // 1. Online sources, all at once. A failed source simply contributes nothing.
            val online: List<Pair<Int, List<Beatmap>>> =
                coroutineScope {
                    val requests = ArrayList<Pair<Int, Deferred<List<Beatmap>>>>(4)
                    current.artist.trim().takeIf { it.isNotEmpty() }?.let { artist ->
                        requests.add(ARTIST_CAP to async { safely { catalog.search(query = artist).items }.orEmpty() })
                    }
                    current.mapper.trim().takeIf { it.isNotEmpty() }?.let { mapper ->
                        requests.add(MAPPER_CAP to async { safely { catalog.search(query = mapper).items }.orEmpty() })
                    }
                    requests.add(DISCOVER_CAP to async { safely { catalog.trending().items.shuffled() }.orEmpty() })
                    requests.add(DISCOVER_CAP to async { safely { catalog.newReleases().items.shuffled() }.orEmpty() })
                    requests.map { (cap, deferred) -> cap to deferred.await() }
                }
            // First pass with a per-source cap keeps the queue varied.
            online.forEach { (cap, items) -> consume(items, cap, requirePlayable = true) }
            // Second pass without caps, in case some sources returned little.
            online.forEach { (_, items) -> consume(items, Int.MAX_VALUE, requirePlayable = true) }

            // 2. Own library when the network did not deliver enough.
            if (picked.size < MIN_ONLINE_RESULTS) {
                val pickedIds = picked.mapTo(HashSet()) { it.id }
                val libraryKeys = HashSet<String>(queueKeys).also { keys -> picked.forEach { keys.add(keyOf(it)) } }
                // Songs that were not played recently come first.
                val library = libraryCandidates(snapshot).sortedBy { if (it.id in recentIds) 1 else 0 }
                for (beatmap in library) {
                    if (picked.size >= limit) break
                    // "Avoid repeats" is relaxed for the library fallback; only what is already
                    // queued (or just picked) stays excluded.
                    if (beatmap.id <= 0L || beatmap.id in queueIds || beatmap.id in pickedIds) continue
                    if (!libraryKeys.add(keyOf(beatmap))) continue
                    pickedIds.add(beatmap.id)
                    picked.add(beatmap)
                }
            }

            SmartShuffle.spread(picked)
        }

    /**
     * Songs that were already played in this queue, in a fresh smart-shuffled order. Used when
     * nothing new could be found, so the player loops the queue instead of stopping.
     */
    fun replay(
        current: Beatmap,
        queue: List<Beatmap>,
        limit: Int = DEFAULT_LIMIT,
    ): List<Beatmap> {
        val earlier =
            queue
                .filter { it.id > 0L && it.id != current.id }
                .distinctBy { it.id }
        return SmartShuffle.order(earlier).take(limit)
    }

    private suspend fun libraryCandidates(snapshot: History?): List<Beatmap> {
        val favourites = safely { favorites?.snapshot() }.orEmpty().shuffled()
        val offline = safely { downloads?.sets?.first() }.orEmpty().map { it.beatmap }.shuffled()
        val frequent = snapshot?.mostPlayed.orEmpty().take(MOST_PLAYED_LIMIT).shuffled()
        return (favourites + offline + frequent).distinctBy { it.id }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            null
        }

    private fun isPlayable(beatmap: Beatmap): Boolean =
        beatmap.status.isBlank() || beatmap.status.lowercase(Locale.ROOT) in PLAYABLE_STATUSES

    private fun keyOf(beatmap: Beatmap): String =
        "${beatmap.title.trim().lowercase(Locale.ROOT)}|${beatmap.artist.trim().lowercase(Locale.ROOT)}"

    private companion object {
        const val DEFAULT_LIMIT = 10
        const val RECENT_LIMIT = 60
        const val MOST_PLAYED_LIMIT = 40
        const val MIN_ONLINE_RESULTS = 4
        const val ARTIST_CAP = 4
        const val MAPPER_CAP = 2
        const val DISCOVER_CAP = 3
        val PLAYABLE_STATUSES = setOf("ranked", "loved", "approved")
    }
}
