package com.example.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AddonGroup
import com.example.data.model.AddonItem
import com.example.data.model.MenuSection
import com.example.data.model.Product
import com.example.data.model.SelectedAddonItem
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
    val menuSections: List<MenuSection> = emptyList(),
    val selectedSectionName: String = "Todos",
    val searchQuery: String = "",
    val selectedProductForModal: Product? = null,
    val modalQuantity: Int = 1,
    val modalNotes: String = "",
    val modalAddonGroups: List<AddonGroup> = emptyList(),
    val modalAddonItemsMap: Map<String, List<AddonItem>> = emptyMap(),
    val modalSelectedAddonsMap: Map<String, List<AddonItem>> = emptyMap(),
    val modalError: String? = null
) {
    val canAddToCart: Boolean
        get() {
            if (selectedProductForModal == null) return false
            for (group in modalAddonGroups) {
                val selected = modalSelectedAddonsMap[group.id] ?: emptyList()
                if (selected.size < group.minSelect) {
                    return false
                }
            }
            return true
        }

    val modalUnitPrice: Double
        get() {
            val product = selectedProductForModal ?: return 0.0
            var base = product.price

            val replacingGroup = modalAddonGroups.firstOrNull { group ->
                group.priceReplacesBase && (modalSelectedAddonsMap[group.id]?.isNotEmpty() == true)
            }

            if (replacingGroup != null) {
                val selectedInReplacing = modalSelectedAddonsMap[replacingGroup.id] ?: emptyList()
                val replacePrice = selectedInReplacing.sumOf { it.price }
                base = replacePrice

                val otherSum = modalAddonGroups.filter { it.id != replacingGroup.id }
                    .sumOf { g ->
                        (modalSelectedAddonsMap[g.id] ?: emptyList()).sumOf { it.price }
                    }
                return base + otherSum
            }

            val addonsSum = modalAddonGroups.sumOf { g ->
                (modalSelectedAddonsMap[g.id] ?: emptyList()).sumOf { it.price }
            }
            return base + addonsSum
        }

    val modalTotalPrice: Double
        get() = modalUnitPrice * modalQuantity
}

class StoreDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StoreDetailUiState())
    val uiState: StateFlow<StoreDetailUiState> = _uiState.asStateFlow()

    private val _rawProducts = MutableStateFlow<List<Product>>(emptyList())
    private var allAddonGroups: List<AddonGroup> = emptyList()
    private var productAddonGroupsMap: Map<String, List<String>> = emptyMap()
    private var allAddonItems: List<AddonItem> = emptyList()

    val cartState: StateFlow<CartState> = CartRepository.cartState

    val filteredProducts: StateFlow<List<Product>> = combine(
        _rawProducts,
        _uiState
    ) { products, state ->
        val query = state.searchQuery.trim().lowercase()
        val sectionName = state.selectedSectionName

        products.filter { product ->
            val matchesSection = if (sectionName == "Todos") {
                true
            } else {
                val selectedSec = state.menuSections.find { it.name.equals(sectionName, ignoreCase = true) }
                if (selectedSec != null) {
                    product.sectionId == selectedSec.id || product.category.equals(selectedSec.name, ignoreCase = true)
                } else {
                    product.category.equals(sectionName, ignoreCase = true)
                }
            }

            val matchesQuery = if (query.isBlank()) true else {
                product.name.lowercase().contains(query) || product.description.lowercase().contains(query)
            }

            matchesSection && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadStore(storeId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val storeList = StoreRepository.stores.value
            val store = storeList.find { it.id == storeId } ?: storeList.firstOrNull()

            // Fetch menu_sections from Supabase
            val sections = SupabaseClient.fetchMenuSectionsForStore(storeId)

            // Fetch products from Supabase (already sorted by name & filtered)
            val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
            _rawProducts.value = remoteProducts

            // Fetch addon data
            allAddonGroups = SupabaseClient.fetchAddonGroupsForStore(storeId)
            productAddonGroupsMap = SupabaseClient.fetchProductAddonGroupsMap()
            allAddonItems = SupabaseClient.fetchAddonItemsForStore()

            _uiState.value = _uiState.value.copy(
                store = store,
                isLoading = false,
                menuSections = sections,
                selectedSectionName = "Todos"
            )
        }
    }

    fun selectSection(sectionName: String) {
        _uiState.value = _uiState.value.copy(selectedSectionName = sectionName)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openProductModal(product: Product) {
        // Find addon groups for this product
        val directGroups = allAddonGroups.filter { it.productId == product.id }
        val mappedGroupIds = productAddonGroupsMap[product.id] ?: emptyList()
        val mappedGroups = allAddonGroups.filter { mappedGroupIds.contains(it.id) }

        val combinedGroups = (directGroups + mappedGroups).distinctBy { it.id }.sortedBy { it.sortOrder }

        val itemsMap = mutableMapOf<String, List<AddonItem>>()
        val initialSelectedMap = mutableMapOf<String, List<AddonItem>>()

        for (group in combinedGroups) {
            val groupItems = allAddonItems.filter { it.groupId == group.id && it.isAvailable }.sortedBy { it.sortOrder }
            itemsMap[group.id] = groupItems

            // If required and maxSelect == 1 and items available, preselect first item
            if (group.minSelect > 0 && group.maxSelect == 1 && groupItems.isNotEmpty()) {
                initialSelectedMap[group.id] = listOf(groupItems.first())
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedProductForModal = product,
            modalQuantity = 1,
            modalNotes = "",
            modalAddonGroups = combinedGroups,
            modalAddonItemsMap = itemsMap,
            modalSelectedAddonsMap = initialSelectedMap,
            modalError = null
        )
    }

    fun closeProductModal() {
        _uiState.value = _uiState.value.copy(selectedProductForModal = null)
    }

    fun toggleAddonItem(group: AddonGroup, item: AddonItem) {
        val currentSelectedMap = _uiState.value.modalSelectedAddonsMap.toMutableMap()
        val currentSelected = (currentSelectedMap[group.id] ?: emptyList()).toMutableList()

        if (group.maxSelect == 1) {
            // Single selection
            if (currentSelected.contains(item) && group.minSelect == 0) {
                currentSelected.clear()
            } else {
                currentSelected.clear()
                currentSelected.add(item)
            }
        } else {
            // Multiple selection
            if (currentSelected.contains(item)) {
                currentSelected.remove(item)
            } else {
                if (currentSelected.size < group.maxSelect) {
                    currentSelected.add(item)
                }
            }
        }

        currentSelectedMap[group.id] = currentSelected
        _uiState.value = _uiState.value.copy(modalSelectedAddonsMap = currentSelectedMap, modalError = null)
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
        val state = _uiState.value
        val product = state.selectedProductForModal ?: return
        
        // Validate required groups
        for (group in state.modalAddonGroups) {
            val selected = state.modalSelectedAddonsMap[group.id] ?: emptyList()
            if (selected.size < group.minSelect) {
                _uiState.value = state.copy(
                    modalError = "Selecione pelo menos ${group.minSelect} opção em '${group.name}'"
                )
                return
            }
        }

        val storeName = state.store?.name ?: "Loja"

        // Build list of SelectedAddonItem
        val selectedAddonsList = mutableListOf<SelectedAddonItem>()
        for (group in state.modalAddonGroups) {
            val selectedItems = state.modalSelectedAddonsMap[group.id] ?: emptyList()
            for (item in selectedItems) {
                selectedAddonsList.add(
                    SelectedAddonItem(
                        itemId = item.id,
                        itemName = item.name,
                        itemPrice = item.price,
                        groupId = group.id,
                        groupName = group.name,
                        priceReplacesBase = group.priceReplacesBase
                    )
                )
            }
        }

        CartRepository.addProduct(
            product = product,
            storeName = storeName,
            quantity = state.modalQuantity,
            notes = state.modalNotes,
            selectedAddons = selectedAddonsList
        )
        closeProductModal()
    }

    fun addDirectProductToCart(product: Product) {
        // If product has required addon groups, open modal instead of adding directly
        val directGroups = allAddonGroups.filter { it.productId == product.id }
        val mappedGroupIds = productAddonGroupsMap[product.id] ?: emptyList()
        val mappedGroups = allAddonGroups.filter { mappedGroupIds.contains(it.id) }
        val hasRequiredGroups = (directGroups + mappedGroups).any { it.minSelect > 0 }

        if (hasRequiredGroups) {
            openProductModal(product)
        } else {
            val storeName = _uiState.value.store?.name ?: "Loja"
            CartRepository.addProduct(
                product = product,
                storeName = storeName,
                quantity = 1
            )
        }
    }
}
