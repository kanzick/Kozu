package vn.kozu.data.lazer.realm

import android.content.Context
import io.realm.kotlin.internal.RealmInitializer
import io.realm.kotlin.internal.interop.Link
import io.realm.kotlin.internal.interop.PropertyKey
import io.realm.kotlin.internal.interop.RealmInterop
import io.realm.kotlin.internal.interop.RealmObjectPointer
import io.realm.kotlin.internal.interop.RealmPointer
import io.realm.kotlin.internal.interop.SchemaMode
import io.realm.kotlin.internal.interop.ValueType
import io.realm.kotlin.internal.interop.getterScope
import io.realm.kotlin.internal.interop.inputScope
import java.io.File
import vn.kozu.data.lazer.LazerAccessException
import vn.kozu.data.lazer.LazerAccessProblem
import vn.kozu.data.lazer.LazerCollection
import vn.kozu.data.lazer.LazerLibraryData
import vn.kozu.data.lazer.LazerLibrarySet

internal class LazerRealmReader {
    private class ClassMeta(
        val key: io.realm.kotlin.internal.interop.ClassKey,
        val properties: Map<String, PropertyKey>,
    )

    fun read(database: File): LazerLibraryData {
        val realm = open(database)
        try {
            return readAll(realm)
        } catch (e: LazerAccessException) {
            throw e
        } catch (e: Throwable) {
            throw LazerAccessException(LazerAccessProblem.DATABASE, e.message, e)
        } finally {
            try {
                RealmInterop.realm_close(realm)
            } catch (_: Throwable) {
            }
        }
    }

    private fun open(database: File): RealmPointer {
        var failure: Throwable? = null
        for (mode in OPEN_MODES) {
            try {
                val config = RealmInterop.realm_config_new()
                RealmInterop.realm_config_set_path(config, database.absolutePath)
                RealmInterop.realm_config_set_schema_mode(config, mode)
                val scheduler = RealmInterop.realm_create_scheduler()
                return RealmInterop.realm_open(config, scheduler).first
            } catch (e: Throwable) {
                failure = e
            }
        }
        throw LazerAccessException(LazerAccessProblem.DATABASE, failure?.message, failure)
    }

    private fun meta(
        realm: RealmPointer,
        vararg names: String,
    ): ClassMeta? {
        val key = names.firstNotNullOfOrNull { RealmInterop.realm_find_class(realm, it) } ?: return null
        val info = RealmInterop.realm_get_class(realm, key)
        val properties =
            RealmInterop.realm_get_class_properties(realm, key, info.numProperties + info.numComputedProperties)
        return ClassMeta(key, properties.associate { it.name to it.key })
    }

    private fun readAll(realm: RealmPointer): LazerLibraryData {
        val sets = meta(realm, "BeatmapSet", "BeatmapSetInfo") ?: throw LazerAccessException(LazerAccessProblem.NOT_LAZER)
        val beatmaps = meta(realm, "Beatmap", "BeatmapInfo")
        val metadata = meta(realm, "BeatmapMetadata")
        val rulesets = meta(realm, "Ruleset", "RulesetInfo")
        val usages = meta(realm, "RealmNamedFileUsage")
        val files = meta(realm, "File", "RealmFile")
        val users = meta(realm, "RealmUser")
        val collections = meta(realm, "BeatmapCollection")
        val byMd5 = HashMap<String, Int>()

        val results =
            inputScope {
                val query =
                    RealmInterop.realm_query_parse(realm, sets.key, "TRUEPREDICATE", queryArgsOf(emptyList()))
                RealmInterop.realm_query_find_all(query)
            }
        val count = RealmInterop.realm_results_count(results)
        val output = ArrayList<LazerLibrarySet>()

        for (index in 0 until count) {
            val link =
                with(RealmInterop) { getterScope { realm_results_get(results, index).getLink() } }
            val set = RealmInterop.realm_get_object(realm, link)

            if (bool(set, sets, "DeletePending") || bool(set, sets, "Protected")) continue

            val fileMap = LinkedHashMap<String, String>()
            if (usages != null) {
                for (usageLink in listLinks(set, sets, "Files")) {
                    val usage = RealmInterop.realm_get_object(realm, usageLink)
                    val name = string(usage, usages, "Filename")
                    val fileLink = link(usage, usages, "File")
                    val hash =
                        if (fileLink != null && files != null) {
                            string(RealmInterop.realm_get_object(realm, fileLink), files, "Hash")
                        } else {
                            ""
                        }
                    if (name.isNotBlank() && hash.isNotBlank()) fileMap[name] = hash
                }
            }
            if (fileMap.isEmpty()) continue

            val difficultyLinks = if (beatmaps != null) listLinks(set, sets, "Beatmaps") else emptyList()
            val modes = LinkedHashSet<Int>()
            var title = ""
            var titleUnicode = ""
            var artist = ""
            var artistUnicode = ""
            var author = ""
            var source = ""
            var tags = ""
            var stars = 0.0
            val position = output.size

            for (difficultyLink in difficultyLinks) {
                val difficulty = RealmInterop.realm_get_object(realm, difficultyLink)
                if (beatmaps != null) {
                    stars = maxOf(stars, double(difficulty, beatmaps, "StarRating"))
                    val md5 = string(difficulty, beatmaps, "MD5Hash")
                    if (md5.isNotBlank()) byMd5[md5.lowercase()] = position
                }
                if (beatmaps != null && rulesets != null) {
                    val rulesetLink = link(difficulty, beatmaps, "Ruleset")
                    if (rulesetLink != null) {
                        val mode = long(RealmInterop.realm_get_object(realm, rulesetLink), rulesets, "OnlineID", -1L)
                        if (mode in 0L..3L) modes += mode.toInt()
                    }
                }
                if (title.isNotBlank() && artist.isNotBlank()) continue
                val metadataLink = if (beatmaps != null) link(difficulty, beatmaps, "Metadata") else null
                if (metadataLink != null && metadata != null) {
                    val data = RealmInterop.realm_get_object(realm, metadataLink)
                    title = title.ifBlank { string(data, metadata, "Title") }
                    titleUnicode = titleUnicode.ifBlank { string(data, metadata, "TitleUnicode") }
                    artist = artist.ifBlank { string(data, metadata, "Artist") }
                    artistUnicode = artistUnicode.ifBlank { string(data, metadata, "ArtistUnicode") }
                    source = source.ifBlank { string(data, metadata, "Source") }
                    tags = tags.ifBlank { string(data, metadata, "Tags") }
                    if (author.isBlank()) author = authorOf(realm, data, metadata, users)
                }
            }

            val hash = string(set, sets, "Hash")
            output +=
                LazerLibrarySet(
                    key = "$index:$hash",
                    onlineId = long(set, sets, "OnlineID", -1L),
                    title = title,
                    titleUnicode = titleUnicode,
                    artist = artist,
                    artistUnicode = artistUnicode,
                    author = author,
                    source = source,
                    tags = tags,
                    rulesets = modes,
                    difficulties = difficultyLinks.size,
                    files = fileMap,
                    status = long(set, sets, "Status", -3L).toInt(),
                    addedAt = timestampMillis(set, sets, "DateAdded"),
                    stars = stars,
                )
        }
        return LazerLibraryData(output, readCollections(realm, collections, output, byMd5))
    }

    private fun readCollections(
        realm: RealmPointer,
        collections: ClassMeta?,
        sets: List<LazerLibrarySet>,
        byMd5: Map<String, Int>,
    ): List<LazerCollection> {
        if (collections == null) return emptyList()
        val results =
            inputScope {
                val query =
                    RealmInterop.realm_query_parse(realm, collections.key, "TRUEPREDICATE", queryArgsOf(emptyList()))
                RealmInterop.realm_query_find_all(query)
            }
        val count = RealmInterop.realm_results_count(results)
        val output = ArrayList<LazerCollection>()
        for (index in 0 until count) {
            val link = with(RealmInterop) { getterScope { realm_results_get(results, index).getLink() } }
            val collection = RealmInterop.realm_get_object(realm, link)
            val name = string(collection, collections, "Name").ifBlank { "Collection ${index + 1}" }
            val members = LinkedHashSet<Int>()
            for (md5 in listStrings(collection, collections, "BeatmapMD5Hashes")) {
                byMd5[md5.lowercase()]?.let(members::add)
            }
            if (members.isNotEmpty()) output += LazerCollection(name, members.map { sets[it] })
        }
        return output
    }

    private fun authorOf(
        realm: RealmPointer,
        data: RealmObjectPointer,
        metadata: ClassMeta,
        users: ClassMeta?,
    ): String {
        val key = metadata.properties["Author"] ?: return ""
        return when (val value = scalar(data, key)) {
            is String -> value
            is Link -> if (users != null) string(RealmInterop.realm_get_object(realm, value), users, "Username") else ""
            else -> ""
        }
    }

    private fun scalar(
        obj: RealmObjectPointer,
        key: PropertyKey,
    ): Any? =
        with(RealmInterop) {
            getterScope {
                val value = realm_get_value(obj, key)
                when (value.getType()) {
                    ValueType.RLM_TYPE_INT -> value.getLong()
                    ValueType.RLM_TYPE_BOOL -> value.getBoolean()
                    ValueType.RLM_TYPE_STRING -> value.getString()
                    ValueType.RLM_TYPE_LINK -> value.getLink()
                    ValueType.RLM_TYPE_DOUBLE -> value.getDouble()
                    ValueType.RLM_TYPE_TIMESTAMP -> TimestampSeconds(value.getTimestamp().seconds)
                    else -> null
                }
            }
        }

    private fun string(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): String {
        val key = meta.properties[name] ?: return ""
        return scalar(obj, key) as? String ?: ""
    }

    private fun long(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
        default: Long,
    ): Long {
        val key = meta.properties[name] ?: return default
        return scalar(obj, key) as? Long ?: default
    }

    private class TimestampSeconds(
        val seconds: Long,
    )

    private fun double(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): Double {
        val key = meta.properties[name] ?: return 0.0
        return scalar(obj, key) as? Double ?: 0.0
    }

    private fun timestampMillis(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): Long {
        val key = meta.properties[name] ?: return 0L
        return (scalar(obj, key) as? TimestampSeconds)?.seconds?.times(1000L) ?: 0L
    }

    private fun listStrings(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): List<String> {
        val key = meta.properties[name] ?: return emptyList()
        val list = RealmInterop.realm_get_list(obj, key)
        val size = RealmInterop.realm_list_size(list)
        val values = ArrayList<String>(size.toInt())
        for (position in 0 until size) {
            val text =
                with(RealmInterop) {
                    getterScope {
                        val value = realm_list_get(list, position)
                        if (value.getType() == ValueType.RLM_TYPE_STRING) value.getString() else ""
                    }
                }
            if (text.isNotBlank()) values += text
        }
        return values
    }

    private fun bool(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): Boolean {
        val key = meta.properties[name] ?: return false
        return scalar(obj, key) as? Boolean ?: false
    }

    private fun link(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): Link? {
        val key = meta.properties[name] ?: return null
        return scalar(obj, key) as? Link
    }

    private fun listLinks(
        obj: RealmObjectPointer,
        meta: ClassMeta,
        name: String,
    ): List<Link> {
        val key = meta.properties[name] ?: return emptyList()
        val list = RealmInterop.realm_get_list(obj, key)
        val size = RealmInterop.realm_list_size(list)
        val links = ArrayList<Link>(size.toInt())
        for (position in 0 until size) {
            links += with(RealmInterop) { getterScope { realm_list_get(list, position).getLink() } }
        }
        return links
    }

    companion object {
        private val OPEN_MODES =
            listOf(
                SchemaMode.RLM_SCHEMA_MODE_ADDITIVE_DISCOVERED,
                SchemaMode.RLM_SCHEMA_MODE_READ_ONLY,
                SchemaMode.RLM_SCHEMA_MODE_IMMUTABLE,
            )

        @Volatile
        private var initialized = false

        @Synchronized
        fun ensureLoaded(context: Context) {
            if (initialized) return
            RealmInitializer().create(context.applicationContext)
            initialized = true
        }
    }
}
