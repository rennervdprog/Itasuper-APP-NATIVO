package com.example.ui.search

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Coupon
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
    FREE("Entrega grátis"),
    TOP("4.5+ ★"),
    FAST("< 30 min"),
    COUPON("Com cupom"),
    CHEAP("Até R$ 30")
}

/** Extrai o maior número de minutos de um texto tipo "20–30 min". Null se não der pra parsear. */
fun maxDeliveryMinutes(deliveryTime: String): Int? {
    val numbers = Regex("\\d+").findAll(deliveryTime).map { it.value.toInt() }.toList()
    return numbers.maxOrNull()
}

/** Política única dos chips da Descoberta; não permite resultados por suposição. */
fun matchesDiscoverQuickFilter(
    store: Store,
    filter: DiscoverQuickFilter?,
    couponStoreIds: Set<String> = emptySet(),
    cheapStoreIds: Set<String> = emptySet()
): Boolean = when (filter) {
    null -> true
    DiscoverQuickFilter.FREE -> store.isFreeDelivery
    DiscoverQuickFilter.TOP -> store.rating >= 4.5
    DiscoverQuickFilter.FAST -> (maxDeliveryMinutes(store.deliveryTime) ?: Int.MAX_VALUE) <= 30
    DiscoverQuickFilter.COUPON -> store.id in couponStoreIds
    DiscoverQuickFilter.CHEAP -> store.id in cheapStoreIds
}

// ===== Modelos das novas seções da Descoberta (plano 8 ideias) =====

/** Banner do carrossel hero: loja real com cupom ativo (via RPC get_stores_with_coupons). */
data class PromoBanner(
    val id: String,
    val tag: String = "Cupom",
    val title: String,
    val subtitle: String,
    val ctaText: String = "Ver loja",
    val backgroundColor: Long,
    val storeId: String,
    val imageUrl: String = ""
)

/** Coleção dinâmica gerada de dados reais (lojas filtradas), com botão salvar. */
data class DiscoverCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val stores: List<Store>,
    val backgroundColor: Long = 0xFFFFF4EC,
    val saved: Boolean = false
)

/** Oferta do dia: cupom real; endsAtMillis = 0 quando o cupom não tem expiração. */
data class DailyDeal(
    val id: String,
    val title: String,
    val description: String,
    val discountBadge: String,
    val endsAtMillis: Long,
    val tall: Boolean = false,
    val storeId: String? = null
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

/** Converte "2026-10-20T23:59:59+00:00" em millis (API 24-safe, sem java.time). 0 = sem expiração. */
fun parseIsoToMillis(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        val clean = iso.trim()
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .replace(Regex("Z$"), "")
            .substringBefore(".")
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(clean)?.time ?: 0L
    } catch (_: Throwable) {
        0L
    }
}

/** Badge do cupom real, ex: "-20%" ou "-R$10". */
fun couponBadgeText(discountType: String, discountValue: Double): String =
    if (discountType.equals("percent", ignoreCase = true) || discountType.equals("percentage", ignoreCase = true))
        "-${discountValue.toInt()}%"
    else
        "-R$${discountValue.toInt()}"

/** Descrição legível do cupom real em pt-BR. */
fun couponDescriptionText(coupon: Coupon): String {
    val parts = mutableListOf<String>()
    val discount = if (coupon.discountType.equals("percent", ignoreCase = true) || coupon.discountType.equals("percentage", ignoreCase = true))
        "${coupon.discountValue.toInt()}% de desconto"
    else
        "R$ ${"%.2f".format(coupon.discountValue).replace('.', ',')} de desconto"
    parts += discount
    if (coupon.minOrderValue > 0)
        parts += "em pedidos acima de R$ ${"%.2f".format(coupon.minOrderValue).replace('.', ',')}"
    if (coupon.firstOrderOnly) parts += "só na 1ª compra"
    return parts.joinToString(" • ")
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
        _uiState,
        _couponStoreIds,
        cheapStoreIds
    ) { products, stores, state, couponIds, cheapIds ->
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
            val matchesQuickFilter = store != null && matchesDiscoverQuickFilter(store, state.activeQuickFilter, couponIds, cheapIds)
            matchesQuery && matchesCategory && matchesQuickFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredStores: StateFlow<List<Store>> = combine(regionalStores, _uiState, _couponStoreIds, cheapStoreIds) { stores, state, couponIds, cheapIds ->
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
            val matchesQuickFilter = matchesDiscoverQuickFilter(store, state.activeQuickFilter, couponIds, cheapIds)
            matchesCategory && matchesQuery && matchesQuickFilter
        }
        matched.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ===== Seções da Descoberta com dados reais =====

    private val _promoBanners = MutableStateFlow<List<PromoBanner>>(emptyList())

    /** Banners reais: lojas da cidade com cupom ativo (RPC get_stores_with_coupons). */
    val promoBanners: StateFlow<List<PromoBanner>> = _promoBanners.asStateFlow()

    private val _couponStoreIds = MutableStateFlow<Set<String>>(emptySet())

    /** IDs das lojas com cupom ativo — usado pelo filtro "Com cupom". */
    val couponStoreIds: StateFlow<Set<String>> = _couponStoreIds.asStateFlow()

    /** IDs das lojas com ao menos um produto de até R$ 30 — usado pelo filtro "Até R$ 30". */
    val cheapStoreIds: StateFlow<Set<String>> = _searchableProducts
        .map { products -> products.filter { it.price <= 30.0 }.map { it.storeId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val bannerPalette = listOf(0xFFF26522, 0xFF177A3D, 0xFF8A4B0F)

    init {
        viewModelScope.launch {
            regionalStores.collect { stores ->
                try {
                    val ids = stores.map { it.id }.toSet()
                    val withCoupons = SupabaseClient.fetchStoresWithCoupons(limit = 20)
                        .filter { it.storeId in ids }
                    _couponStoreIds.value = withCoupons.map { it.storeId }.toSet()
                    _promoBanners.value = withCoupons.take(5).mapIndexed { index, swc ->
                        PromoBanner(
                            id = "coupon_${swc.storeId}_${swc.couponCode}",
                            title = couponBadgeText(swc.discountType, swc.discountValue),
                            subtitle = "${swc.storeName} • Cupom ${swc.couponCode}",
                            backgroundColor = bannerPalette[index % bannerPalette.size],
                            storeId = swc.storeId,
                            imageUrl = swc.storeLogoUrl.orEmpty()
                        )
                    }
                } catch (_: Exception) {
                    _promoBanners.value = emptyList()
                    _couponStoreIds.value = emptySet()
                }
            }
        }
    }

    private val _collectionsSaved = MutableStateFlow<Set<String>>(emptySet())

    /** Coleções dinâmicas geradas de dados reais da cidade. Só aparece quem tem loja. */
    val collections: StateFlow<List<DiscoverCollection>> = combine(
        regionalStores,
        _couponStoreIds,
        _collectionsSaved
    ) { stores, couponIds, savedIds ->
        // API 24-safe: sem java.time
        val thirtyDaysAgo = try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -30)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
        } catch (_: Throwable) { "" }
        buildList {
            val onSale = stores.filter { it.id in couponIds }
            if (onSale.isNotEmpty()) add(
                DiscoverCollection(
                    id = "em_oferta",
                    title = "Em oferta",
                    subtitle = "${onSale.size} ${if (onSale.size == 1) "loja" else "lojas"} com cupom",
                    stores = onSale.sortedByDescending { it.rating }.take(3),
                    backgroundColor = 0xFFFFF4EC,
                    saved = "em_oferta" in savedIds
                )
            )
            val topRated = stores.filter { it.rating >= 4.5 }
            if (topRated.isNotEmpty()) add(
                DiscoverCollection(
                    id = "bem_avaliados",
                    title = "Bem avaliados",
                    subtitle = "${topRated.size} ${if (topRated.size == 1) "loja" else "lojas"} nota 4.5+",
                    stores = topRated.sortedByDescending { it.rating }.take(3),
                    backgroundColor = 0xFFE8F5E9,
                    saved = "bem_avaliados" in savedIds
                )
            )
            val freeDelivery = stores.filter { it.isFreeDelivery }
            if (freeDelivery.isNotEmpty()) add(
                DiscoverCollection(
                    id = "entrega_gratis",
                    title = "Entrega grátis",
                    subtitle = "${freeDelivery.size} ${if (freeDelivery.size == 1) "loja" else "lojas"} sem taxa",
                    stores = freeDelivery.sortedByDescending { it.rating }.take(3),
                    backgroundColor = 0xFFE3F2FD,
                    saved = "entrega_gratis" in savedIds
                )
            )
            val newOnes = stores.filter { store ->
                store.createdAt.isNotBlank() && thirtyDaysAgo.isNotBlank() && store.createdAt >= thirtyDaysAgo
            }
            if (newOnes.isNotEmpty()) add(
                DiscoverCollection(
                    id = "novidades",
                    title = "Novidades",
                    subtitle = "${newOnes.size} ${if (newOnes.size == 1) "loja nova" else "lojas novas"}",
                    stores = newOnes.sortedByDescending { it.createdAt }.take(3),
                    backgroundColor = 0xFFF3E5F5,
                    saved = "novidades" in savedIds
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleCollectionSaved(id: String) {
        _collectionsSaved.value = if (id in _collectionsSaved.value)
            _collectionsSaved.value - id else _collectionsSaved.value + id
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

    private val _dailyDeals = MutableStateFlow<List<DailyDeal>>(emptyList())

    /** Ofertas do dia: cupons reais ativos; countdown só quando há expiração real. */
    val dailyDeals: StateFlow<List<DailyDeal>> = _dailyDeals.asStateFlow()

    init {
        viewModelScope.launch {
            _dailyDeals.value = try {
                val now = System.currentTimeMillis()
                SupabaseClient.fetchActiveCoupons(limit = 10).mapNotNull { coupon ->
                    if (coupon.code.isBlank()) return@mapNotNull null
                    val endsAt = parseIsoToMillis(coupon.expiresAt)
                    // Descarta cupom com expiração real já passada
                    if (coupon.expiresAt?.isNotBlank() == true && endsAt in 1 until now) return@mapNotNull null
                    DailyDeal(
                        id = coupon.id.ifBlank { coupon.code },
                        title = coupon.code.uppercase(),
                        description = couponDescriptionText(coupon),
                        discountBadge = couponBadgeText(coupon.discountType, coupon.discountValue),
                        endsAtMillis = endsAt,
                        storeId = coupon.storeId?.takeIf { it.isNotBlank() }
                    )
                }.take(6).mapIndexed { index, deal -> deal.copy(tall = index % 2 == 0) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

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
