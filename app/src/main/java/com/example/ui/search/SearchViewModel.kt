package com.example.ui.search

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Store
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

data class SearchUiState(
    val rawQuery: String = "",
    val debouncedQuery: String = "",
    val selectedCategoryId: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val isFetchingGps: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchHistoryRepo = SearchHistoryRepository(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchCategories: List<SearchCategory> = StoreRepository.searchCategories

    val recentSearches: StateFlow<List<String>> = searchHistoryRepo.recentSearches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Debounce query input by 250ms
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _uiState
                .map { it.rawQuery }
                .debounce(250)
                .collect { debounced ->
                    _uiState.value = _uiState.value.copy(debouncedQuery = debounced)
                }
        }

        // Trigger store load
        viewModelScope.launch {
            StoreRepository.refreshStoresFromSupabase()
        }
    }

    val isSearchActive: StateFlow<Boolean> = _uiState.map { state ->
        state.debouncedQuery.normalizeText().length >= 2 || state.selectedCategoryId != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Mesma fonte regional usada pela Home: cidade ativa do GPS/endereço + lojas públicas. */
    private val regionalStores: StateFlow<List<Store>> = combine(
        StoreRepository.stores,
        UserSessionRepository.userSession
    ) { stores, session ->
        val city = session.activeLocationCity.ifBlank { session.addressCity }.normalizeText()
        if (city.isBlank()) emptyList() else {
            stores.filter { it.addressCity.normalizeText() == city }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seção "Em Alta" (lojas abertas da cidade ativa, ordenadas por rating desc, máx 8)
    val trendingStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        stores.filter { it.isOpen && (it.logoUrl.isNotBlank() || it.bannerUrl.isNotBlank()) }
            .sortedByDescending { it.rating }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seção "Novidades" da cidade ativa (máx 8)
    val newStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        val thirtyDaysAgo = try {
            Instant.now().minus(30, ChronoUnit.DAYS).toString()
        } catch (e: Exception) {
            ""
        }
        stores.filter { store ->
            store.createdAt.isNotBlank() && thirtyDaysAgo.isNotBlank() && store.createdAt >= thirtyDaysAgo
        }.sortedByDescending { it.createdAt }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Resultados de busca somente da cidade ativa.
    val filteredStores: StateFlow<List<Store>> = combine(
        regionalStores,
        _uiState
    ) { stores, state ->
        val normQuery = state.debouncedQuery.normalizeText()
        val catId = state.selectedCategoryId

        if (normQuery.length < 2 && catId == null) {
            return@combine emptyList()
        }

        val selectedCat = searchCategories.find { it.id == catId }

        val matched = stores.filter { store ->
            val normStoreName = store.name.normalizeText()
            val normStoreCat = store.category.normalizeText()

            val matchesCategory = if (selectedCat == null) {
                true
            } else {
                val catTermsNorm = selectedCat.matchingTerms.map { it.normalizeText() }
                normStoreCat == selectedCat.id ||
                        normStoreCat == selectedCat.name.normalizeText() ||
                        catTermsNorm.any { normStoreCat.contains(it) || it.contains(normStoreCat) }
            }

            val matchesQuery = if (normQuery.length < 2) {
                true
            } else {
                normStoreName.contains(normQuery) ||
                        normStoreCat.contains(normQuery) ||
                        searchCategories.any { cat ->
                            cat.matchingTerms.any { term ->
                                term.normalizeText().contains(normQuery) && normStoreCat.contains(term.normalizeText())
                            }
                        }
            }

            matchesCategory && matchesQuery
        }

        // Open stores first, closed stores second
        matched.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(rawQuery = newQuery)
    }

    fun submitSearch(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) {
            viewModelScope.launch {
                searchHistoryRepo.addSearchTerm(clean)
            }
        }
    }

    fun clearQuery() {
        _uiState.value = _uiState.value.copy(rawQuery = "", debouncedQuery = "")
    }

    fun onCategorySelect(categoryId: String) {
        val newCat = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(selectedCategoryId = newCat)
    }

    fun clearCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null)
    }

    fun onRecentSearchSelect(term: String) {
        _uiState.value = _uiState.value.copy(rawQuery = term, debouncedQuery = term)
        submitSearch(term)
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepo.clearHistory()
        }
    }

    fun requestGpsLocation(context: Context) {
        try {
            _uiState.value = _uiState.value.copy(isFetchingGps = true)
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val networkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val bestLoc = gpsLoc ?: networkLoc
                    if (bestLoc != null) {
                        _uiState.value = _uiState.value.copy(
                            userLocation = Pair(bestLoc.latitude, bestLoc.longitude),
                            isFetchingGps = false
                        )
                        return
                    }
                }
            }
            _uiState.value = _uiState.value.copy(isFetchingGps = false)
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error fetching GPS location", e)
            _uiState.value = _uiState.value.copy(isFetchingGps = false)
        }
    }
}
