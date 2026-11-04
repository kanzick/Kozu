package vn.kozu.player

import vn.kozu.domain.model.Beatmap
import java.util.Locale
import kotlin.random.Random

/**
 * Shuffle that feels random to a listener, not just to a random number generator.
 *
 * 1. Every track gets an equal chance (a full shuffle of the whole list, not only a random start).
 * 2. Tracks the listener heard very recently ([deprioritized]) are moved to the back, so a new
 *    shuffle does not open with songs they just played.
 * 3. The same artist is never played back to back when another artist is available nearby.
 */
object SmartShuffle {
    private const val LOOKAHEAD = 24
    private const val SAME_ARTIST_GAP = 2

    fun order(
        items: List<Beatmap>,
        deprioritized: Set<Long> = emptySet(),
        random: Random = Random.Default,
    ): List<Beatmap> {
        if (items.size < 2) return items
        val shuffled = items.shuffled(random)
        if (deprioritized.isEmpty()) return spread(shuffled)

        val (stale, fresh) = shuffled.partition { it.id in deprioritized }
        // When everything is "recent" (tiny library) there is nothing to demote.
        return spread(if (fresh.isEmpty()) shuffled else fresh + stale)
    }

    /** Keeps the order as is, but breaks up runs of the same artist using a small look-ahead window. */
    fun spread(input: List<Beatmap>): List<Beatmap> {
        if (input.size < 3) return input
        val remaining = ArrayList<Beatmap>(input)
        val out = ArrayList<Beatmap>(input.size)
        while (remaining.isNotEmpty()) {
            val window = minOf(LOOKAHEAD, remaining.size)
            var pick = 0
            for (index in 0 until window) {
                if (!clashes(remaining[index], out)) {
                    pick = index
                    break
                }
            }
            out.add(remaining.removeAt(pick))
        }
        return out
    }

    private fun clashes(
        candidate: Beatmap,
        placed: List<Beatmap>,
    ): Boolean {
        val key = artistKey(candidate) ?: return false
        for (back in 1..SAME_ARTIST_GAP) {
            val previous = placed.getOrNull(placed.size - back) ?: return false
            if (artistKey(previous) == key) return true
        }
        return false
    }

    private fun artistKey(beatmap: Beatmap): String? =
        beatmap.artist
            .trim()
            .lowercase(Locale.ROOT)
            .takeIf { it.isNotEmpty() }
}
