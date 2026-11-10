package vn.kozu.player

import java.util.Locale
import kotlin.random.Random
import vn.kozu.domain.model.Beatmap

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

        return spread(if (fresh.isEmpty()) shuffled else fresh + stale)
    }


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
