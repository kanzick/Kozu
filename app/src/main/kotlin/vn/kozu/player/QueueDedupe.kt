package vn.kozu.player

import vn.kozu.domain.model.Beatmap

object QueueDedupe {
    fun dedupe(
        queue: List<Beatmap>,
        keepIndex: Int,
    ): Pair<List<Beatmap>, Int> {
        if (queue.isEmpty()) return queue to 0
        val keep = keepIndex.coerceIn(0, queue.lastIndex)
        val keptId = queue[keep].id
        val seen = HashSet<Long>()
        val result = ArrayList<Beatmap>(queue.size)
        var keptPosition = 0
        queue.forEachIndexed { index, beatmap ->
            if (index == keep) {
                keptPosition = result.size
                seen.add(beatmap.id)
                result.add(beatmap)
            } else if (beatmap.id != keptId && seen.add(beatmap.id)) {
                result.add(beatmap)
            }
        }
        return result to keptPosition
    }

    fun distinct(queue: List<Beatmap>): List<Beatmap> = dedupe(queue, 0).first
}
