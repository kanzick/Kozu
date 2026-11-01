package vn.kozu.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.domain.model.Beatmap
import java.util.Locale

class AutoplayQueue(
    private val catalog: BeatmapRepository,
    private val history: HistoryStore
) {

    suspend fun fetch(
        current: Beatmap,
        queue: List<Beatmap>,
        avoidRepeats: Boolean,
        limit: Int = DEFAULT_LIMIT
    ): List<Beatmap> = withContext(Dispatchers.IO) {
        val blockedIds = HashSet<Long>()
        val blockedKeys = HashSet<String>()
        (queue + current).forEach { beatmap ->
            blockedIds.add(beatmap.id)
            blockedKeys.add(keyOf(beatmap))
        }
        if (avoidRepeats) {
            val snapshot = try {
                history.history.first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                null
            }
            snapshot?.events?.asReversed()
                ?.map { it.beatmapId }
                ?.distinct()
                ?.take(RECENT_LIMIT)
                ?.forEach { id ->
                    blockedIds.add(id)
                    snapshot.tracks[id]?.let { blockedKeys.add(keyOf(it)) }
                }
        }

        val picked = ArrayList<Beatmap>(limit)

        fun consume(items: List<Beatmap>) {
            for (beatmap in items) {
                if (picked.size >= limit) return
                if (beatmap.id <= 0L || !isPlayable(beatmap) || beatmap.id in blockedIds) continue
                if (!blockedKeys.add(keyOf(beatmap))) continue
                blockedIds.add(beatmap.id)
                picked.add(beatmap)
            }
        }

        val sources: List<suspend () -> List<Beatmap>> = buildList {
            current.artist.trim().takeIf { it.isNotEmpty() }?.let { artist ->
                add { catalog.search(query = artist).items }
            }
            current.mapper.trim().takeIf { it.isNotEmpty() }?.let { mapper ->
                add { catalog.search(query = mapper).items }
            }
            add { catalog.trending().items.shuffled() }
            add { catalog.newReleases().items.shuffled() }
        }

        for (source in sources) {
            if (picked.size >= limit) break
            val items = try {
                source()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                emptyList()
            }
            consume(items)
        }
        picked
    }

    private fun isPlayable(beatmap: Beatmap): Boolean =
        beatmap.status.isBlank() || beatmap.status.lowercase(Locale.ROOT) in PLAYABLE_STATUSES

    private fun keyOf(beatmap: Beatmap): String =
        "${beatmap.title.trim().lowercase(Locale.ROOT)}|${beatmap.artist.trim().lowercase(Locale.ROOT)}"

    private companion object {
        const val DEFAULT_LIMIT = 10
        const val RECENT_LIMIT = 60
        val PLAYABLE_STATUSES = setOf("ranked", "loved", "approved")
    }
}
