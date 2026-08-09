package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategoryId: String = "todas",
    val recentSearches: List<String> = listOf("Pastel", "Pizza", "Mercado", "Cerveja", "Hambúrguer", "Farmácia"),
    val isRefreshingLocation: Boolean = false,
    val address: String = "Rodovia Amaral Peixoto, 100"
)

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val categories: List<CategoryItem> = StoreRepository.categories

    val filteredStores: StateFlow<List<Store>> = combine(
        StoreRepository.stores,
        _uiState
    ) { stores, state ->
        val query = state.query.trim().lowercase()
        val categoryId = state.selectedCategoryId.lowercase()

        stores.filter { store ->
            val matchesCategory = if (categoryId == "todas") {
                true
            } else {
                val catObj = categories.find { it.id == categoryId }
                val catName = catObj?.name ?: categoryId
                store.category.equals(catName, ignoreCase = true) ||
                        store.category.lowercase().contains(categoryId)
            }

            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                store.name.lowercase().contains(query) ||
                        store.category.lowercase().contains(query)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.isNotBlank() && !_uiState.value.recentSearches.contains(newQuery.trim())) {
            // Keep recent searches updated with typed queries when saved
        }
    }

    fun clearQuery() {
        _uiState.value = _uiState.value.copy(query = "")
    }

    fun onCategorySelect(categoryId: String) {
        val newCategory = if (_uiState.value.selectedCategoryId == categoryId && categoryId != "todas") {
            "todas"
        } else {
            categoryId
        }
        _uiState.value = _uiState.value.copy(selectedCategoryId = newCategory)
    }

    fun onRecentSearchSelect(term: String) {
        _uiState.value = _uiState.value.copy(query = term)
    }

    fun clearRecentSearches() {
        _uiState.value = _uiState.value.copy(recentSearches = emptyList())
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingLocation = true)
            delay(1000)
            _uiState.value = _uiState.value.copy(
                isRefreshingLocation = false,
                address = "Rua Central, 250 (Atualizado)"
            )
        }
    }
}
