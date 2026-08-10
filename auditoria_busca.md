# Auditoria Técnica Máxima: Tela de Busca (`/busca` — SearchScreen.kt)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Campo de Entrada de Busca** | Placeholder: "Buscar por loja, prato ou mercado..." | `onValueChange = viewModel::onQueryChange`, `onSearch = { viewModel.submitSearch(uiState.rawQuery); focusManager.clearFocus() }` |
| **Botão Limpar Busca** | Ícone `Icons.Default.Clear` | `onClick = viewModel::clearQuery` |
| **Botão "Ative sua localização"** | Ícone `Icons.Outlined.LocationOn` | `onClick = { permissionLauncher.launch(...) }` (Solicita permissões de GPS reais `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`) |
| **Chips de Buscas Recentes** | Termos recentes persistidos no DataStore | `onClick = { viewModel.onRecentSearchSelect(term) }` |
| **Botão "Limpar" Buscas Recentes** | TextButton: "Limpar" | `onClick = viewModel::clearRecentSearches` |
| **Cards de Categoria (Grid 2 colunas)** | "Lanches", "Pizzaria", "Marmita", "Açaí & Sobremesas", "Bebidas", "Mercado", "Pastel & Salgados", "Churrasco" | `onClick = { viewModel.onCategorySelect(category.id) }` |
| **Chip de Filtro Ativo** | "Categoria: {nome}" com Ícone `Close` | `onClick = { viewModel.clearCategory() }` |
| **Cards de Loja "Em Alta"** | Logo/Banner, Nome, Categoria, Rating, Distância Haversine | `onClick = { onNavigateToStore(store.id) }` |
| **Cards de Loja "Novidades"** | Logo/Banner, Nome, Categoria, Badge "NOVO" | `onClick = { onNavigateToStore(store.id) }` |
| **Card de Resultado da Loja** | Nome, Categoria, Rating, Distância Haversine, Badge "FECHADA" (se fechada) | `onClick = { onNavigateToStore(store.id) }` |
| **Botão "Ver todas as opções"** (Estado Vazio) | Button: "Ver todas as opções" | `onClick = { viewModel.clearQuery(); viewModel.clearCategory() }` |
| **Barra de Navegação Inferior** (`ItaSuperBottomNavBar`) | Ícones: Início, Busca, Pedidos, Perfil | `onNavigateToRoute = onNavigateToRoute` |

---

## 2. CÓDIGO LITERAL DE CADA FUNÇÃO

### Arquivo Completo: `SearchViewModel.kt`

```kotlin
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

    // Seção "Em Alta" (lojas abertas com foto, ordenadas por rating desc, máx 8)
    val trendingStores: StateFlow<List<Store>> = StoreRepository.stores.map { stores ->
        stores.filter { it.isOpen && (it.logoUrl.isNotBlank() || it.bannerUrl.isNotBlank()) }
            .sortedByDescending { it.rating }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Seção "Novidades" (lojas com created_at nos últimos 30 dias ou mais recentes, máx 8)
    val newStores: StateFlow<List<Store>> = StoreRepository.stores.map { stores ->
        val thirtyDaysAgo = try {
            Instant.now().minus(30, ChronoUnit.DAYS).toString()
        } catch (e: Exception) {
            ""
        }

        val filteredByDate = stores.filter { store ->
            if (store.createdAt.isNotBlank() && thirtyDaysAgo.isNotBlank()) {
                store.createdAt >= thirtyDaysAgo
            } else true
        }

        val resultList = if (filteredByDate.size >= 2) filteredByDate else stores
        resultList.sortedByDescending { it.createdAt }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Result List for Search Mode
    val filteredStores: StateFlow<List<Store>> = combine(
        StoreRepository.stores,
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
```

---

## 3. TODA QUERY AO SUPABASE NESTA TELA

A tela de busca consome reativamente as lojas e horários de funcionamento do Supabase em `SupabaseClient.kt`:

1. **Query Principal de Lojas**:
   - **View Consultada**: `stores_public`
   - **Filtro de Status**: `status=eq.ativo` (com fallback sem o parâmetro `status` para tratar variações do enum Postgres)
   - **Filtro Backend de `is_open`**: **NÃO FILTRADO NO BACKEND**. Trará todas as lojas com `status=ativo`.
   - **Ordenação**: `order=rating.desc`

2. **Query da Tabela de Horários (`opening_hours`)**:
   - **Tabela Consultada**: `opening_hours`
   - **Colunas**: `store_id`, `day_of_week`, `open_time`, `close_time`, `is_closed_all_day`
   - **Cálculo de Aberto/Fechado no Cliente**:
     - O cliente obtém o dia da semana e minuto atual (`Calendar.getInstance()`).
     - Compara com os horários da loja obtidos de `opening_hours`.
     - Define `isOpen = true/false` para cada loja. Lojas fechadas aparecem na lista em tom cinza com o badge `"FECHADA"`.

---

## 4. RECURSOS IMPLEMENTADOS E CONFIRMADOS

1. **Persistência de Buscas Recentes com DataStore**:
   - Persistência real utilizando `androidx.datastore.preferences.core`.
   - Salva até no máximo 5 termos distintos.
   - Salva apenas quando o usuário confirma a busca (Submit/Enter), não a cada tecla digitada.

2. **GPS e Cálculo de Distância via Haversine**:
   - Botão "Ative sua localização" exibido somente quando não há coordenadas registradas.
   - Solicita permissões nativas de GPS (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
   - Distância calculada em tempo real com a fórmula de Haversine (`calculateHaversineDistanceKm`).

3. **Duas Estruturas de Exibição (Modos)**:
   - **MODO PADRÃO** (Sem busca ativada e sem categoria selecionada):
     - Buscas Recentes (se houver)
     - Grid de Categorias (2 colunas, 8 categorias exatas)
     - Seção "Em Alta" (lojas abertas com foto, ordenadas por rating desc, scroll horizontal, máx 8)
     - Seção "Novidades" (lojas criadas nos últimos 30 dias / mais recentes, máx 8)
   - **MODO RESULTADO** (Busca com 2+ caracteres OU categoria selecionada):
     - Lista vertical de lojas filtradas por nome, categoria e termos normalizados.
     - Debounce de 250ms e remoção de acentos/diacríticos.
