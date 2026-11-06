package vn.kozu.ui.search

import java.text.Normalizer
import vn.kozu.domain.model.Beatmap

/**
 * Lightweight client-side re-ranking for relevance searches.
 *
 * The mirrors rank mostly by popularity, so an exact "Kiss Me When I'm Dead" can land below
 * partial matches. This pass keeps the server order as the tie-breaker (the sort is stable) but
 * lifts results whose title/artist/mapper actually match what was typed, tolerating accents,
 * punctuation, partially typed words and a one-letter typo.
 */
object SearchRanker {
    private val diacritics = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun rank(query: String, items: List<Beatmap>): List<Beatmap> {
        val tokens = tokenize(query)
        if (tokens.isEmpty() || items.size < 2) return items
        val phrase = tokens.joinToString(" ")
        val scored = items.map { it to score(it, phrase, tokens) }
        if (scored.all { it.second <= 0 }) return items
        return scored.sortedByDescending { it.second }.map { it.first }
    }

    private fun score(map: Beatmap, phrase: String, tokens: List<String>): Int {
        val titles = listOf(map.title, map.titleUnicode).filter { it.isNotBlank() }.map(::normalize)
        val artists = listOf(map.artist, map.artistUnicode).filter { it.isNotBlank() }.map(::normalize)
        val mapper = normalize(map.mapper)
        val tags = normalize(map.tags)
        val titleWords = titles.flatMap { it.split(' ') }.filter { it.isNotEmpty() }
        val artistWords = artists.flatMap { it.split(' ') }.filter { it.isNotEmpty() }
        val mapperWords = mapper.split(' ').filter { it.isNotEmpty() }
        val combined = buildList {
            titles.forEach { t -> add(t); artists.forEach { a -> add("$a $t"); add("$t $a") } }
        }

        var total = 0
        if (titles.any { it == phrase } || combined.any { it == phrase }) total += 100
        else if (titles.any { it.contains(phrase) }) total += 60
        else if (combined.any { it.contains(phrase) }) total += 50
        else if (artists.any { it == phrase }) total += 45
        else if (mapper == phrase) total += 40

        var matched = 0
        for (token in tokens) {
            var s = 0
            if (titleWords.any { it == token }) s = 8
            else if (artistWords.any { it == token }) s = 6
            else if (mapperWords.any { it == token }) s = 4
            else if (titleWords.any { it.startsWith(token) } || artistWords.any { it.startsWith(token) }) s = 5
            else if (tags.contains(token)) s = 2
            else if (token.length >= 4 &&
                (titleWords.any { close(it, token) } || artistWords.any { close(it, token) })
            ) s = 4
            if (s > 0) matched++
            total += s
        }
        if (matched == tokens.size) total += 30
        else if (matched == 0) total -= 10
        return total
    }

    private fun tokenize(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotEmpty() }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(separators, " ")
            .trim()

    /** True when the words differ by at most one edit (and are of similar length). */
    private fun close(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        if (a.length < 4 || b.length < 4) return false
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        if (i == a.length && i == b.length) return true
        return when {
            a.length == b.length -> a.substring(i + 1) == b.substring(i + 1)
            a.length > b.length -> a.substring(i + 1) == b.substring(i)
            else -> a.substring(i) == b.substring(i + 1)
        }
    }
}
