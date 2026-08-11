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

import com.example.data.model.PastelBorder

data class StoreDetailUiState(
    val store: Store? = null,
    val isLoading: Boolean = true,
    val menuSections: List<MenuSection> = emptyList(),
    val pastelBorders: List<PastelBorder> = emptyList(),
    val pizzaBorders: List<PastelBorder> = emptyList(),
    val selectedSectionName: String = "Todos",
    val searchQuery: String = "",
    val selectedProductForModal: Product? = null,
    val modalQuantity: Int = 1,
    val modalNotes: String = "",
    val modalAddonGroups: List<AddonGroup> = emptyList(),
    val modalAddonItemsMap: Map<String, List<AddonItem>> = emptyMap(),
    val modalSelectedAddonsMap: Map<String, List<AddonItem>> = emptyMap(),
    val modalError: String? = null,
    // Custom Builder State (Monte Sua Pizza / Monte Seu Pastel)
    val showBuilderModal: Boolean = false,
    val builderType: String = "", // "pizza" or "pastel"
    // Pastel Wizard State
    val wizardStep: Int = 0,
    val wizardTargetFlavors: Int = 2,
    val wizardSelectedFlavors: List<Product?> = listOf(null, null),
    val wizardSelectedComplements: List<PastelBorder> = emptyList(),
    val wizardQuantity: Int = 1,
    val wizardNotes: String = "",
    val wizardErrorMessage: String? = null,
    // Pizza builder state
    val builderSelectedFlavors: List<Product> = emptyList(),
    val builderQuantity: Int = 1,
    val builderNotes: String = "",
    val builderSelectedSize: String = "Média",
    val builderStuffedCrust: AddonItem? = null,
    val builderSelectedComplements: List<AddonItem> = emptyList(),
    val builderIsCombo: Boolean = false,
    val builderErrorMessage: String? = null,
    val snackbarMessage: String? = null
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

    val wizardUnitPrice: Double
        get() {
            val selected = wizardSelectedFlavors.filterNotNull()
            if (selected.isEmpty()) return 0.0

            val priceMode = store?.settings?.pastelPriceMode ?: "maior"

            val basePrice = when (priceMode.lowercase()) {
                "media" -> selected.map { it.price }.average()
                "soma" -> selected.sumOf { it.price }
                else -> selected.maxOf { it.price } // "maior" default
            }

            val complementsPrice = wizardSelectedComplements.sumOf { it.price }
            return basePrice + complementsPrice
        }

    val wizardTotalPrice: Double
        get() = wizardUnitPrice * wizardQuantity

    val builderUnitPrice: Double
        get() {
            if (builderSelectedFlavors.isEmpty()) return 0.0
            val priceMode = if (builderType == "pizza") {
                store?.settings?.pizzaPriceMode ?: "maior"
            } else {
                store?.settings?.pastelPriceMode ?: "maior"
            }

            val basePrice = when (priceMode.lowercase()) {
                "media" -> builderSelectedFlavors.map { it.price }.average()
                "soma" -> builderSelectedFlavors.sumOf { it.price }
                else -> builderSelectedFlavors.maxOf { it.price } // "maior" default
            }

            val stuffedCrustPrice = builderStuffedCrust?.price ?: 0.0
            val complementsPrice = builderSelectedComplements.sumOf { it.price }

            return basePrice + stuffedCrustPrice + complementsPrice
        }

    val builderTotalPrice: Double
        get() = builderUnitPrice * builderQuantity

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

            // Fetch pastel_borders from Supabase
            val borders = SupabaseClient.fetchPastelBordersForStore(storeId)

            // Fetch pizza_borders from Supabase (borda recheada, catupiry, cheddar, etc.)
            val pizzaBordersList = SupabaseClient.fetchPizzaBordersForStore(storeId)

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
                pastelBorders = borders,
                pizzaBorders = pizzaBordersList,
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

    fun openBuilder(type: String) {
        val store = _uiState.value.store
        val products = _rawProducts.value

        // Check if store is closed
        if (store == null || !store.isOpen) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Loja fechada. No momento esta loja não está aceitando pedidos."
            )
            return
        }

        // Check if store has at least 2 products
        if (products.size < 2) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Cadastre pelo menos 2 sabores de pizza/pastel para usar o meio a meio."
            )
            return
        }

        if (type == "pastel") {
            val maxFlavors = store.settings.pastelMaxFlavors
            val initialStep = if (maxFlavors > 2) 0 else 1
            val targetFlavors = if (maxFlavors > 2) 2 else minOf(2, maxFlavors)

            _uiState.value = _uiState.value.copy(
                showBuilderModal = true,
                builderType = "pastel",
                wizardStep = initialStep,
                wizardTargetFlavors = targetFlavors,
                wizardSelectedFlavors = List(targetFlavors) { null },
                wizardSelectedComplements = emptyList(),
                wizardQuantity = 1,
                wizardNotes = "",
                wizardErrorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                showBuilderModal = true,
                builderType = type,
                builderSelectedFlavors = emptyList(),
                builderQuantity = 1,
                builderNotes = "",
                builderSelectedSize = "Média",
                builderStuffedCrust = null,
                builderSelectedComplements = emptyList(),
                builderIsCombo = false,
                builderErrorMessage = null
            )
        }
    }

    // Pastel Wizard Methods
    fun setWizardTargetFlavors(target: Int) {
        _uiState.value = _uiState.value.copy(
            wizardTargetFlavors = target,
            wizardSelectedFlavors = List(target) { null },
            wizardStep = 1,
            wizardErrorMessage = null
        )
    }

    fun selectWizardFlavor(slotIndex: Int, product: Product) {
        val current = _uiState.value.wizardSelectedFlavors.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = product
            _uiState.value = _uiState.value.copy(
                wizardSelectedFlavors = current,
                wizardErrorMessage = null
            )
        }
    }

    fun nextWizardStep() {
        val state = _uiState.value
        val step = state.wizardStep
        if (step == 0) {
            _uiState.value = state.copy(wizardStep = 1, wizardErrorMessage = null)
            return
        }
        if (step >= 1 && step <= state.wizardTargetFlavors) {
            val flavorForStep = state.wizardSelectedFlavors.getOrNull(step - 1)
            if (flavorForStep == null) {
                val flavorNum = when (step) {
                    1 -> "1º"
                    2 -> "2º"
                    3 -> "3º"
                    else -> "${step}º"
                }
                _uiState.value = state.copy(wizardErrorMessage = "Por favor, escolha o $flavorNum sabor para continuar.")
                return
            }
            _uiState.value = state.copy(wizardStep = step + 1, wizardErrorMessage = null)
        }
    }

    fun prevWizardStep() {
        val state = _uiState.value
        val step = state.wizardStep
        val maxFlavors = state.store?.settings?.pastelMaxFlavors ?: 4
        if (step == 1 && maxFlavors > 2) {
            _uiState.value = state.copy(wizardStep = 0, wizardErrorMessage = null)
        } else if (step > 1) {
            _uiState.value = state.copy(wizardStep = step - 1, wizardErrorMessage = null)
        } else {
            closeBuilderModal()
        }
    }

    fun toggleWizardComplement(border: PastelBorder) {
        val current = _uiState.value.wizardSelectedComplements.toMutableList()
        val maxComp = _uiState.value.store?.settings?.pastelMaxComplements ?: 3

        if (current.any { it.id == border.id }) {
            current.removeAll { it.id == border.id }
        } else {
            if (current.size < maxComp) {
                current.add(border)
            } else {
                _uiState.value = _uiState.value.copy(
                    wizardErrorMessage = "Você pode escolher no máximo $maxComp complementos."
                )
                return
            }
        }
        _uiState.value = _uiState.value.copy(wizardSelectedComplements = current, wizardErrorMessage = null)
    }

    fun incrementWizardQuantity() {
        _uiState.value = _uiState.value.copy(wizardQuantity = _uiState.value.wizardQuantity + 1)
    }

    fun decrementWizardQuantity() {
        if (_uiState.value.wizardQuantity > 1) {
            _uiState.value = _uiState.value.copy(wizardQuantity = _uiState.value.wizardQuantity - 1)
        }
    }

    fun updateWizardNotes(notes: String) {
        _uiState.value = _uiState.value.copy(wizardNotes = notes)
    }

    fun addPastelWizardToCart() {
        val state = _uiState.value
        val store = state.store ?: return
        val selectedFlavors = state.wizardSelectedFlavors.filterNotNull()

        if (selectedFlavors.size < state.wizardTargetFlavors) {
            _uiState.value = state.copy(wizardErrorMessage = "Selecione todos os ${state.wizardTargetFlavors} sabores.")
            return
        }

        val flavorsText = selectedFlavors.joinToString(" / ") { it.name }
        val productName = "Pastel Meio a Meio ($flavorsText)"

        val selectedAddons = mutableListOf<SelectedAddonItem>()
        for (comp in state.wizardSelectedComplements) {
            selectedAddons.add(
                SelectedAddonItem(
                    itemId = comp.id,
                    itemName = comp.name,
                    itemPrice = comp.price,
                    groupId = "pastel_border",
                    groupName = "Complemento Extra"
                )
            )
        }

        val customProduct = Product(
            id = "custom_pastel_${System.currentTimeMillis()}",
            storeId = store.id,
            name = productName,
            description = "Sabores: $flavorsText",
            price = state.wizardUnitPrice,
            category = "Pastéis"
        )

        CartRepository.addProduct(
            product = customProduct,
            storeName = store.name,
            quantity = state.wizardQuantity,
            notes = state.wizardNotes.trim(),
            selectedAddons = selectedAddons
        )

        closeBuilderModal()
    }

    fun closeBuilderModal() {
        _uiState.value = _uiState.value.copy(showBuilderModal = false)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun toggleBuilderFlavor(product: Product) {
        val current = _uiState.value.builderSelectedFlavors.toMutableList()
        val type = _uiState.value.builderType
        val maxFlavors = if (type == "pizza") {
            _uiState.value.store?.settings?.pizzaMaxFlavors ?: 4
        } else {
            _uiState.value.store?.settings?.pastelMaxFlavors ?: 4
        }

        if (current.any { it.id == product.id }) {
            current.removeAll { it.id == product.id }
        } else {
            if (current.size < maxFlavors) {
                current.add(product)
            } else {
                _uiState.value = _uiState.value.copy(
                    builderErrorMessage = "Você pode escolher no máximo $maxFlavors sabores."
                )
                return
            }
        }
        _uiState.value = _uiState.value.copy(builderSelectedFlavors = current, builderErrorMessage = null)
    }

    fun incrementBuilderQuantity() {
        _uiState.value = _uiState.value.copy(builderQuantity = _uiState.value.builderQuantity + 1)
    }

    fun decrementBuilderQuantity() {
        if (_uiState.value.builderQuantity > 1) {
            _uiState.value = _uiState.value.copy(builderQuantity = _uiState.value.builderQuantity - 1)
        }
    }

    fun updateBuilderNotes(notes: String) {
        _uiState.value = _uiState.value.copy(builderNotes = notes)
    }

    fun setBuilderSize(size: String) {
        _uiState.value = _uiState.value.copy(builderSelectedSize = size)
    }

    fun setBuilderStuffedCrust(addon: AddonItem?) {
        _uiState.value = _uiState.value.copy(builderStuffedCrust = addon)
    }

    fun toggleBuilderComplement(addon: AddonItem) {
        val current = _uiState.value.builderSelectedComplements.toMutableList()
        val maxComp = _uiState.value.store?.settings?.pastelMaxComplements ?: 3

        if (current.any { it.id == addon.id }) {
            current.removeAll { it.id == addon.id }
        } else {
            if (current.size < maxComp) {
                current.add(addon)
            } else {
                _uiState.value = _uiState.value.copy(
                    builderErrorMessage = "Você pode escolher no máximo $maxComp complementos."
                )
                return
            }
        }
        _uiState.value = _uiState.value.copy(builderSelectedComplements = current, builderErrorMessage = null)
    }

    fun setBuilderIsCombo(isCombo: Boolean) {
        _uiState.value = _uiState.value.copy(builderIsCombo = isCombo)
    }

    fun addBuilderToCart() {
        val state = _uiState.value
        val store = state.store ?: return
        val type = state.builderType

        if (state.builderSelectedFlavors.isEmpty()) {
            _uiState.value = state.copy(builderErrorMessage = "Selecione pelo menos 1 sabor.")
            return
        }

        val flavorsText = state.builderSelectedFlavors.joinToString(" / ") { it.name }
        val titlePrefix = if (type == "pizza") "Pizza Meio a Meio" else "Pastel Meio a Meio"
        val productName = "$titlePrefix ($flavorsText)"

        val singleSize = if (type == "pizza") store.settings.pizzaSingleSize else store.settings.pastelSingleSize
        val sizeText = if (!singleSize) "Tamanho: ${state.builderSelectedSize}. " else ""

        val selectedAddons = mutableListOf<SelectedAddonItem>()

        state.builderStuffedCrust?.let { crust ->
            selectedAddons.add(
                SelectedAddonItem(
                    itemId = crust.id,
                    itemName = crust.name,
                    itemPrice = crust.price,
                    groupId = crust.groupId,
                    groupName = "Borda Recheada"
                )
            )
        }

        for (comp in state.builderSelectedComplements) {
            selectedAddons.add(
                SelectedAddonItem(
                    itemId = comp.id,
                    itemName = comp.name,
                    itemPrice = comp.price,
                    groupId = comp.groupId,
                    groupName = "Complemento Extra"
                )
            )
        }

        val customProduct = Product(
            id = "custom_${type}_${System.currentTimeMillis()}",
            storeId = store.id,
            name = productName,
            description = "Sabores: $flavorsText",
            price = state.builderUnitPrice,
            category = if (type == "pizza") "Pizzas" else "Pastéis"
        )

        val fullNotes = (sizeText + state.builderNotes).trim()

        CartRepository.addProduct(
            product = customProduct,
            storeName = store.name,
            quantity = state.builderQuantity,
            notes = fullNotes,
            selectedAddons = selectedAddons
        )

        closeBuilderModal()
    }
}
