package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Banner
import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.data.model.DiscoverProduct

data class HomeUiState(
    val streetName: String = "",
    val streetNumber: String = "",
    val isEditingNumber: Boolean = false,
    val selectedCategory: String = "todas",
    val searchQuery: String = "",
    val categories: List<CategoryItem> = StoreRepository.categories,
    val stores: List<Store> = emptyList(),
    val favoriteStores: List<Store> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val discoverProducts: List<DiscoverProduct> = emptyList(),
    val isFreeFeeFilterActive: Boolean = false,
    val isDirectDeliveryFilterActive: Boolean = false,
    val lastOrder: LastOrder? = null,
    val showSupportSheet: Boolean = false,
    val snackbarMessage: String? = null,
    val isRefreshingLocation: Boolean = false,
    val isLoadingStores: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val userSession = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            streetName = userSession.addressStreet,
            streetNumber = userSession.addressNumber,
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
                loadDiscoverProducts(updatedStores)
            }
        }

        // Fetch stores and banners from Supabase
        loadStores()
        loadBanners()
    }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingStores = true,
                errorMessage = null
            )
            val success = StoreRepository.refreshStoresFromSupabase()
            val currentStores = StoreRepository.stores.value

            if (!success || currentStores.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStores = false,
                    errorMessage = "Não foi possível carregar as lojas, tentar novamente"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingStores = false,
                    errorMessage = null
                )
                loadDiscoverProducts(currentStores)
            }
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            val remoteBanners = SupabaseClient.fetchBanners()
            _uiState.value = _uiState.value.copy(banners = remoteBanners)
        }
    }

    private fun loadDiscoverProducts(stores: List<Store>) {
        viewModelScope.launch {
            val openStores = stores.filter { it.isOpen }
            val products = SupabaseClient.fetchDiscoverProducts(openStores.ifEmpty { stores })
            _uiState.value = _uiState.value.copy(discoverProducts = products)
        }
    }

    fun toggleFreeFeeFilter() {
        val nextState = !_uiState.value.isFreeFeeFilterActive
        _uiState.value = _uiState.value.copy(
            isFreeFeeFilterActive = nextState,
            snackbarMessage = if (nextState) "Filtro: Sem taxa ativado" else "Filtro: Sem taxa removido"
        )
        filterStores()
    }

    fun toggleDirectDeliveryFilter() {
        val nextState = !_uiState.value.isDirectDeliveryFilterActive
        _uiState.value = _uiState.value.copy(
            isDirectDeliveryFilterActive = nextState,
            snackbarMessage = if (nextState) "Filtro: Entrega direta ativado" else "Filtro: Entrega direta removido"
        )
        filterStores()
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
        val freeFeeOnly = _uiState.value.isFreeFeeFilterActive
        val directDeliveryOnly = _uiState.value.isDirectDeliveryFilterActive

        val filtered = allStores.filter { store ->
            val matchCategory = if (currentCategory == "todas") true else {
                store.category.equals(currentCategory, ignoreCase = true)
            }
            val matchQuery = if (query.isEmpty()) true else {
                store.name.lowercase().contains(query) || store.category.lowercase().contains(query)
            }
            val matchFreeFee = if (freeFeeOnly) {
                store.isFreeDelivery || store.ownDeliveryFee <= 0.0 || store.deliveryFee.equals("Grátis", ignoreCase = true)
            } else true

            val matchDirectDelivery = if (directDeliveryOnly) {
                store.deliveryMode.equals("direto", ignoreCase = true) || store.deliveryMode.equals("own", ignoreCase = true)
            } else true

            matchCategory && matchQuery && matchFreeFee && matchDirectDelivery
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
        val currentNumber = _uiState.value.streetNumber
        val currentUserSession = UserSessionRepository.userSession.value

        UserSessionRepository.updateProfile(
            name = currentUserSession.name,
            whatsapp = currentUserSession.whatsapp,
            street = currentUserSession.addressStreet,
            number = currentNumber,
            neighborhood = currentUserSession.addressNeighborhood,
            cep = currentUserSession.addressCep,
            pixKeyType = currentUserSession.pixKeyType,
            pixKey = currentUserSession.pixKey
        )

        viewModelScope.launch {
            if (currentUserSession.userId.isNotBlank()) {
                SupabaseClient.updateUserProfileNumber(currentUserSession.userId, currentNumber)
            }
        }

        _uiState.value = _uiState.value.copy(
            streetNumber = currentNumber,
            isEditingNumber = false,
            snackbarMessage = if (currentNumber.isNotBlank()) "Endereço atualizado no perfil!" else "Por favor adicione seu endereço"
        )
    }

    fun refreshLocation() {
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = true
        )
        // Recarrega lista de lojas
        loadStores()
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = false,
            snackbarMessage = "Lista de lojas atualizada!"
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
