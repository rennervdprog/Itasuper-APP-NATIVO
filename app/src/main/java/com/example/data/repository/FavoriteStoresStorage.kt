package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Armazenamento local de lojas favoritas do usuário.
 * Usa SharedPreferences com um Set de IDs das lojas favoritadas.
 */
class FavoriteStoresStorage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun isFavorite(storeId: String): Boolean {
        return _favorites.value.contains(storeId)
    }

    fun toggleFavorite(storeId: String): Boolean {
        val current = _favorites.value.toMutableSet()
        val isNowFavorite = if (current.contains(storeId)) {
            current.remove(storeId)
            false
        } else {
            current.add(storeId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        _favorites.value = current
        return isNowFavorite
    }

    private fun readFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
    }

    companion object {
        private const val PREFS_NAME = "itasuper_favorites"
        private const val KEY_FAVORITES = "favorite_store_ids"
    }
}
