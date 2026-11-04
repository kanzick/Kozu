package vn.kozu.domain.model

import androidx.annotation.StringRes
import vn.kozu.R

/** Supported app languages. An empty tag means follow the device language. */
enum class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int,
) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ARABIC("ar", R.string.language_arabic),
    GERMAN("de", R.string.language_german),
    SPANISH("es", R.string.language_spanish),
    FRENCH("fr", R.string.language_french),
    INDONESIAN("id", R.string.language_indonesian),
    ITALIAN("it", R.string.language_italian),
    JAPANESE("ja", R.string.language_japanese),
    KOREAN("ko", R.string.language_korean),
    PORTUGUESE_BRAZIL("pt-BR", R.string.language_portuguese_brazil),
    RUSSIAN("ru", R.string.language_russian),
    TURKISH("tr", R.string.language_turkish),
    CHINESE_SIMPLIFIED("zh-CN", R.string.language_chinese_simplified),
    VIETNAMESE("vi", R.string.language_vietnamese),
    ;

    companion object {
        fun normalize(tag: String?): String {
            val value = tag?.trim().orEmpty()
            return entries.firstOrNull { it.tag.equals(value, ignoreCase = true) }?.tag ?: SYSTEM.tag
        }

        fun fromTag(tag: String?): AppLanguage {
            val normalized = normalize(tag)
            return entries.firstOrNull { it.tag == normalized } ?: SYSTEM
        }
    }
}
