package com.example.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.CartState
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StoreDetailUiState(
    val store: Store? = null,
    val isLoading: Boolean = true,
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val categories: List<String> = emptyList(),
    val selectedProductForModal: Product? = null,
    val modalQuantity: Int = 1,
    val modalNotes: String = ""
)

class StoreDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StoreDetailUiState())
    val uiState: StateFlow<StoreDetailUiState> = _uiState.asStateFlow()

    private val _rawProducts = MutableStateFlow<List<Product>>(emptyList())

    val cartState: StateFlow<CartState> = CartRepository.cartState

    val filteredProducts: StateFlow<List<Product>> = combine(
        _rawProducts,
        _uiState
    ) { products, state ->
        val query = state.searchQuery.trim().lowercase()
        val category = state.selectedCategory

        products.filter { product ->
            val matchesCategory = if (category == "Todos") true else product.category.equals(category, ignoreCase = true)
            val matchesQuery = if (query.isBlank()) true else {
                product.name.lowercase().contains(query) || product.description.lowercase().contains(query)
            }
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadStore(storeId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Find store in repository or fetch refreshed list
            val storeList = StoreRepository.stores.value
            val store = storeList.find { it.id == storeId } ?: storeList.firstOrNull()

            // Fetch products from Supabase
            val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
            _rawProducts.value = remoteProducts

            val categoryList = if (remoteProducts.isNotEmpty()) {
                val list = mutableListOf("Todos")
                list.addAll(remoteProducts.map { it.category }.distinct())
                list
            } else {
                emptyList()
            }

            _uiState.value = _uiState.value.copy(
                store = store,
                isLoading = false,
                categories = categoryList,
                selectedCategory = "Todos"
            )
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openProductModal(product: Product) {
        _uiState.value = _uiState.value.copy(
            selectedProductForModal = product,
            modalQuantity = 1,
            modalNotes = ""
        )
    }

    fun closeProductModal() {
        _uiState.value = _uiState.value.copy(selectedProductForModal = null)
    }

    fun incrementModalQuantity() {
        _uiState.value = _uiState.value.copy(modalQuantity = _uiState.value.modalQuantity + 1)
    }

    fun decrementModalQuantity() {
        if (_uiState.value.modalQuantity > 1) {
            _uiState.value = _uiState.value.copy(modalQuantity = _uiState.value.modalQuantity - 1)
        }
    }

    fun updateModalNotes(notes: String) {
        _uiState.value = _uiState.value.copy(modalNotes = notes)
    }

    fun addSelectedProductToCart() {
        val product = _uiState.value.selectedProductForModal ?: return
        val storeName = _uiState.value.store?.name ?: "Loja"
        CartRepository.addProduct(
            product = product,
            storeName = storeName,
            quantity = _uiState.value.modalQuantity,
            notes = _uiState.value.modalNotes
        )
        closeProductModal()
    }

    fun addDirectProductToCart(product: Product) {
        val storeName = _uiState.value.store?.name ?: "Loja"
        CartRepository.addProduct(
            product = product,
            storeName = storeName,
            quantity = 1
        )
    }
}
