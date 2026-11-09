package vn.kozu.data.repository

import java.time.LocalDate
import java.time.YearMonth
import java.time.Year
import java.time.ZoneOffset
import java.util.Locale
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BrowseFilters
import vn.kozu.domain.model.BrowseGenre
import vn.kozu.domain.model.BrowseLanguage
import vn.kozu.domain.model.BrowseStatus
import vn.kozu.domain.model.ModeFilter

/**
 * osu!'s own beatmap search syntax: `stars<8 ar>=9 ranked=2025 status=r mapper="Some One" fox`.
 *
 * One parser serves the Search tab and All beatmaps. Keywords that map onto an app filter
 * (status, mode, language, genre) set that filter. The rest are handed to the official API
 * untouched (it understands the full syntax) and, when only the public mirror is available,
 * applied to the results here for the fields the mirror reports.
 */
object OsuSearchSyntax {
    enum class Op(val symbol: String) {
        EQ("="),
        NE("!="),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
    }

    data class Condition(
        val key: String,
        val op: Op,
        val value: String,
        val raw: String,
    )

    data class Parsed(
        /** The free text left once every recognised keyword is taken out. */
        val text: String,
        val conditions: List<Condition>,
        val statuses: Set<String>,
        val mode: ModeFilter?,
        val language: BrowseLanguage?,
        val genre: BrowseGenre?,
        /** Everything that was recognised, in the order typed, for display. */
        val recognised: List<String>,
    ) {
        val hasSyntax: Boolean get() = recognised.isNotEmpty()

        /** True when the public mirror needs help from the client to honour the query. */
        val needsClientFilter: Boolean get() = conditions.any { it.key in CLIENT_KEYS } || statuses.size > 1

        /** The query to send to the official API: keywords it understands plus the free text. */
        fun passthroughQuery(): String =
            (conditions.map { it.raw } + text).filter { it.isNotBlank() }.joinToString(" ")

        /** Applies the app-level keywords to [filters] and leaves only the free text as query. */
        fun applyTo(filters: BrowseFilters): BrowseFilters {
            var next = filters.copy(query = text)
            mode?.let { next = next.copy(mode = it) }
            language?.let { next = next.copy(language = it) }
            genre?.let { next = next.copy(genre = it) }
            if (statuses.size == 1) {
                statusOf(statuses.first())?.let { next = next.copy(status = it) }
            } else if (statuses.size > 1) {
                next = next.copy(status = BrowseStatus.ANY)
            }
            return next
        }

        /** Labels of keywords the public mirror cannot evaluate (no data for them). */
        fun unsupportedLabels(): List<String> =
            conditions.filter { it.key in UNSUPPORTED_ON_MIRROR }.map { it.raw }.distinct()

        fun matches(beatmap: Beatmap): Boolean {
            if (statuses.size > 1 && statuses.none { matchesStatus(it, beatmap.status) }) return false
            return conditions.all { matchesCondition(it, beatmap) }
        }
    }

    private val KEYS =
        mapOf(
            "artist" to "artist", "title" to "title",
            "creator" to "creator", "author" to "creator", "mapper" to "creator",
            "diff" to "diff", "difficulty" to "diff",
            "source" to "source", "tag" to "tag", "tags" to "tag",
            "ar" to "ar", "cs" to "cs", "od" to "od", "hp" to "hp", "dr" to "hp",
            "star" to "stars", "stars" to "stars", "sr" to "stars",
            "bpm" to "bpm", "length" to "length", "drain" to "drain",
            "key" to "keys", "keys" to "keys",
            "status" to "status", "mode" to "mode",
            "language" to "language", "lang" to "language", "genre" to "genre",
            "ranked" to "ranked", "created" to "created", "submitted" to "created", "updated" to "updated",
        )

    private val CLIENT_KEYS = setOf("artist", "title", "creator", "source", "tag", "stars", "bpm", "ranked", "created", "updated")
    private val UNSUPPORTED_ON_MIRROR = setOf("ar", "cs", "od", "hp", "length", "drain", "keys", "diff")
    private val tokenRegex = Regex("""^([A-Za-z]+)(==|!=|<=|>=|=|<|>|:)(.*)$""")

    fun parse(query: String): Parsed {
        val text = mutableListOf<String>()
        val conditions = mutableListOf<Condition>()
        val recognised = mutableListOf<String>()
        val statuses = linkedSetOf<String>()
        var mode: ModeFilter? = null
        var language: BrowseLanguage? = null
        var genre: BrowseGenre? = null

        for (token in tokenize(query)) {
            val match = tokenRegex.find(token)
            val key = match?.groupValues?.get(1)?.lowercase(Locale.US)?.let { KEYS[it] }
            val value = match?.groupValues?.get(3)?.trim()?.trim('"').orEmpty()
            if (match == null || key == null || value.isEmpty()) {
                text += token
                continue
            }
            val op = operatorOf(match.groupValues[2])
            when (key) {
                "status" -> {
                    val names = value.split(',').map { it.trim().lowercase(Locale.US) }.filter { it.isNotEmpty() }
                    val normalised = names.mapNotNull(::normaliseStatus)
                    if (normalised.isEmpty()) {
                        text += token
                    } else {
                        statuses += normalised
                        recognised += token
                    }
                }
                "mode" -> {
                    val parsed = modeOf(value)
                    if (parsed == null) text += token else { mode = parsed; recognised += token }
                }
                "language" -> {
                    val parsed = languageOf(value)
                    if (parsed == null) text += token else { language = parsed; recognised += token }
                }
                "genre" -> {
                    val parsed = genreOf(value)
                    if (parsed == null) text += token else { genre = parsed; recognised += token }
                }
                else -> {
                    conditions += Condition(key, op, value, token)
                    recognised += token
                }
            }
        }
        return Parsed(
            text = text.joinToString(" ").trim(),
            conditions = conditions,
            statuses = statuses,
            mode = mode,
            language = language,
            genre = genre,
            recognised = recognised,
        )
    }

    /** Splits on spaces but keeps `key="a b"` together. */
    private fun tokenize(query: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        for (ch in query) {
            when {
                ch == '"' -> {
                    quoted = !quoted
                    current.append(ch)
                }
                ch.isWhitespace() && !quoted -> {
                    if (current.isNotEmpty()) out += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }

    private fun operatorOf(symbol: String): Op =
        when (symbol) {
            "!=" -> Op.NE
            "<=" -> Op.LE
            ">=" -> Op.GE
            "<" -> Op.LT
            ">" -> Op.GT
            else -> Op.EQ
        }

    private fun normaliseStatus(value: String): String? =
        when (value) {
            "r", "ranked" -> "ranked"
            "a", "approved" -> "approved"
            "l", "loved" -> "loved"
            "q", "qualified" -> "qualified"
            "p", "pending" -> "pending"
            "g", "graveyard" -> "graveyard"
            "n", "u", "notsubmitted", "unknown", "any" -> "any"
            else -> null
        }

    private fun statusOf(name: String): BrowseStatus? =
        when (name) {
            "ranked", "approved" -> BrowseStatus.RANKED
            "loved" -> BrowseStatus.LOVED
            "qualified" -> BrowseStatus.QUALIFIED
            "pending" -> BrowseStatus.PENDING
            "graveyard" -> BrowseStatus.GRAVEYARD
            "any" -> BrowseStatus.ANY
            else -> null
        }

    private fun matchesStatus(name: String, status: String): Boolean {
        val s = status.lowercase(Locale.US)
        return when (name) {
            "ranked" -> s == "ranked" || s == "approved"
            "any" -> true
            else -> s == name
        }
    }

    private fun modeOf(value: String): ModeFilter? =
        when (value.lowercase(Locale.US)) {
            "o", "osu", "std", "standard", "0" -> ModeFilter.OSU
            "t", "taiko", "1" -> ModeFilter.TAIKO
            "c", "catch", "catchthebeat", "fruits", "ctb", "2" -> ModeFilter.CATCH
            "m", "mania", "3" -> ModeFilter.MANIA
            else -> null
        }

    /** Accepts any unambiguous prefix: `japan` -> Japanese, `eng` -> English. */
    private fun languageOf(value: String): BrowseLanguage? {
        val v = value.lowercase(Locale.US)
        val all = BrowseLanguage.entries.filter { it.id != null }
        return all.firstOrNull { it.label.equals(v, ignoreCase = true) }
            ?: all.firstOrNull { it.label.lowercase(Locale.US).startsWith(v) && v.length >= 2 }
            ?: all.firstOrNull { v.startsWith(it.label.lowercase(Locale.US).take(5)) && v.length >= 4 }
    }

    private fun genreOf(value: String): BrowseGenre? {
        val v = value.lowercase(Locale.US).replace('_', ' ')
        val all = BrowseGenre.entries.filter { it.id != null }
        return all.firstOrNull { it.label.equals(v, ignoreCase = true) }
            ?: all.firstOrNull { it.label.lowercase(Locale.US).startsWith(v) && v.length >= 2 }
    }

    // ── client-side evaluation ──────────────────────────────────────────────

    private fun matchesCondition(c: Condition, b: Beatmap): Boolean =
        when (c.key) {
            "artist" -> text(c, b.artist, b.artistUnicode)
            "title" -> text(c, b.title, b.titleUnicode)
            "creator" -> text(c, b.mapper)
            "source" -> text(c, b.source)
            "tag" -> text(c, b.tags)
            "stars" -> numeric(c, b.minStars, b.maxStars, tolerance = 0.05)
            "bpm" -> numeric(c, b.bpm, b.bpm, tolerance = 0.5)
            "ranked" -> date(c, b.rankedAt)
            "created" -> date(c, b.submittedAt)
            "updated" -> date(c, b.updatedAt)
            else -> true // not known to the mirror: left to the server, never filtered here
        }

    private fun text(c: Condition, vararg haystack: String): Boolean {
        val needle = c.value.lowercase(Locale.US)
        val hit = haystack.any { it.lowercase(Locale.US).contains(needle) }
        return if (c.op == Op.NE) !hit else hit
    }

    private fun numeric(c: Condition, min: Double, max: Double, tolerance: Double): Boolean {
        val v = c.value.toDoubleOrNull() ?: return true
        if (max <= 0.0 && min <= 0.0) return true // no data reported
        return when (c.op) {
            Op.EQ -> v >= min - tolerance && v <= max + tolerance
            Op.NE -> !(v >= min - tolerance && v <= max + tolerance)
            Op.LT -> min < v
            Op.LE -> min <= v + tolerance
            Op.GT -> max > v
            Op.GE -> max >= v - tolerance
        }
    }

    private fun date(c: Condition, millis: Long): Boolean {
        val range = dateRange(c.value) ?: return true
        if (millis <= 0L) return true
        val (start, end) = range
        return when (c.op) {
            Op.EQ -> millis in start until end
            Op.NE -> millis !in start until end
            Op.LT -> millis < start
            Op.LE -> millis < end
            Op.GT -> millis >= end
            Op.GE -> millis >= start
        }
    }

    /** `2025`, `2025-08` or `2025-08-15` as a half-open UTC range in epoch millis. */
    private fun dateRange(value: String): Pair<Long, Long>? =
        runCatching {
            val parts = value.trim().split('-')
            when (parts.size) {
                1 -> Year.of(parts[0].toInt()).let { y ->
                    y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() to
                        y.plusYears(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
                2 -> YearMonth.of(parts[0].toInt(), parts[1].toInt()).let { m ->
                    m.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() to
                        m.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
                else -> LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()).let { d ->
                    d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() to
                        d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
            }
        }.getOrNull()
}
