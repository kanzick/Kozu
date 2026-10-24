package vn.kozu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Một DataStore duy nhất cho toàn app; mỗi store con dùng key riêng. */
internal val Context.kozuDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_preferences")
