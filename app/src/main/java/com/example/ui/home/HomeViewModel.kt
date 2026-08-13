package com.example.ui.home

import android.content.Context
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
    val isFreeFeeFilterActive: Boolean = false,
    val isDirectDeliveryFilterActive: Boolean = false,
    val lastOrder: LastOrder? = null,
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

    init {
        val userSession = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            streetName = userSession.addressStreet,
            streetNumber = userSession.addressNumber,
            activeCity = userSession.addressCity,
            requiresAddress = userSession.addressCity.isBlank(),
            lastOrder = StoreRepository.lastOrder.value
        )

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
            val success = StoreRepository.refreshStoresFromSupabase()
            val currentStores = storesWithCalculatedDistance(StoreRepository.stores.value)

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
        val activeCity = _uiState.value.activeCity.ifBlank { UserSessionRepository.userSession.value.addressCity }
        val normalizedCity = normalizeForComparison(activeCity)
        val regionalStores = if (normalizedCity.isBlank()) emptyList() else {
            sourceStores.filter { normalizeForComparison(it.addressCity) == normalizedCity }
        }
        val generation = ++catalogGeneration
        val filteredStores = applyUiFilters(regionalStores)
        val sortedStores = filteredStores.sortedWith(
            compareBy<Store> { !it.isOpen }
                .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                .thenBy { it.name.lowercase() }
        )
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
        refreshRegionalCatalog()
    }

    private fun applyUiFilters(regionalStores: List<Store>): List<Store> {
        val currentCategory = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()
        val freeFeeOnly = _uiState.value.isFreeFeeFilterActive
        val directDeliveryOnly = _uiState.value.isDirectDeliveryFilterActive
        return regionalStores.filter { store ->
            val matchCategory = currentCategory == "todas" || store.category.equals(currentCategory, ignoreCase = true)
            val matchQuery = query.isEmpty() || store.name.lowercase().contains(query) || store.category.lowercase().contains(query)
            val matchFreeFee = !freeFeeOnly || store.isFreeDelivery || store.ownDeliveryFee <= 0.0 || store.deliveryFee.equals("Grátis", ignoreCase = true)
            val matchDirectDelivery = !directDeliveryOnly || store.deliveryMode.equals("direto", ignoreCase = true) || store.deliveryMode.equals("own", ignoreCase = true)
            matchCategory && matchQuery && matchFreeFee && matchDirectDelivery
        }
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
                        var resolvedCity = UserSessionRepository.userSession.value.addressCity
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale("pt", "BR"))
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(bestLoc.latitude, bestLoc.longitude, 1)
                            val addr = addresses?.firstOrNull()
                            if (addr != null) {
                                val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: addr.locality
                                if (!thoroughfare.isNullOrBlank()) {
                                    resolvedStreet = thoroughfare
                                    resolvedNumber = addr.subThoroughfare ?: (addr.subLocality ?: addr.locality ?: "")
                                    resolvedCity = addr.locality ?: addr.subAdminArea ?: resolvedCity
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
                            city = resolvedCity
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
            draft.city.isBlank() || draft.state.isBlank() -> {
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
                state = draft.state.trim(),
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
                state = draft.state.trim(),
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

    fun onRepeatLastOrder() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "Itens do último pedido adicionados ao carrinho!")
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
