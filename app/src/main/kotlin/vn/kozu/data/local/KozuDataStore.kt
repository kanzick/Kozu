package vn.kozu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.kozuDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_preferences")
internal val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_history")
internal val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_account")
internal val Context.downloadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kozu_downloads")
