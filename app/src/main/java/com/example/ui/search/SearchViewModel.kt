package com.example.ui.search

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DiscoverProduct
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.SearchCategory
import com.example.data.repository.SearchHistoryRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import com.example.location.CurrentLocationProvider
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

fun supportsPickup(deliveryMode: String): Boolean =
    deliveryMode.equals("pickup", ignoreCase = true) || deliveryMode.equals("both", ignoreCase = true)

fun supportsDelivery(deliveryMode: String): Boolean =
    deliveryMode.trim().lowercase() in setOf("platform", "own", "direto", "both")

/**
 * Converte a categoria recebida pela rota no id canônico do catálogo.
 * Um id desconhecido vira null para que a Busca abra completa em vez de vazia,
 * caso a Home passe a usar um vocabulário que esta tela ainda não conhece.
 */
fun resolveInitialCategoryId(categoryId: String?): String? {
    val normalized = categoryId?.trim().orEmpty()
    if (normalized.isBlank()) return null
    return StoreRepository.searchCategories
        .firstOrNull { it.id.equals(normalized, ignoreCase = true) }
        ?.id
}

enum class DiscoverQuickFilter(val label: String) {
    OPEN_NOW("Aberto agora"),
    DELIVERY_AVAILABLE("Entrega disponível"),
    FREE_FEE("Taxa grátis"),
    PICKUP("Retirada")
}

/** Política única dos chips da Descoberta; não permite resultados por suposição. */
fun matchesDiscoverQuickFilter(store: Store, filter: DiscoverQuickFilter?): Boolean = when (filter) {
    null -> true
    DiscoverQuickFilter.OPEN_NOW -> store.isOpen
    DiscoverQuickFilter.DELIVERY_AVAILABLE -> supportsDelivery(store.deliveryMode) &&
        store.hasAvailableDriver == true
    DiscoverQuickFilter.FREE_FEE -> supportsDelivery(store.deliveryMode) && store.isFreeDelivery
    DiscoverQuickFilter.PICKUP -> supportsPickup(store.deliveryMode)
}

// ===== Modelos das novas seções da Descoberta (plano 8 ideias) =====

/** Banner do carrossel hero de promoções (dados mockados por enquanto). */
data class PromoBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val ctaText: String,
    val backgroundColor: Long,
    val emoji: String = ""
)

/** Coleção editorial ("Top 10 hambúrgueres") com curador e botão salvar. */
data class DiscoverCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val curatorName: String,
    val emoji: String = "",
    val backgroundColor: Long = 0xFFFFF4EC,
    val saved: Boolean = false
)

/** Oferta do dia com contagem regressiva. */
data class DailyDeal(
    val id: String,
    val title: String,
    val description: String,
    val discountBadge: String,
    val endsAtMillis: Long,
    val tall: Boolean = false
)

/** Item para o rail "Peça de novo", derivado do histórico de pedidos. */
data class OrderAgainItem(
    val productId: String,
    val productName: String,
    val storeId: String,
    val storeName: String,
    val price: Double,
    val imageUrl: String,
    val product: Product
)

/** Formata millis restantes como HH:MM:SS ou MM:SS (sem java.time). */
fun formatCountdown(remainingMillis: Long): String {
    val totalSec = (remainingMillis / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

data class SearchUiState(
    val rawQuery: String = "",
    val debouncedQuery: String = "",
    val selectedCategoryId: String? = null,
    val activeQuickFilter: DiscoverQuickFilter? = null,
    val userLocation: Pair<Double, Double>? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isFetchingGps: Boolean = false,
    val isLoadingDiscovery: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchHistoryRepo = SearchHistoryRepository(application)
    private var initialCategoryApplied = false
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
                _uiState.value = _uiState.value.copy(isLoadingDiscovery = true)
                try {
                    val openStores = stores.filter { it.isOpen }
                    val eligibleStores = openStores.ifEmpty { stores }
                    val products = SupabaseClient.fetchDiscoverProducts(
                        openStores = eligibleStores,
                        limit = 60
                    )
                    _searchableProducts.value = products
                    _featuredProducts.value = products.take(6)
                } catch (error: Exception) {
                    Log.w("SearchViewModel", "Não foi possível carregar os destaques da descoberta", error)
                    _searchableProducts.value = emptyList()
                    _featuredProducts.value = emptyList()
                } finally {
                    _uiState.value = _uiState.value.copy(isLoadingDiscovery = false)
                }
            }
        }
    }

    val trendingStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        stores.filter { it.isOpen && (it.logoUrl.isNotBlank() || it.bannerUrl.isNotBlank()) }
            .sortedByDescending { it.rating }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val newStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        // API 24-safe: sem java.time (Instant/ChronoUnit são API 26+)
        val thirtyDaysAgo = try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -30)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
        } catch (_: Throwable) { "" }
        stores.filter { store ->
            store.createdAt.isNotBlank() && thirtyDaysAgo.isNotBlank() && store.createdAt >= thirtyDaysAgo
        }.sortedByDescending { it.createdAt }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val featuredProducts: StateFlow<List<DiscoverProduct>> = _featuredProducts.asStateFlow()

    /** Catálogo mais amplo para resultados por nome de produto, loja ou categoria. */
    val searchableProducts: StateFlow<List<DiscoverProduct>> = combine(
        _searchableProducts,
        regionalStores,
        _uiState
    ) { products, stores, state ->
        val query = state.debouncedQuery.normalizeText()
        val selectedCategory = searchCategories.find { it.id == state.selectedCategoryId }
        val storesById = stores.associateBy { it.id }
        products.filter { product ->
            val store = storesById[product.storeId]
            val matchesQuery = query.length < 2 ||
                product.name.normalizeText().contains(query) ||
                product.storeName.normalizeText().contains(query) ||
                product.storeCategory.normalizeText().contains(query)
            val matchesCategory = selectedCategory == null ||
                selectedCategory.matchingTerms.any { term ->
                    product.storeCategory.normalizeText().contains(term.normalizeText())
                } || product.storeCategory.normalizeText() == selectedCategory.name.normalizeText()
            val matchesQuickFilter = store != null && matchesDiscoverQuickFilter(store, state.activeQuickFilter)
            matchesQuery && matchesCategory && matchesQuickFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredStores: StateFlow<List<Store>> = combine(regionalStores, _uiState) { stores, state ->
        val normQuery = state.debouncedQuery.normalizeText()
        val selectedCat = searchCategories.find { it.id == state.selectedCategoryId }

        val matched = stores.filter { store ->
            val normStoreName = store.name.normalizeText()
            val normCategories = (listOf(store.category) + store.secondaryCategories).map { it.normalizeText() }
            val matchesCategory = selectedCat == null ||
                StoreRepository.matchesCategory(store, selectedCat.id)
            val matchesQuery = normQuery.length < 2 ||
                normStoreName.contains(normQuery) ||
                normCategories.any { it.contains(normQuery) } ||
                store.addressNeighborhood.normalizeText().contains(normQuery)
            val matchesQuickFilter = matchesDiscoverQuickFilter(store, state.activeQuickFilter)
            matchesCategory && matchesQuery && matchesQuickFilter
        }
        matched.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ===== Novas seções da Descoberta (plano 8 ideias) =====

    /** Banners do carrossel hero (mockados; no futuro vêm de campanha ativa). */
    val promoBanners: StateFlow<List<PromoBanner>> = MutableStateFlow(
        listOf(
            PromoBanner(
                id = "frete_gratis",
                title = "Frete grátis",
                subtitle = "Em pedidos acima de R$ 35 nas lojas participantes",
                ctaText = "Ver lojas",
                backgroundColor = 0xFFF26522,
                emoji = "\uD83D\uDE9A"
            ),
            PromoBanner(
                id = "bemvindo30",
                title = "30% OFF",
                subtitle = "Na primeira compra com o cupom BEMVINDO30",
                ctaText = "Pedir agora",
                backgroundColor = 0xFF7B4DFF,
                emoji = "\uD83C\uDF89"
            ),
            PromoBanner(
                id = "acai_dobro",
                title = "Açaí em dobro",
                subtitle = "Na compra de 1 açaí 500ml, ganhe outro",
                ctaText = "Aproveitar",
                backgroundColor = 0xFF2E7D32,
                emoji = "\uD83C\uDF67"
            )
        )
    ).asStateFlow()

    private val _collections = MutableStateFlow(
        listOf(
            DiscoverCollection(
                id = "top_burger",
                title = "Top 10 hambúrgueres",
                subtitle = "Os mais pedidos da cidade",
                curatorName = "Equipe ItaSuper",
                emoji = "\uD83C\uDF54",
                backgroundColor = 0xFFFFF4EC
            ),
            DiscoverCollection(
                id = "barato_30",
                title = "Comida boa < R$ 30",
                subtitle = "Delícias que cabem no bolso",
                curatorName = "Equipe ItaSuper",
                emoji = "\uD83D\uDCB0",
                backgroundColor = 0xFFE8F5E9
            ),
            DiscoverCollection(
                id = "sobremesas",
                title = "Sobremesas pra hoje",
                subtitle = "Açúcar na medida certa",
                curatorName = "Equipe ItaSuper",
                emoji = "\uD83C\uDF70",
                backgroundColor = 0xFFFCE4EC
            ),
            DiscoverCollection(
                id = "saudavel",
                title = "Leve e saudável",
                subtitle = "Opções fit perto de você",
                curatorName = "Equipe ItaSuper",
                emoji = "\uD83E\uDD57",
                backgroundColor = 0xFFE0F2F1
            )
        )
    )
    val collections: StateFlow<List<DiscoverCollection>> = _collections.asStateFlow()

    fun toggleCollectionSaved(id: String) {
        _collections.value = _collections.value.map {
            if (it.id == id) it.copy(saved = !it.saved) else it
        }
    }

    /** "Peça de novo": pratos distintos do histórico de pedidos. */
    val orderAgainItems: StateFlow<List<OrderAgainItem>> = OrderRepository.orders
        .map { orders ->
            orders
                .flatMap { order -> order.items.map { it to order } }
                .distinctBy { (item, _) -> item.product.id }
                .take(8)
                .map { (item, order) ->
                    OrderAgainItem(
                        productId = item.product.id,
                        productName = item.product.name,
                        storeId = item.product.storeId,
                        storeName = order.storeName,
                        price = item.unitPrice,
                        imageUrl = item.product.imageUrl,
                        product = item.product
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adiciona o produto ao carrinho (1 unidade). */
    fun reorderItem(item: OrderAgainItem) {
        viewModelScope.launch {
            try {
                CartRepository.addProduct(item.product, item.storeName, quantity = 1)
            } catch (_: Exception) {
                // carrinho indisponível no momento; sem crash
            }
        }
    }

    /** Ofertas do dia com contagem regressiva (mockadas; expiram em horas). */
    val dailyDeals: StateFlow<List<DailyDeal>> = MutableStateFlow(
        run {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            listOf(
                DailyDeal(
                    id = "deal_frete",
                    title = "FRETE10",
                    description = "Frete grátis em qualquer pedido",
                    discountBadge = "-R$ 10",
                    endsAtMillis = now + 5 * hour,
                    tall = true
                ),
                DailyDeal(
                    id = "deal_pizza",
                    title = "PIZZA20",
                    description = "20% off em pizzarias",
                    discountBadge = "-20%",
                    endsAtMillis = now + 2 * hour + 34 * 60_000L
                ),
                DailyDeal(
                    id = "deal_acai",
                    title = "ACAI15",
                    description = "15% off em açaís",
                    discountBadge = "-15%",
                    endsAtMillis = now + 8 * hour,
                    tall = true
                ),
                DailyDeal(
                    id = "deal_primeira",
                    title = "BEMVINDO30",
                    description = "30% off na 1ª compra",
                    discountBadge = "-30%",
                    endsAtMillis = now + 11 * hour + 12 * 60_000L
                )
            )
        }
    ).asStateFlow()

    private val utilityTerms = setOf("farmacia", "pet", "mercado", "supermercado", "conveniencia", "padaria")

    /** "Mercados e mais": farmácia/pet/mercado em linhas compactas. */
    val utilityStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        stores.filter { store ->
            val cats = (listOf(store.category) + store.secondaryCategories).map { it.normalizeText() }
            utilityTerms.any { term -> cats.any { it.contains(term) } }
        }.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating }).take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recomendado: abertas, bem avaliadas (base da personalização futura). */
    val recommendedStores: StateFlow<List<Store>> = regionalStores.map { stores ->
        stores.filter { it.isOpen }
            .sortedWith(compareByDescending<Store> { it.rating }.thenBy { it.distanceKm ?: Double.MAX_VALUE })
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Aplica a categoria recebida pela rota uma única vez por navegação.
     * Um id desconhecido é ignorado para que a Busca abra completa em vez de
     * vazia caso a Home mude de vocabulário antes desta tela.
     */
    fun applyInitialCategory(categoryId: String?) {
        if (initialCategoryApplied) return
        initialCategoryApplied = true
        val resolved = resolveInitialCategoryId(categoryId) ?: return
        _uiState.value = _uiState.value.copy(selectedCategoryId = resolved)
    }

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
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val granted = hasFine || hasCoarse
        _uiState.value = _uiState.value.copy(isLocationPermissionGranted = granted)
        if (granted) requestGpsLocation(context)
    }

    fun requestGpsLocation(context: Context) {
        _uiState.value = _uiState.value.copy(isFetchingGps = true)
        viewModelScope.launch {
            val current = try {
                CurrentLocationProvider.getCurrentAddress(context)
            } catch (error: Exception) {
                Log.e("SearchViewModel", "Erro ao atualizar localização atual na descoberta", error)
                null
            }
            if (current != null && current.city.isNotBlank()) {
                UserSessionRepository.updateActiveLocation(
                    street = current.street,
                    number = current.number,
                    neighborhood = current.neighborhood,
                    city = current.city,
                    state = current.state,
                    cep = current.cep,
                    latitude = current.latitude,
                    longitude = current.longitude
                )
            }
            _uiState.value = _uiState.value.copy(
                userLocation = current?.let { it.latitude to it.longitude } ?: _uiState.value.userLocation,
                isFetchingGps = false
            )
        }
    }
}
