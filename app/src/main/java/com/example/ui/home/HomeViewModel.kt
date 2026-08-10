package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val streetName: String = "Rodovia Amaral Peixoto",
    val streetNumber: String = "100",
    val isEditingNumber: Boolean = false,
    val selectedCategory: String = "todas",
    val searchQuery: String = "",
    val categories: List<CategoryItem> = StoreRepository.categories,
    val stores: List<Store> = emptyList(),
    val favoriteStores: List<Store> = emptyList(),
    val lastOrder: LastOrder? = null,
    val showSupportSheet: Boolean = false,
    val snackbarMessage: String? = null,
    val isRefreshingLocation: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val userSession = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            streetName = userSession.addressStreet.ifBlank { "Rodovia Amaral Peixoto" },
            streetNumber = userSession.addressNumber.ifBlank { "100" },
            lastOrder = StoreRepository.lastOrder.value
        )

        // Observe stores flow reactively
        viewModelScope.launch {
            StoreRepository.stores.collect { updatedStores ->
                _uiState.value = _uiState.value.copy(
                    stores = updatedStores,
                    favoriteStores = updatedStores.take(2)
                )
                filterStores()
            }
        }

        // Refresh stores from Supabase stores_public view
        viewModelScope.launch {
            StoreRepository.refreshStoresFromSupabase()
        }
    }

    fun onCategorySelect(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
        filterStores()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterStores()
    }

    private fun filterStores() {
        val currentCategory = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()
        val allStores = StoreRepository.stores.value

        val filtered = allStores.filter { store ->
            val matchCategory = if (currentCategory == "todas") true else {
                store.category.equals(currentCategory, ignoreCase = true)
            }
            val matchQuery = if (query.isEmpty()) true else {
                store.name.lowercase().contains(query) || store.category.lowercase().contains(query)
            }
            matchCategory && matchQuery
        }

        _uiState.value = _uiState.value.copy(stores = filtered)
    }

    fun toggleEditNumber() {
        _uiState.value = _uiState.value.copy(
            isEditingNumber = !_uiState.value.isEditingNumber
        )
    }

    fun onStreetNumberChange(newNumber: String) {
        _uiState.value = _uiState.value.copy(streetNumber = newNumber)
    }

    fun saveStreetNumber() {
        val currentNumber = _uiState.value.streetNumber.ifBlank { "100" }
        _uiState.value = _uiState.value.copy(
            streetNumber = currentNumber,
            isEditingNumber = false,
            snackbarMessage = "Endereço atualizado com sucesso!"
        )
    }

    fun refreshLocation() {
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = true
        )
        // Simulate GPS refresh
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = false,
            snackbarMessage = "Localização atualizada via GPS!"
        )
    }

    fun openSupportSheet() {
        _uiState.value = _uiState.value.copy(showSupportSheet = true)
    }

    fun closeSupportSheet() {
        _uiState.value = _uiState.value.copy(showSupportSheet = false)
    }

    fun onFiltersClick() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "Filtros em breve")
    }

    fun onRepeatLastOrder() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "Itens do último pedido adicionados ao carrinho!")
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
