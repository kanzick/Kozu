package vn.kozu.domain.model

import androidx.annotation.StringRes
import java.util.Locale
import vn.kozu.R

enum class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int,
) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    VIETNAMESE("vi", R.string.language_vietnamese),
    ;

    companion object {

        fun normalize(tag: String?): String {
            val value = tag?.trim().orEmpty()
            if (value.isEmpty()) return SYSTEM.tag

            return when (Locale.forLanguageTag(value.replace('_', '-')).language.lowercase(Locale.ROOT)) {
                "en" -> ENGLISH.tag
                "vi" -> VIETNAMESE.tag
                else -> SYSTEM.tag
            }
        }

        fun fromTag(tag: String?): AppLanguage {
            val normalized = normalize(tag)
            return entries.firstOrNull { it.tag == normalized } ?: SYSTEM
        }
    }
}
