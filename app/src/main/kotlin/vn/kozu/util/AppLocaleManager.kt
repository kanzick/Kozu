package vn.kozu.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import vn.kozu.domain.model.AppLanguage

object AppLocaleManager {
    private const val PREFERENCES_NAME = "app_locale_preferences"
    private const val KEY_LANGUAGE_TAG = "app_language_tag"
    private const val KEY_PLATFORM_LOCALE_MIGRATED = "platform_locale_migrated"

    fun currentLanguageTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            if (manager != null) {
                migrateLegacyPreference(context, manager)
                val platformTag = manager.applicationLocales.toLanguageTags().substringBefore(',')
                val normalized = AppLanguage.normalize(platformTag)

                if (platformTag.isNotBlank() && normalized.isBlank()) {
                    manager.applicationLocales = LocaleList.getEmptyLocaleList()
                }
                return normalized
            }
            return AppLanguage.SYSTEM.tag
        }

        return AppLanguage.normalize(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE_TAG, AppLanguage.SYSTEM.tag),
        )
    }

    fun applyLanguage(context: Context, languageTag: String) {
        val normalized = AppLanguage.normalize(languageTag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
            manager.applicationLocales =
                if (normalized.isBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(normalized)
                }
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PLATFORM_LOCALE_MIGRATED, true)
                .apply()
            return
        }

        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, normalized)
            .apply()
    }

    private fun migrateLegacyPreference(
        context: Context,
        manager: android.app.LocaleManager,
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_PLATFORM_LOCALE_MIGRATED, false)) return

        val legacyTag = AppLanguage.normalize(preferences.getString(KEY_LANGUAGE_TAG, AppLanguage.SYSTEM.tag))
        if (manager.applicationLocales.isEmpty && legacyTag.isNotBlank()) {
            manager.applicationLocales = LocaleList.forLanguageTags(legacyTag)
        }
        preferences.edit().putBoolean(KEY_PLATFORM_LOCALE_MIGRATED, true).apply()
    }

    fun wrapContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

        val languageTag = currentLanguageTag(base)
        if (languageTag.isBlank()) {
            val systemLocale = base.resources.configuration.locales.get(0)
            if (systemLocale != null) Locale.setDefault(systemLocale)
            return base
        }

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }
}
