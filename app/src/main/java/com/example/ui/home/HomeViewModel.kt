package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Banner
import com.example.data.model.CategoryItem
import com.example.data.model.Order
import com.example.data.model.Store
import com.example.data.model.normalizeBrazilianUf
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.data.model.DiscoverProduct

data class AddressDraft(
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val referencePoint: String = "",
    val whatsapp: String = ""
)

enum class HomeStoreSort(val label: String) {
    RELEVANCE("Mais relevantes"),
    DISTANCE("Mais perto"),
    DELIVERY_FEE("Menor taxa"),
    NAME("Nome da loja")
}

data class HomeUiState(
    val streetName: String = "",
    val streetNumber: String = "",
    val activeCity: String = "",
    val requiresAddress: Boolean = false,
    val isEditingNumber: Boolean = false,
    val selectedCategory: String = "todas",
    val searchQuery: String = "",
    val categories: List<CategoryItem> = StoreRepository.categories,
    val stores: List<Store> = emptyList(),
    val regionalStoreCount: Int = 0,
    val favoriteStores: List<Store> = emptyList(),
    val recentStores: List<Store> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val discoverProducts: List<DiscoverProduct> = emptyList(),
    val affordableProducts: List<DiscoverProduct> = emptyList(),
    val repeatProducts: List<DiscoverProduct> = emptyList(),
    val isFreeFeeFilterActive: Boolean = false,
    val isDirectDeliveryFilterActive: Boolean = false,
    val storeSort: HomeStoreSort = HomeStoreSort.RELEVANCE,
    val recentCompletedOrder: Order? = null,
    val isReordering: Boolean = false,
    val showSupportSheet: Boolean = false,
    val showAddressChoiceDialog: Boolean = false,
    val showAddressForm: Boolean = false,
    val addressDraft: AddressDraft = AddressDraft(),
    val isLookingUpCep: Boolean = false,
    val isSavingAddress: Boolean = false,
    val addressFormError: String? = null,
    val snackbarMessage: String? = null,
    val isRefreshingLocation: Boolean = false,
    val isLoadingStores: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Coordenadas são mantidas apenas em memória: o endereço salvo continua sendo
    // a fonte de localização do perfil, enquanto o GPS melhora distância e ordenação.
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var catalogGeneration = 0
    private var lastLoadedOrderUserId = ""

    init {
        val userSession = UserSessionRepository.userSession.value
        val initialCity = userSession.activeLocationCity.ifBlank { userSession.addressCity }
        _uiState.value = _uiState.value.copy(
            streetName = userSession.activeLocationStreet.ifBlank { userSession.addressStreet },
            streetNumber = userSession.activeLocationNumber.ifBlank { userSession.addressNumber },
            activeCity = initialCity,
            requiresAddress = initialCity.isBlank()
        )

        // A cidade usada no catálogo é GPS quando ativo; sem GPS, usa o endereço cadastrado.
        viewModelScope.launch {
            UserSessionRepository.userSession.collect { session ->
                val effectiveCity = session.activeLocationCity.ifBlank { session.addressCity }
                _uiState.value = _uiState.value.copy(
                    streetName = session.activeLocationStreet.ifBlank { session.addressStreet },
                    streetNumber = session.activeLocationNumber.ifBlank { session.addressNumber },
                    activeCity = effectiveCity,
                    requiresAddress = effectiveCity.isBlank()
                )
                if (session.isLoggedIn && session.userId.isNotBlank() && session.accessToken.isNotBlank() &&
                    session.userId != lastLoadedOrderUserId
                ) {
                    lastLoadedOrderUserId = session.userId
                    loadLatestCompletedOrder(session.userId, session.accessToken)
                    loadRepeatProducts(session.userId, session.accessToken)
                } else if (!session.isLoggedIn) {
                    lastLoadedOrderUserId = ""
                    _uiState.value = _uiState.value.copy(
                        recentCompletedOrder = null,
                        repeatProducts = emptyList()
                    )
                }
                refreshRegionalCatalog()
            }
        }

        // Observe stores flow reactively
        viewModelScope.launch {
            StoreRepository.stores.collect { updatedStores ->
                val storesWithDistance = storesWithCalculatedDistance(updatedStores)
                refreshRegionalCatalog(storesWithDistance)
                refreshRecentStores(storesWithDistance)
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
            val refreshResult = StoreRepository.refreshStoresFromSupabase()
            val currentStores = storesWithCalculatedDistance(refreshResult.stores)

            if (!refreshResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStores = false,
                    errorMessage = "Não foi possível carregar as lojas. Verifique sua conexão e tente novamente."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingStores = false,
                    errorMessage = null
                )
                refreshRegionalCatalog(currentStores)
            }
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            val remoteBanners = SupabaseClient.fetchBanners()
            _uiState.value = _uiState.value.copy(banners = remoteBanners)
        }
    }

    /**
     * Fonte única do catálogo regional. Toda vitrine da Home recebe a mesma lista de
     * lojas da cidade ativa; filtros de interface são aplicados somente após esse recorte.
     */
    private fun refreshRegionalCatalog(sourceStores: List<Store> = storesWithCalculatedDistance(StoreRepository.stores.value)) {
        val activeCity = _uiState.value.activeCity.ifBlank {
            UserSessionRepository.userSession.value.activeLocationCity.ifBlank {
                UserSessionRepository.userSession.value.addressCity
            }
        }
        val normalizedCity = normalizeForComparison(activeCity)
        val regionalStores = if (normalizedCity.isBlank()) emptyList() else {
            sourceStores.filter { normalizeForComparison(it.addressCity) == normalizedCity }
        }
        val generation = ++catalogGeneration
        val filteredStores = applyUiFilters(regionalStores)
        val sortedStores = when (_uiState.value.storeSort) {
            HomeStoreSort.RELEVANCE -> filteredStores.sortedWith(
                compareBy<Store> { !it.isOpen }
                    .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            HomeStoreSort.DISTANCE -> filteredStores.sortedWith(
                compareBy<Store> { !it.isOpen }
                    .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            HomeStoreSort.DELIVERY_FEE -> filteredStores.sortedWith(
                compareBy<Store> { !it.isOpen }
                    .thenBy { deliverySortValue(it) }
                    .thenBy { it.name.lowercase() }
            )
            HomeStoreSort.NAME -> filteredStores.sortedWith(
                compareBy<Store> { !it.isOpen }
                    .thenBy { it.name.lowercase() }
            )
        }
        _uiState.value = _uiState.value.copy(
            stores = sortedStores,
            regionalStoreCount = regionalStores.size,
            favoriteStores = regionalStores.sortedWith(
                compareBy<Store> { !it.isOpen }
                    .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            ).take(2),
            requiresAddress = normalizedCity.isBlank()
        )
        loadDiscoverProducts(regionalStores, generation)
        loadAffordableProducts(regionalStores, generation)
    }

    /**
     * A Home web monta “Suas lojas” pelas lojas de pedidos reais do cliente.
     * Aqui reutilizamos a consulta autenticada já usada na área de pedidos e
     * cruzamos os IDs com o catálogo público para obter logo e metadados atuais.
     */
    private fun refreshRecentStores(sourceStores: List<Store>) {
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(recentStores = emptyList())
            return
        }
        viewModelScope.launch {
            val recentStoreIds = SupabaseClient.fetchOrdersForClient(session.userId, session.accessToken)
                .map { it.storeId }
                .distinct()
                .take(6)
            val storesById = sourceStores.associateBy { it.id }
            _uiState.value = _uiState.value.copy(
                recentStores = recentStoreIds.mapNotNull { storesById[it] }
            )
        }
    }

    /**
     * Produz a vitrine "Peça de novo" somente a partir de pedidos efetivamente concluídos.
     * Cada produto é revalidado no cardápio atual da loja para não oferecer item removido,
     * indisponível ou pertencente a loja fechada. A ordem respeita a recência dos pedidos.
     */
    private fun loadRepeatProducts(userId: String, accessToken: String) {
        viewModelScope.launch {
            try {
                val completedOrders = SupabaseClient.fetchOrdersForClient(userId, accessToken)
                    .filter { order ->
                        order.status.trim().lowercase() in setOf("entregue", "finalizado") && order.items.isNotEmpty()
                    }
                val currentProductsByStore = mutableMapOf<String, Map<String, com.example.data.model.Product>>()
                val seenProductIds = mutableSetOf<String>()
                val repeatProducts = buildList {
                    completedOrders.forEach { order ->
                        if (size >= 8) return@forEach
                        val store = StoreRepository.getStoreById(order.storeId)
                        if (store == null || !store.isOpen) return@forEach
                        val productsById = currentProductsByStore[order.storeId]
                            ?: SupabaseClient.fetchProductsForStore(order.storeId)
                                .filter { it.isAvailable }
                                .associateBy { it.id }
                                .also { currentProductsByStore[order.storeId] = it }
                        order.items.forEach { previousItem ->
                            if (size >= 8 || !seenProductIds.add(previousItem.product.id)) return@forEach
                            val currentProduct = productsById[previousItem.product.id] ?: return@forEach
                            add(
                                DiscoverProduct(
                                    id = currentProduct.id,
                                    storeId = store.id,
                                    storeName = store.name,
                                    storeCategory = store.category,
                                    name = currentProduct.name,
                                    price = currentProduct.price,
                                    imageUrl = currentProduct.imageUrl
                                )
                            )
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(repeatProducts = repeatProducts)
            } catch (error: Exception) {
                android.util.Log.w("HomeViewModel", "Unable to load repeat products", error)
                _uiState.value = _uiState.value.copy(repeatProducts = emptyList())
            }
        }
    }

    private fun loadDiscoverProducts(regionalStores: List<Store>, generation: Int) {
        viewModelScope.launch {
            val openStores = regionalStores.filter { it.isOpen }
            val products = if (regionalStores.isEmpty()) emptyList() else {
                SupabaseClient.fetchDiscoverProducts(openStores.ifEmpty { regionalStores })
            }
            if (generation == catalogGeneration) {
                _uiState.value = _uiState.value.copy(discoverProducts = products)
            }
        }
    }

    /**
     * Vitrine de preço acessível: mostra apenas produtos atuais de lojas abertas
     * na cidade ativa, ordenados pelo menor preço real cadastrado.
     */
    private fun loadAffordableProducts(regionalStores: List<Store>, generation: Int) {
        viewModelScope.launch {
            val openStores = regionalStores.filter { it.isOpen }
            val products = if (openStores.isEmpty()) emptyList() else {
                SupabaseClient.fetchDiscoverProducts(
                    openStores = openStores,
                    limit = 8,
                    orderByPrice = true
                )
            }
            if (generation == catalogGeneration) {
                _uiState.value = _uiState.value.copy(affordableProducts = products)
            }
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

    fun onStoreSortSelect(sort: HomeStoreSort) {
        if (_uiState.value.storeSort == sort) return
        _uiState.value = _uiState.value.copy(storeSort = sort)
        filterStores()
    }

    private fun filterStores() {
        refreshRegionalCatalog()
    }

    private fun applyUiFilters(regionalStores: List<Store>): List<Store> {
        val currentCategory = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()
        val freeFeeOnly = _uiState.value.isFreeFeeFilterActive
        val directDeliveryOnly = _uiState.value.isDirectDeliveryFilterActive
        return regionalStores.filter { store ->
            val categories = listOf(store.category) + store.secondaryCategories
            val matchCategory = currentCategory == "todas" || categories.any { it.equals(currentCategory, ignoreCase = true) }
            val matchQuery = query.isEmpty() ||
                store.name.lowercase().contains(query) ||
                categories.any { it.lowercase().contains(query) } ||
                store.addressNeighborhood.lowercase().contains(query)
            val matchFreeFee = !freeFeeOnly || store.isFreeDelivery || store.ownDeliveryFee <= 0.0 || store.deliveryFee.equals("Grátis", ignoreCase = true)
            val matchDirectDelivery = !directDeliveryOnly || store.deliveryMode.equals("direto", ignoreCase = true) || store.deliveryMode.equals("own", ignoreCase = true)
            matchCategory && matchQuery && matchFreeFee && matchDirectDelivery
        }
    }

    private fun deliverySortValue(store: Store): Double {
        if (store.isFreeDelivery || store.deliveryFee.equals("Grátis", ignoreCase = true)) return 0.0
        return store.officialCustomerDeliveryFee
            ?: store.ownDeliveryFee.takeIf { it > 0.0 }
            ?: Double.MAX_VALUE
    }

    private fun storesWithCalculatedDistance(stores: List<Store>): List<Store> {
        val userLat = currentLatitude ?: return stores
        val userLng = currentLongitude ?: return stores
        return stores.map { store ->
            val storeLat = store.latitude
            val storeLng = store.longitude
            if (storeLat == null || storeLng == null) {
                store
            } else {
                // A referência web aplica fator urbano de 1,3 sobre a distância geodésica.
                store.copy(distanceKm = haversineKm(userLat, userLng, storeLat, storeLng) * 1.3)
            }
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return earthRadiusKm * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private fun normalizeForComparison(value: String): String {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .trim()
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
                SupabaseClient.updateUserProfileNumber(
                    userId = currentUserSession.userId,
                    accessToken = currentUserSession.accessToken,
                    number = currentNumber
                )
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

    fun fetchGpsLocation(context: Context) {
        try {
            _uiState.value = _uiState.value.copy(isRefreshingLocation = true)
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (locationManager != null) {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val gpsLoc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    val networkLoc = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    val bestLoc = gpsLoc ?: networkLoc
                    if (bestLoc != null) {
                        // Converte lat/lng em endereço legível (rua + bairro) via Geocoder nativo do Android
                        var resolvedStreet = "Sua Localização GPS"
                        var resolvedNumber = "${String.format(java.util.Locale.US, "%.4f", bestLoc.latitude)}, ${String.format(java.util.Locale.US, "%.4f", bestLoc.longitude)}"
                        var resolvedNeighborhood = ""
                        var resolvedCity = UserSessionRepository.userSession.value.addressCity
                        var resolvedState = UserSessionRepository.userSession.value.addressState
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale("pt", "BR"))
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(bestLoc.latitude, bestLoc.longitude, 1)
                            val addr = addresses?.firstOrNull()
                            if (addr != null) {
                                val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: addr.locality
                                if (!thoroughfare.isNullOrBlank()) {
                                    resolvedStreet = thoroughfare
                                    resolvedNumber = addr.subThoroughfare ?: ""
                                    resolvedNeighborhood = addr.subLocality ?: addr.subAdminArea ?: ""
                                    resolvedCity = addr.locality ?: addr.subAdminArea ?: resolvedCity
                                    resolvedState = addr.adminArea ?: resolvedState
                                }
                            }
                        } catch (geoEx: Exception) {
                            android.util.Log.e("HomeViewModel", "Geocoder failed, mantendo coordenadas cruas", geoEx)
                        }

                        currentLatitude = bestLoc.latitude
                        currentLongitude = bestLoc.longitude
                        UserSessionRepository.updateActiveLocation(
                            street = resolvedStreet,
                            number = resolvedNumber,
                            neighborhood = resolvedNeighborhood,
                            city = resolvedCity,
                            state = resolvedState,
                            latitude = bestLoc.latitude,
                            longitude = bestLoc.longitude
                        )
                        _uiState.value = _uiState.value.copy(
                            streetName = resolvedStreet,
                            streetNumber = resolvedNumber,
                            activeCity = resolvedCity,
                            requiresAddress = resolvedCity.isBlank(),
                            isRefreshingLocation = false,
                            snackbarMessage = "Localização atualizada com sucesso!"
                        )
                        refreshRegionalCatalog()
                        loadStores()
                        return
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                isRefreshingLocation = false,
                snackbarMessage = "Não foi possível obter GPS. Usando endereço cadastrado."
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error fetching GPS", e)
            _uiState.value = _uiState.value.copy(
                isRefreshingLocation = false,
                snackbarMessage = "Endereço cadastrado ativo."
            )
        }
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

    fun openLocationOrAddressDialog() {
        _uiState.value = _uiState.value.copy(showAddressChoiceDialog = true)
    }

    fun closeLocationOrAddressDialog() {
        _uiState.value = _uiState.value.copy(showAddressChoiceDialog = false)
    }

    fun openAddressForm() {
        val session = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            showAddressChoiceDialog = false,
            showAddressForm = true,
            addressFormError = null,
            addressDraft = AddressDraft(
                cep = session.addressCep,
                street = session.addressStreet,
                number = session.addressNumber,
                complement = session.addressComplement,
                neighborhood = session.addressNeighborhood,
                city = session.addressCity,
                state = session.addressState,
                referencePoint = session.addressReferencePoint,
                whatsapp = session.whatsapp
            )
        )
    }

    fun closeAddressForm() {
        _uiState.value = _uiState.value.copy(showAddressForm = false, addressFormError = null)
    }

    fun updateAddressDraft(transform: (AddressDraft) -> AddressDraft) {
        _uiState.value = _uiState.value.copy(
            addressDraft = transform(_uiState.value.addressDraft),
            addressFormError = null
        )
    }

    fun lookupAddressByCep() {
        val cleanCep = _uiState.value.addressDraft.cep.filter { it.isDigit() }
        if (cleanCep.length != 8) {
            _uiState.value = _uiState.value.copy(addressFormError = "Informe um CEP válido com 8 dígitos.")
            return
        }
        _uiState.value = _uiState.value.copy(isLookingUpCep = true, addressFormError = null)
        viewModelScope.launch {
            val result = SupabaseClient.fetchAddressByCep(cleanCep)
            if (result == null) {
                _uiState.value = _uiState.value.copy(
                    isLookingUpCep = false,
                    addressFormError = "CEP não encontrado. Confira ou preencha manualmente."
                )
            } else {
                updateAddressDraft { current ->
                    current.copy(
                        cep = result.cep.ifBlank { cleanCep },
                        street = result.street.ifBlank { current.street },
                        neighborhood = result.neighborhood.ifBlank { current.neighborhood },
                        city = result.city.ifBlank { current.city },
                        state = result.state.ifBlank { current.state }
                    )
                }
                _uiState.value = _uiState.value.copy(isLookingUpCep = false)
            }
        }
    }

    fun saveAddress() {
        val draft = _uiState.value.addressDraft
        val cleanCep = draft.cep.filter { it.isDigit() }
        val cleanState = normalizeBrazilianUf(draft.state)
        val cleanWhatsapp = draft.whatsapp.filter { it.isDigit() }
        when {
            cleanCep.length != 8 -> {
                _uiState.value = _uiState.value.copy(addressFormError = "Informe um CEP válido.")
                return
            }
            draft.street.isBlank() || draft.number.isBlank() || draft.neighborhood.isBlank() -> {
                _uiState.value = _uiState.value.copy(addressFormError = "Preencha rua, número e bairro.")
                return
            }
            draft.city.isBlank() || cleanState.isBlank() -> {
                _uiState.value = _uiState.value.copy(addressFormError = "Informe um CEP que identifique cidade e estado.")
                return
            }
            cleanWhatsapp.length < 10 -> {
                _uiState.value = _uiState.value.copy(addressFormError = "Informe um WhatsApp válido com DDD.")
                return
            }
        }

        val session = UserSessionRepository.userSession.value
        if (session.userId.isBlank()) {
            _uiState.value = _uiState.value.copy(addressFormError = "Sua sessão expirou. Entre novamente para salvar o endereço.")
            return
        }

        _uiState.value = _uiState.value.copy(isSavingAddress = true, addressFormError = null)
        viewModelScope.launch {
            val saved = SupabaseClient.updateUserProfileAddress(
                userId = session.userId,
                accessToken = session.accessToken,
                cep = cleanCep,
                street = draft.street.trim(),
                number = draft.number.trim(),
                complement = draft.complement.trim(),
                neighborhood = draft.neighborhood.trim(),
                city = draft.city.trim(),
                state = cleanState,
                referencePoint = draft.referencePoint.trim(),
                whatsapp = cleanWhatsapp
            )
            if (!saved) {
                _uiState.value = _uiState.value.copy(
                    isSavingAddress = false,
                    addressFormError = "Não foi possível salvar o endereço. Tente novamente."
                )
                return@launch
            }

            UserSessionRepository.updateProfile(
                name = session.name,
                whatsapp = cleanWhatsapp,
                street = draft.street.trim(),
                number = draft.number.trim(),
                neighborhood = draft.neighborhood.trim(),
                cep = cleanCep,
                pixKeyType = session.pixKeyType,
                pixKey = session.pixKey,
                city = draft.city.trim(),
                state = cleanState,
                complement = draft.complement.trim(),
                referencePoint = draft.referencePoint.trim()
            )
            _uiState.value = _uiState.value.copy(
                streetName = draft.street.trim(),
                streetNumber = draft.number.trim(),
                activeCity = draft.city.trim(),
                requiresAddress = false,
                isSavingAddress = false,
                showAddressForm = false,
                snackbarMessage = "Endereço salvo! Atualizando lojas da sua região."
            )
            refreshRegionalCatalog()
            loadStores()
        }
    }

    /** Carrega apenas o pedido concluído mais recente, com itens reais do Supabase. */
    private fun loadLatestCompletedOrder(userId: String, accessToken: String) {
        viewModelScope.launch {
            val lastCompleted = SupabaseClient.fetchOrdersForClient(userId, accessToken)
                .firstOrNull { order ->
                    order.status.trim().lowercase() in setOf("entregue", "finalizado") && order.items.isNotEmpty()
                }
            _uiState.value = _uiState.value.copy(recentCompletedOrder = lastCompleted)
        }
    }

    /**
     * Recria a sacola usando o cardápio atual. Itens removidos ou indisponíveis não são
     * adicionados, para que o cliente nunca tente comprar produto que a loja não vende mais.
     */
    fun reorderLatestCompletedOrder(onCartReady: () -> Unit) {
        val order = _uiState.value.recentCompletedOrder ?: return
        val session = UserSessionRepository.userSession.value
        val store = StoreRepository.getStoreById(order.storeId)
        if (!session.isLoggedIn || session.userId.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Entre novamente para repetir este pedido.")
            return
        }
        if (store == null || !store.isOpen) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Esta loja não está disponível no momento.")
            return
        }

        _uiState.value = _uiState.value.copy(isReordering = true)
        viewModelScope.launch {
            try {
                val availableProducts = SupabaseClient.fetchProductsForStore(order.storeId)
                    .filter { it.isAvailable }
                    .associateBy { it.id }
                val validItems = order.items.mapNotNull { previousItem ->
                    val currentProduct = availableProducts[previousItem.product.id] ?: return@mapNotNull null
                    previousItem.copy(product = currentProduct)
                }

                if (validItems.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isReordering = false,
                        snackbarMessage = "Os itens deste pedido não estão mais disponíveis."
                    )
                    return@launch
                }

                CartRepository.setDeliveryCoordinates(
                    session.activeLocationLatitude,
                    session.activeLocationLongitude
                )
                CartRepository.replaceWithOrder(
                    storeId = order.storeId,
                    storeName = store.name.ifBlank { order.storeName },
                    items = validItems
                )
                val unavailableCount = order.items.size - validItems.size
                _uiState.value = _uiState.value.copy(
                    isReordering = false,
                    snackbarMessage = if (unavailableCount > 0) {
                        "$unavailableCount item(ns) indisponível(is) foram removidos da sacola."
                    } else {
                        "Itens do último pedido adicionados ao carrinho!"
                    }
                )
                onCartReady()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReordering = false,
                    snackbarMessage = "Não foi possível repetir o pedido. Tente novamente."
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
