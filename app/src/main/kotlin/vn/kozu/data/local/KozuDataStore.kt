package vn.kozu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.kozuDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_preferences")
