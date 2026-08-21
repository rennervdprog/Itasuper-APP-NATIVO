package com.example.ui.search

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DiscoverProduct
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import com.example.data.repository.SearchCategory
import com.example.data.repository.SearchHistoryRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Instant
import java.time.temporal.ChronoUnit

fun String.normalizeText(): String {
    val unaccented = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return unaccented.lowercase().trim()
}

fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

enum class DiscoverQuickFilter(val label: String) {
    OPEN_NOW("Aberto agora"),
    DELIVERY_AVAILABLE("Entrega disponível"),
    FREE_FEE("Taxa grátis"),
    PICKUP("Retirada")
}

data class SearchUiState(
    val rawQuery: String = "",
    val debouncedQuery: String = "",
    val selectedCategoryId: String? = null,
    val activeQuickFilter: DiscoverQuickFilter? = null,
    val userLocation: Pair<Double, Double>? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isFetchingGps: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchHistoryRepo = SearchHistoryRepository(application)
    private val _uiState = MutableStateFlow(SearchUiState())
    private val _featuredProducts = MutableStateFlow<List<DiscoverProduct>>(emptyList())
    private val _searchableProducts = MutableStateFlow<List<DiscoverProduct>>(emptyList())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    val searchCategories: List<SearchCategory> = StoreRepository.searchCategories

    val recentSearches: StateFlow<List<String>> = searchHistoryRepo.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCity: StateFlow<String> = UserSessionRepository.userSession
        .map { session -> session.activeLocationCity.ifBlank { session.addressCity }.trim() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _uiState
                .map { it.rawQuery }
                .debounce(250)
                .collect { debounced ->
                    _uiState.value = _uiState.value.copy(debouncedQuery = debounced)
                }
        }

        viewModelScope.launch {
            UserSessionRepository.userSession.collect { session ->
                val latitude = session.activeLocationLatitude
                val longitude = session.activeLocationLongitude
                if (latitude != null && longitude != null) {
                    _uiState.value = _uiState.value.copy(userLocation = latitude to longitude)
                }
            }
        }

        viewModelScope.launch { StoreRepository.refreshStoresFromSupabase() }
    }

    val isSearchActive: StateFlow<Boolean> = _uiState.map { state ->
        state.debouncedQuery.normalizeText().length >= 2 ||
            state.selectedCategoryId != null ||
            state.activeQuickFilter != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Mesma fonte regional usada pela Home: cidade ativa do GPS/endereço + lojas públicas. */
    private val regionalStores: StateFlow<List<Store>> = combine(
        StoreRepository.stores,
        UserSessionRepository.userSession
    ) { stores, session ->
        val city = session.activeLocationCity.ifBlank { session.addressCity }.normalizeText()
        if (city.isBlank()) emptyList() else stores.filter { it.addressCity.normalizeText() == city }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            regionalStores.collect { stores ->
                val openStores = stores.filter { it.isOpen }
                val eligibleStores = openStores.ifEmpty { stores }
                val products = SupabaseClient.fetchDiscoverProducts(
                    openStores = eligibleStores,
                    limit = 60
                )
                _searchableProducts.value = products
                _featuredProducts.value = products.take(6)
            }
        }
    }

    val trendingStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        stores.filter { it.isOpen && (it.logoUrl.isNotBlank() || it.bannerUrl.isNotBlank()) }
            .sortedByDescending { it.rating }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val newStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        val thirtyDaysAgo = runCatching { Instant.now().minus(30, ChronoUnit.DAYS).toString() }.getOrDefault("")
        stores.filter { store ->
            store.createdAt.isNotBlank() && thirtyDaysAgo.isNotBlank() && store.createdAt >= thirtyDaysAgo
        }.sortedByDescending { it.createdAt }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val featuredProducts: StateFlow<List<DiscoverProduct>> = _featuredProducts.asStateFlow()

    /** Catálogo mais amplo para resultados por nome de produto, loja ou categoria. */
    val searchableProducts: StateFlow<List<DiscoverProduct>> = combine(_searchableProducts, _uiState) { products, state ->
        val query = state.debouncedQuery.normalizeText()
        val selectedCategory = searchCategories.find { it.id == state.selectedCategoryId }
        products.filter { product ->
            val matchesQuery = query.length < 2 ||
                product.name.normalizeText().contains(query) ||
                product.storeName.normalizeText().contains(query) ||
                product.storeCategory.normalizeText().contains(query)
            val matchesCategory = selectedCategory == null ||
                selectedCategory.matchingTerms.any { term ->
                    product.storeCategory.normalizeText().contains(term.normalizeText())
                } || product.storeCategory.normalizeText() == selectedCategory.name.normalizeText()
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredStores: StateFlow<List<Store>> = combine(regionalStores, _uiState) { stores, state ->
        val normQuery = state.debouncedQuery.normalizeText()
        val selectedCat = searchCategories.find { it.id == state.selectedCategoryId }

        val matched = stores.filter { store ->
            val normStoreName = store.name.normalizeText()
            val normCategories = (listOf(store.category) + store.secondaryCategories).map { it.normalizeText() }
            val matchesCategory = selectedCat == null || run {
                val terms = selectedCat.matchingTerms.map { it.normalizeText() }
                normCategories.any { category ->
                    category == selectedCat.id ||
                        category == selectedCat.name.normalizeText() ||
                        terms.any { term -> category.contains(term) || term.contains(category) }
                }
            }
            val matchesQuery = normQuery.length < 2 ||
                normStoreName.contains(normQuery) ||
                normCategories.any { it.contains(normQuery) } ||
                store.addressNeighborhood.normalizeText().contains(normQuery)
            val matchesQuickFilter = when (state.activeQuickFilter) {
                DiscoverQuickFilter.OPEN_NOW -> store.isOpen
                DiscoverQuickFilter.DELIVERY_AVAILABLE -> !store.deliveryMode.equals("own", true) || store.hasAvailableDriver == true
                DiscoverQuickFilter.FREE_FEE -> store.isFreeDelivery
                DiscoverQuickFilter.PICKUP -> store.deliveryMode.equals("pickup", true)
                null -> true
            }
            matchesCategory && matchesQuery && matchesQuickFilter
        }
        matched.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(rawQuery = newQuery)
    }

    fun submitSearch(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) viewModelScope.launch { searchHistoryRepo.addSearchTerm(clean) }
    }

    fun clearQuery() {
        _uiState.value = _uiState.value.copy(rawQuery = "", debouncedQuery = "")
    }

    fun onCategorySelect(categoryId: String) {
        val nextCategory = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(selectedCategoryId = nextCategory)
    }

    fun clearCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null)
    }

    /** Retorna a aba ao estado completo de Descobrir em uma única ação visível. */
    fun clearDiscovery() {
        _uiState.value = _uiState.value.copy(
            rawQuery = "",
            debouncedQuery = "",
            selectedCategoryId = null,
            activeQuickFilter = null
        )
    }

    fun toggleQuickFilter(filter: DiscoverQuickFilter) {
        _uiState.value = _uiState.value.copy(
            activeQuickFilter = if (_uiState.value.activeQuickFilter == filter) null else filter
        )
    }

    fun onRecentSearchSelect(term: String) {
        _uiState.value = _uiState.value.copy(rawQuery = term, debouncedQuery = term)
        submitSearch(term)
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchHistoryRepo.clearHistory() }
    }

    fun synchronizeLocation(context: Context) {
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val granted = hasFine || hasCoarse
        _uiState.value = _uiState.value.copy(isLocationPermissionGranted = granted)
        if (granted) requestGpsLocation(context)
    }

    fun requestGpsLocation(context: Context) {
        try {
            _uiState.value = _uiState.value.copy(isFetchingGps = true)
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val location = if (locationManager != null && (hasFine || hasCoarse)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
            _uiState.value = _uiState.value.copy(
                userLocation = location?.let { it.latitude to it.longitude } ?: _uiState.value.userLocation,
                isFetchingGps = false
            )
        } catch (error: Exception) {
            Log.e("SearchViewModel", "Erro ao atualizar localização na descoberta", error)
            _uiState.value = _uiState.value.copy(isFetchingGps = false)
        }
    }
}
