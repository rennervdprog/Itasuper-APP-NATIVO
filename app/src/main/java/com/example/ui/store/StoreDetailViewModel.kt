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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

import com.example.data.model.PastelBorder

data class StoreDetailUiState(
    val store: Store? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
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
    /** 1: detalhes/observações; 2: escolhas obrigatórias, quando existentes. */
    val modalStep: Int = 1,
    val modalAddonsLoading: Boolean = false,
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
    // Legado do construtor simples, mantido apenas para compatibilidade interna.
    val builderSelectedFlavors: List<Product> = emptyList(),
    val builderQuantity: Int = 1,
    val builderNotes: String = "",
    val builderSelectedSize: String = "Média",
    val builderStuffedCrust: AddonItem? = null,
    val builderSelectedComplements: List<AddonItem> = emptyList(),
    val builderIsCombo: Boolean = false,
    val builderErrorMessage: String? = null,
    // Wizard de pizza: mesma sequência da versão Capacitor.
    val pizzaWizardStep: Int = 0,
    val pizzaWizardTargetFlavors: Int = 2,
    val pizzaWizardSelectedFlavors: List<Product?> = listOf(null, null),
    val pizzaWizardSelectedSizeId: String? = null,
    val pizzaWizardSelectedSizeName: String? = null,
    val pizzaWizardAddonGroups: List<AddonGroup> = emptyList(),
    val pizzaWizardAddonItemsMap: Map<String, List<AddonItem>> = emptyMap(),
    val pizzaWizardSelectedAddonsMap: Map<String, List<AddonItem>> = emptyMap(),
    val pizzaWizardSelectedBorder: PastelBorder? = null,
    val pizzaWizardQuantity: Int = 1,
    val pizzaWizardNotes: String = "",
    val pizzaWizardErrorMessage: String? = null,
    val snackbarMessage: String? = null
) {
    val hasRequiredModalAddons: Boolean
        get() = modalAddonGroups.any { it.minSelect > 0 }

    val modalTotalSteps: Int
        get() = if (hasRequiredModalAddons) 2 else 1

    val canAddToCart: Boolean
        get() {
            if (selectedProductForModal == null || modalAddonsLoading) return false
            for (group in modalAddonGroups) {
                val selected = modalSelectedAddonsMap[group.id] ?: emptyList()
                if (selected.size < group.minSelect) return false
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

    val pizzaWizardUnitPrice: Double
        get() {
            val selected = pizzaWizardSelectedFlavors.filterNotNull()
            if (selected.isEmpty()) return 0.0
            val settings = store?.settings ?: return 0.0
            val prices = selected.map { flavor ->
                val sizeId = pizzaWizardSelectedSizeId
                val sizeName = pizzaWizardSelectedSizeName
                when {
                    !sizeId.isNullOrBlank() && flavor.pizzaSizeOverrides[sizeId] ?: 0.0 > 0.0 -> flavor.pizzaSizeOverrides.getValue(sizeId)
                    !sizeId.isNullOrBlank() && flavor.pizzaCategoryId.isNotBlank() && settings.pizzaPriceMatrix[flavor.pizzaCategoryId]?.get(sizeId) ?: 0.0 > 0.0 -> settings.pizzaPriceMatrix[flavor.pizzaCategoryId]!!.getValue(sizeId)
                    !sizeName.isNullOrBlank() -> flavor.legacyPizzaSizes.firstOrNull { it.name == sizeName }?.price ?: flavor.price
                    else -> flavor.price
                }
            }
            val pizzaBase = when (settings.pizzaPriceMode.lowercase()) {
                "media", "soma" -> prices.average()
                else -> prices.maxOrNull() ?: 0.0
            }
            val border = pizzaWizardSelectedBorder?.price ?: 0.0
            val addons = pizzaWizardSelectedAddonsMap.values.flatten().sumOf { it.price }
            return pizzaBase + border + addons
        }

    val pizzaWizardTotalPrice: Double
        get() = pizzaWizardUnitPrice * pizzaWizardQuantity

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
    private var loadStoreJob: Job? = null
    private var deliveryAvailabilityJob: Job? = null

    val cartState: StateFlow<CartState> = CartRepository.cartState
    val allProducts: StateFlow<List<Product>> = _rawProducts.asStateFlow()

    init {
        // O detalhe mantém seus dados completos, mas recebe apenas os campos de
        // disponibilidade do catálogo compartilhado para refletir imediatamente
        // a saída/retorno do motoboy sem recarregar cardápio ou fechar a tela.
        viewModelScope.launch {
            StoreRepository.stores.collect { catalog ->
                val state = _uiState.value
                val currentStore = state.store ?: return@collect
                val catalogStore = catalog.firstOrNull { it.id == currentStore.id } ?: return@collect
                if (
                    currentStore.hasAvailableDriver != catalogStore.hasAvailableDriver ||
                    currentStore.deliveryAvailabilityMessage != catalogStore.deliveryAvailabilityMessage
                ) {
                    _uiState.value = state.copy(
                        store = currentStore.copy(
                            hasAvailableDriver = catalogStore.hasAvailableDriver,
                            deliveryAvailabilityMessage = catalogStore.deliveryAvailabilityMessage
                        )
                    )
                }
            }
        }
    }

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
        loadStoreJob?.cancel()
        val previous = _uiState.value
        _uiState.value = previous.copy(isLoading = previous.store == null, errorMessage = null)

        loadStoreJob = viewModelScope.launch {
            try {
                withTimeout(20_000L) {
                    val storeList = StoreRepository.stores.value
                val cachedStore = storeList.find { it.id == storeId }
                // Consulta completa para que o detalhe não herde campos incompletos
                // carregados anteriormente pela Home ou pela Busca.
                val store = SupabaseClient.fetchStoreById(storeId) ?: cachedStore ?: previous.store
                store?.let {
                    CartRepository.rememberStoreDeliveryProfile(
                        storeId = it.id,
                        feeType = it.deliveryFeeType,
                        officialFee = it.officialCustomerDeliveryFee
                    )
                }

                val sections = SupabaseClient.fetchMenuSectionsForStore(storeId)
                val borders = SupabaseClient.fetchPastelBordersForStore(storeId)
                val pizzaBordersList = SupabaseClient.fetchPizzaBordersForStore(storeId)
                val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
                val addonGroups = SupabaseClient.fetchAddonGroupsForStore(storeId)
                val productGroups = SupabaseClient.fetchProductAddonGroupsMap()
                val addonItems = SupabaseClient.fetchAddonItemsForStore()

                if (store == null) {
                    _uiState.value = previous.copy(
                        isLoading = false,
                        errorMessage = "Não foi possível carregar a loja. Verifique sua conexão e tente novamente."
                    )
                    return@withTimeout
                }

                // Uma resposta vazia durante instabilidade não apaga o cardápio já visível.
                if (remoteProducts.isNotEmpty() || _rawProducts.value.isEmpty()) {
                    _rawProducts.value = remoteProducts
                }
                if (addonGroups.isNotEmpty()) allAddonGroups = addonGroups
                if (productGroups.isNotEmpty()) productAddonGroupsMap = productGroups
                if (addonItems.isNotEmpty()) allAddonItems = addonItems

                _uiState.value = _uiState.value.copy(
                    store = store,
                    isLoading = false,
                    errorMessage = null,
                    menuSections = if (sections.isNotEmpty()) sections else previous.menuSections,
                    pastelBorders = if (borders.isNotEmpty()) borders else previous.pastelBorders,
                    pizzaBorders = if (pizzaBordersList.isNotEmpty()) pizzaBordersList else previous.pizzaBorders,
                    selectedSectionName = previous.selectedSectionName.ifBlank { "Todos" }
                )
                observeDeliveryAvailability(store.id)
                }
            } catch (error: TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "A loja demorou para responder. Verifique sua conexão e tente novamente."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Conexão instável. Não foi possível atualizar esta loja."
                )
            }
        }
    }

    /** Interrompe a busca remota imediatamente quando o Android informa que não há internet. */
    fun showOffline(storeId: String) {
        loadStoreJob?.cancel()
        deliveryAvailabilityJob?.cancel()
        val cachedStore = _uiState.value.store ?: StoreRepository.getStoreById(storeId)
        _uiState.value = _uiState.value.copy(
            store = cachedStore,
            isLoading = false,
            errorMessage = if (cachedStore == null) {
                "Você está sem internet. Conecte-se para abrir esta loja."
            } else {
                "Sem internet. Exibindo informações já carregadas."
            },
            snackbarMessage = if (cachedStore != null) "Sem internet. Exibindo informações já carregadas." else null
        )
    }

    /**
     * Faz uma leitura canônica inicial ao abrir a loja. Depois disso, a atualização
     * vem do StoreRepository compartilhado, que consulta todas as lojas próprias
     * enquanto o app está em primeiro plano.
     */
    private fun observeDeliveryAvailability(storeId: String) {
        deliveryAvailabilityJob?.cancel()
        val currentStore = _uiState.value.store ?: return
        if (!currentStore.deliveryMode.equals("own", ignoreCase = true)) return

        deliveryAvailabilityJob = viewModelScope.launch {
            val availability = runCatching {
                SupabaseClient.fetchStoreDeliveryAvailability(storeId)
            }.getOrNull() ?: return@launch
            val state = _uiState.value
            if (state.store?.id == storeId) {
                _uiState.value = state.copy(
                    store = state.store.copy(
                        hasAvailableDriver = availability.canAcceptDeliveryOrders,
                        deliveryAvailabilityMessage = availability.reasonMessage
                    )
                )
            }
        }
    }

    fun selectSection(sectionName: String) {
        _uiState.value = _uiState.value.copy(selectedSectionName = sectionName)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openProductModal(product: Product) {
        // Abre imediatamente e busca vínculos por produto, igual ao Capacitor.
        // A lista global da loja é mantida para os builders, mas não define mais este fluxo.
        _uiState.value = _uiState.value.copy(
            selectedProductForModal = product,
            modalQuantity = 1,
            modalNotes = "",
            modalAddonGroups = emptyList(),
            modalAddonItemsMap = emptyMap(),
            modalSelectedAddonsMap = emptyMap(),
            modalStep = 1,
            modalAddonsLoading = true,
            modalError = null
        )
        viewModelScope.launch {
            val payload = SupabaseClient.fetchAddonsForProduct(product.id)
            val current = _uiState.value
            if (current.selectedProductForModal?.id != product.id) return@launch
            val itemsMap = payload.groups.associate { group ->
                group.id to payload.items.filter { it.groupId == group.id && it.isAvailable }.sortedBy { it.sortOrder }
            }
            _uiState.value = current.copy(
                modalAddonGroups = payload.groups,
                modalAddonItemsMap = itemsMap,
                modalSelectedAddonsMap = emptyMap(),
                modalStep = 1,
                modalAddonsLoading = false,
                modalError = null
            )
        }
    }

    fun closeProductModal() {
        _uiState.value = _uiState.value.copy(
            selectedProductForModal = null,
            modalStep = 1,
            modalAddonsLoading = false,
            modalError = null
        )
    }

    fun nextProductModalStep() {
        val state = _uiState.value
        if (state.modalTotalSteps > 1) {
            _uiState.value = state.copy(modalStep = 2, modalError = null)
        }
    }

    fun previousProductModalStep() {
        val state = _uiState.value
        if (state.modalStep > 1) {
            _uiState.value = state.copy(modalStep = 1, modalError = null)
        }
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
                if (group.maxSelect <= 0 || currentSelected.size < group.maxSelect) {
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

        if (product.requiresPrescription || product.isControlled || product.pharmacySaleMode != "platform_checkout") {
            _uiState.value = state.copy(
                modalError = "Este produto exige validação da farmácia e não pode ser incluído no checkout comum do ItaSuper."
            )
            return
        }
        
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
        // O vínculo obrigatório é assíncrono e específico por produto; abrir o modal
        // evita adicionar antes de a regra real retornar do Supabase.
        openProductModal(product)
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
            val activeCatalogSizes = store.settings.pizzaSizesCatalog.filter { it.active && it.maxFlavors >= 2 }
            val initialCatalogSize = activeCatalogSizes.firstOrNull()
            val initialLegacySize = products.flatMap { it.legacyPizzaSizes }.firstOrNull()?.name
            _uiState.value = _uiState.value.copy(
                showBuilderModal = true,
                builderType = "pizza",
                pizzaWizardStep = if (store.settings.pizzaMaxFlavors <= 2) 1 else 0,
                pizzaWizardTargetFlavors = 2,
                pizzaWizardSelectedFlavors = listOf(null, null),
                pizzaWizardSelectedSizeId = initialCatalogSize?.id,
                pizzaWizardSelectedSizeName = initialCatalogSize?.name ?: initialLegacySize,
                pizzaWizardAddonGroups = emptyList(),
                pizzaWizardAddonItemsMap = emptyMap(),
                pizzaWizardSelectedAddonsMap = emptyMap(),
                pizzaWizardSelectedBorder = pizzaBordersDefault(),
                pizzaWizardQuantity = 1,
                pizzaWizardNotes = "",
                pizzaWizardErrorMessage = null
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

    private fun pizzaBordersDefault(): PastelBorder? {
        return _uiState.value.pizzaBorders.firstOrNull { border ->
            border.isAvailable && (border.name.contains("tradicional", ignoreCase = true) || border.price <= 0.0)
        }
    }

    private fun pizzaWizardGroupsFor(flavors: List<Product?>): Pair<List<AddonGroup>, Map<String, List<AddonItem>>> {
        val flavorIds = flavors.filterNotNull().map { it.id }.toSet()
        if (flavorIds.isEmpty()) return emptyList<AddonGroup>() to emptyMap()
        val directGroups = allAddonGroups.filter { it.productId in flavorIds }
        val mappedIds = flavorIds.flatMap { productAddonGroupsMap[it].orEmpty() }.toSet()
        val groups = (directGroups + allAddonGroups.filter { it.id in mappedIds })
            .distinctBy { it.id }
            .sortedBy { it.sortOrder }
        val items = groups.associate { group ->
            group.id to allAddonItems.filter { it.groupId == group.id && it.isAvailable }.sortedBy { it.sortOrder }
        }
        return groups to items
    }

    fun setPizzaWizardSize(sizeId: String?, sizeName: String?) {
        val state = _uiState.value
        val store = state.store ?: return
        val selectedSize = store.settings.pizzaSizesCatalog.firstOrNull { it.id == sizeId }
        val effectiveMax = minOf(store.settings.pizzaMaxFlavors, selectedSize?.maxFlavors ?: store.settings.pizzaMaxFlavors)
        val target = state.pizzaWizardTargetFlavors.coerceAtMost(effectiveMax.coerceAtLeast(2))
        _uiState.value = state.copy(
            pizzaWizardSelectedSizeId = sizeId,
            pizzaWizardSelectedSizeName = sizeName,
            pizzaWizardTargetFlavors = target,
            pizzaWizardSelectedFlavors = List(target) { null },
            pizzaWizardAddonGroups = emptyList(),
            pizzaWizardAddonItemsMap = emptyMap(),
            pizzaWizardSelectedAddonsMap = emptyMap(),
            pizzaWizardErrorMessage = null
        )
    }

    fun setPizzaWizardTargetFlavors(target: Int) {
        val state = _uiState.value
        val store = state.store ?: return
        val selectedSizeMax = state.pizzaWizardSelectedSizeId?.let { id ->
            store.settings.pizzaSizesCatalog.firstOrNull { it.id == id }?.maxFlavors
        } ?: store.settings.pizzaMaxFlavors
        val effectiveTarget = target.coerceIn(2, minOf(4, store.settings.pizzaMaxFlavors, selectedSizeMax))
        _uiState.value = state.copy(
            pizzaWizardTargetFlavors = effectiveTarget,
            pizzaWizardSelectedFlavors = List(effectiveTarget) { null },
            pizzaWizardAddonGroups = emptyList(),
            pizzaWizardAddonItemsMap = emptyMap(),
            pizzaWizardSelectedAddonsMap = emptyMap(),
            pizzaWizardSelectedBorder = pizzaBordersDefault(),
            pizzaWizardErrorMessage = null
        )
    }

    fun selectPizzaWizardFlavor(slotIndex: Int, product: Product) {
        val state = _uiState.value
        if (!product.isAvailable || product.isBeverage || product.pizzaUnavailableSizeIds.contains(state.pizzaWizardSelectedSizeId)) return
        val current = state.pizzaWizardSelectedFlavors.toMutableList()
        if (slotIndex !in current.indices) return
        if (current.anyIndexed { index, flavor -> index != slotIndex && flavor?.id == product.id }) {
            _uiState.value = state.copy(pizzaWizardErrorMessage = "Escolha um sabor diferente em cada parte da pizza.")
            return
        }
        current[slotIndex] = product
        val (groups, itemsMap) = pizzaWizardGroupsFor(current)
        val validSelections = state.pizzaWizardSelectedAddonsMap.filterKeys { groupId -> groups.any { it.id == groupId } }
        _uiState.value = state.copy(
            pizzaWizardSelectedFlavors = current,
            pizzaWizardAddonGroups = groups,
            pizzaWizardAddonItemsMap = itemsMap,
            pizzaWizardSelectedAddonsMap = validSelections,
            pizzaWizardErrorMessage = null
        )
    }

    private fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean = any { item -> predicate(indexOf(item), item) }

    fun togglePizzaWizardAddon(group: AddonGroup, item: AddonItem) {
        val state = _uiState.value
        val next = state.pizzaWizardSelectedAddonsMap.toMutableMap()
        val current = next[group.id].orEmpty().toMutableList()
        if (current.any { it.id == item.id }) {
            current.removeAll { it.id == item.id }
        } else if (group.maxSelect <= 0 || current.size < group.maxSelect) {
            current.add(item)
        } else {
            _uiState.value = state.copy(pizzaWizardErrorMessage = "Você pode selecionar no máximo ${group.maxSelect} opção(ões) em ${group.name}.")
            return
        }
        next[group.id] = current
        _uiState.value = state.copy(pizzaWizardSelectedAddonsMap = next, pizzaWizardErrorMessage = null)
    }

    fun selectPizzaWizardBorder(border: PastelBorder) {
        _uiState.value = _uiState.value.copy(pizzaWizardSelectedBorder = border, pizzaWizardErrorMessage = null)
    }

    fun updatePizzaWizardNotes(notes: String) {
        _uiState.value = _uiState.value.copy(pizzaWizardNotes = notes.take(200))
    }

    fun incrementPizzaWizardQuantity() {
        _uiState.value = _uiState.value.copy(pizzaWizardQuantity = _uiState.value.pizzaWizardQuantity + 1)
    }

    fun decrementPizzaWizardQuantity() {
        if (_uiState.value.pizzaWizardQuantity > 1) {
            _uiState.value = _uiState.value.copy(pizzaWizardQuantity = _uiState.value.pizzaWizardQuantity - 1)
        }
    }

    private fun pizzaWizardAddonsValid(state: StoreDetailUiState): Boolean = state.pizzaWizardAddonGroups.all { group ->
        state.pizzaWizardSelectedAddonsMap[group.id].orEmpty().size >= group.minSelect
    }

    fun nextPizzaWizardStep() {
        val state = _uiState.value
        if (state.pizzaWizardStep == 0) {
            _uiState.value = state.copy(pizzaWizardStep = 1, pizzaWizardErrorMessage = null)
            return
        }
        if (state.pizzaWizardStep in 1..state.pizzaWizardTargetFlavors) {
            val selected = state.pizzaWizardSelectedFlavors.getOrNull(state.pizzaWizardStep - 1)
            if (selected == null) {
                _uiState.value = state.copy(pizzaWizardErrorMessage = "Escolha o ${state.pizzaWizardStep}º sabor para continuar.")
                return
            }
            if (state.pizzaWizardStep < state.pizzaWizardTargetFlavors) {
                _uiState.value = state.copy(pizzaWizardStep = state.pizzaWizardStep + 1, pizzaWizardErrorMessage = null)
                return
            }
            if (state.pizzaWizardAddonGroups.isNotEmpty() || state.pizzaBorders.any { it.isAvailable }) {
                _uiState.value = state.copy(pizzaWizardStep = state.pizzaWizardStep + 1, pizzaWizardErrorMessage = null)
            }
            return
        }
        val addonStep = state.pizzaWizardTargetFlavors + 1
        if (state.pizzaWizardStep == addonStep && state.pizzaWizardAddonGroups.isNotEmpty()) {
            if (!pizzaWizardAddonsValid(state)) {
                _uiState.value = state.copy(pizzaWizardErrorMessage = "Complete as opções obrigatórias antes de continuar.")
                return
            }
            if (state.pizzaBorders.any { it.isAvailable }) {
                _uiState.value = state.copy(pizzaWizardStep = addonStep + 1, pizzaWizardErrorMessage = null)
            }
        }
    }

    fun prevPizzaWizardStep() {
        val state = _uiState.value
        when {
            state.pizzaWizardStep == 0 -> closeBuilderModal()
            state.pizzaWizardStep == 1 -> _uiState.value = state.copy(pizzaWizardStep = 0, pizzaWizardErrorMessage = null)
            else -> _uiState.value = state.copy(pizzaWizardStep = state.pizzaWizardStep - 1, pizzaWizardErrorMessage = null)
        }
    }

    fun addPizzaWizardToCart() {
        val state = _uiState.value
        val store = state.store ?: return
        val flavors = state.pizzaWizardSelectedFlavors.filterNotNull()
        if (flavors.size != state.pizzaWizardTargetFlavors) {
            _uiState.value = state.copy(pizzaWizardErrorMessage = "Selecione todos os ${state.pizzaWizardTargetFlavors} sabores.")
            return
        }
        if (!pizzaWizardAddonsValid(state)) {
            _uiState.value = state.copy(pizzaWizardErrorMessage = "Complete as opções obrigatórias antes de adicionar.")
            return
        }
        val fraction = when (state.pizzaWizardTargetFlavors) { 2 -> "½"; 3 -> "⅓"; else -> "¼" }
        val selectedAddons = mutableListOf<SelectedAddonItem>()
        state.pizzaWizardSelectedSizeName?.let { size ->
            selectedAddons.add(SelectedAddonItem("pizza_size_${state.pizzaWizardSelectedSizeId ?: size}", "Tamanho: $size", 0.0, "pizza_size", "Tamanho"))
        }
        flavors.forEachIndexed { index, flavor ->
            selectedAddons.add(SelectedAddonItem("pizza_flavor_${index}_${flavor.id}", "$fraction ${flavor.name}", 0.0, "pizza_flavors", "Sabores"))
        }
        state.pizzaWizardSelectedBorder?.let { border ->
            selectedAddons.add(SelectedAddonItem(border.id, "Borda: ${border.name}", border.price, "pizza_border", "Borda"))
        }
        state.pizzaWizardAddonGroups.forEach { group ->
            state.pizzaWizardSelectedAddonsMap[group.id].orEmpty().forEach { item ->
                selectedAddons.add(SelectedAddonItem(item.id, item.name, item.price, group.id, group.name, group.priceReplacesBase))
            }
        }
        val basePrice = state.pizzaWizardUnitPrice - (state.pizzaWizardSelectedBorder?.price ?: 0.0) - state.pizzaWizardSelectedAddonsMap.values.flatten().sumOf { it.price }
        val title = if (state.pizzaWizardTargetFlavors == 2) "Pizza Meio a Meio" else "Pizza ${state.pizzaWizardTargetFlavors} Sabores"
        val flavorsText = flavors.joinToString(" / ") { it.name }
        val product = flavors.first().copy(
            name = state.pizzaWizardSelectedSizeName?.let { "$title: $flavorsText ($it)" } ?: "$title: $flavorsText",
            description = "Sabores: $flavorsText",
            price = basePrice.coerceAtLeast(0.0)
        )
        CartRepository.addProduct(product, store.name, state.pizzaWizardQuantity, state.pizzaWizardNotes.trim(), selectedAddons)
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
