package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchDataStore by preferencesDataStore(name = "search_history_preferences")

class SearchHistoryRepository(private val context: Context) {

    companion object {
        private val RECENT_SEARCHES_KEY = stringSetPreferencesKey("recent_searches_list")
    }

    val recentSearches: Flow<List<String>> = context.searchDataStore.data.map { preferences ->
        val set = preferences[RECENT_SEARCHES_KEY] ?: emptySet()
        set.toList()
    }

    suspend fun addSearchTerm(term: String) {
        val cleanTerm = term.trim()
        if (cleanTerm.length < 2) return

        context.searchDataStore.edit { preferences ->
            val current = preferences[RECENT_SEARCHES_KEY] ?: emptySet()
            // Remove previous instances of cleanTerm (case-insensitive deduplication)
            val filtered = current.filter { !it.equals(cleanTerm, ignoreCase = true) }
            val updated = (listOf(cleanTerm) + filtered).take(5)
            preferences[RECENT_SEARCHES_KEY] = updated.toSet()
        }
    }

    suspend fun clearHistory() {
        context.searchDataStore.edit { preferences ->
            preferences[RECENT_SEARCHES_KEY] = emptySet()
        }
    }
}
