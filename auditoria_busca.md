# Auditoria Técnica Máxima: Tela de Busca (`/busca` — SearchScreen.kt)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Botão de Atualizar Localização** | Ícone `Icons.Default.Refresh` / `CircularProgressIndicator` | `onClick = viewModel::refreshLocation` |
| **Campo de Entrada de Busca** | Placeholder: "Buscar por loja, prato ou mercado..." | `onValueChange = viewModel::onQueryChange`, `onSearch = { focusManager.clearFocus() }` |
| **Botão Limpar Busca** | Ícone `Icons.Default.Clear` | `onClick = viewModel::clearQuery` |
| **Chips de Categoria** | "Todas", "Lanches", "Pizza", "Mercado", "Farmácia", "Bebidas" | `onClick = { viewModel.onCategorySelect(cat.id) }` |
| **Botão "Limpar" Buscas Recentes** | TextButton: "Limpar" | `onClick = viewModel::clearRecentSearches` |
| **Chips de Buscas Recentes** | Termos (ex: "Pastel", "Pizza", "Mercado", "Cerveja", etc.) | `onClick = { viewModel.onRecentSearchSelect(term) }` |
| **Botão "Ver todas as lojas"** (Estado Vazio) | "Ver todas as lojas" | `onClick = { viewModel.clearQuery(); viewModel.onCategorySelect("todas") }` |
| **Card de Resultado da Loja** | Nome, categoria, avaliação, tempo e taxa | `onClick = { onNavigateToStore(store.id) }` |
| **Barra de Navegação Inferior** (`ItaSuperBottomNavBar`) | Ícones: Início, Busca, Pedidos, Perfil | `onNavigateToRoute = onNavigateToRoute` |

---

## 2. CÓDIGO LITERAL DE CADA FUNÇÃO

### Arquivo Completo: `SearchViewModel.kt`

```kotlin
package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategoryId: String = "todas",
    val recentSearches: List<String> = listOf("Pastel", "Pizza", "Mercado", "Cerveja", "Hambúrguer", "Farmácia"),
    val isRefreshingLocation: Boolean = false,
    val address: String = "Rodovia Amaral Peixoto, 100"
)

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val categories: List<CategoryItem> = StoreRepository.categories

    val filteredStores: StateFlow<List<Store>> = combine(
        StoreRepository.stores,
        _uiState
    ) { stores, state ->
        val query = state.query.trim().lowercase()
        val categoryId = state.selectedCategoryId.lowercase()

        stores.filter { store ->
            val matchesCategory = if (categoryId == "todas") {
                true
            } else {
                val catObj = categories.find { it.id == categoryId }
                val catName = catObj?.name ?: categoryId
                store.category.equals(catName, ignoreCase = true) ||
                        store.category.lowercase().contains(categoryId)
            }

            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                store.name.lowercase().contains(query) ||
                        store.category.lowercase().contains(query)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.isNotBlank() && !_uiState.value.recentSearches.contains(newQuery.trim())) {
            // Keep recent searches updated with typed queries when saved
        }
    }

    fun clearQuery() {
        _uiState.value = _uiState.value.copy(query = "")
    }

    fun onCategorySelect(categoryId: String) {
        val newCategory = if (_uiState.value.selectedCategoryId == categoryId && categoryId != "todas") {
            "todas"
        } else {
            categoryId
        }
        _uiState.value = _uiState.value.copy(selectedCategoryId = newCategory)
    }

    fun onRecentSearchSelect(term: String) {
        _uiState.value = _uiState.value.copy(query = term)
    }

    fun clearRecentSearches() {
        _uiState.value = _uiState.value.copy(recentSearches = emptyList())
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingLocation = true)
            delay(1000)
            _uiState.value = _uiState.value.copy(
                isRefreshingLocation = false,
                address = "Rua Central, 250 (Atualizado)"
            )
        }
    }
}
```

---

## 3. TODA QUERY AO SUPABASE NESTA TELA

A tela de busca consome diretamente a lista reativa `StoreRepository.stores`. As lojas nessa lista são originadas e mantidas atualizadas a partir da query ao Supabase executada no `SupabaseClient.kt`:

- **View Consultada**: `stores_public`
- **Colunas Selecionadas**: `*` (todas as colunas)
- **Filtros Aplicados**: `status=eq.active&is_open=eq.true` (com fallback sem o parâmetro `status=eq.active` para prevenir o erro do enum Postgres `22P02`)
- **Ordenação (order by)**: `order=rating.desc`
- **Tipo de Operação**: Leitura (`GET` / REST HTTP)
- **Código Kotlin Exato (`SupabaseClient.kt`)**:

```kotlin
suspend fun fetchActiveStores(): List<Store> = withContext(Dispatchers.IO) {
    try {
        var url = "$SUPABASE_URL/rest/v1/stores_public?select=*&status=eq.active&is_open=eq.true&order=rating.desc"

        var request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        var response = httpClient.newCall(request).execute()
        var responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val fallbackUrl = "$SUPABASE_URL/rest/v1/stores_public?select=*&is_open=eq.true&order=rating.desc"
            val fallbackRequest = Request.Builder()
                .url(fallbackUrl)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()
            response = httpClient.newCall(fallbackRequest).execute()
            responseText = response.body?.string() ?: ""
        }

        if (response.isSuccessful && responseText.isNotBlank()) {
            val jsonArray = JSONArray(responseText)
            val storeList = mutableListOf<Store>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "Loja sem nome")
                val category = obj.optString("category", "Geral")
                val logoUrl = obj.optString("logo_url", obj.optString("banner_url", ""))
                val rating = obj.optDouble("rating", 5.0)
                val deliveryTime = obj.optString("delivery_time", "30-45 min")
                val deliveryFee = obj.optString("delivery_fee", "Grátis")
                val distanceKm = obj.optDouble("distance_km", 1.2)
                val isFreeDelivery = deliveryFee.contains("Grátis", ignoreCase = true) || deliveryFee == "R$ 0,00"

                if (id.isNotBlank()) {
                    storeList.add(
                        Store(
                            id = id,
                            name = name,
                            category = category,
                            logoUrl = logoUrl,
                            rating = rating,
                            deliveryTime = deliveryTime,
                            deliveryFee = deliveryFee,
                            isFreeDelivery = isFreeDelivery,
                            distanceKm = distanceKm
                        )
                    )
                }
            }
            storeList
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching active stores", e)
        emptyList()
    }
}
```

---

## 4. VALIDAÇÕES — CÓDIGO LITERAL

### Validação 1: Filtragem Combinada por Categoria e Texto em Tempo Real (`SearchViewModel.kt`)
```kotlin
val filteredStores: StateFlow<List<Store>> = combine(
    StoreRepository.stores,
    _uiState
) { stores, state ->
    val query = state.query.trim().lowercase()
    val categoryId = state.selectedCategoryId.lowercase()

    stores.filter { store ->
        val matchesCategory = if (categoryId == "todas") {
            true
        } else {
            val catObj = categories.find { it.id == categoryId }
            val catName = catObj?.name ?: categoryId
            store.category.equals(catName, ignoreCase = true) ||
                    store.category.lowercase().contains(categoryId)
        }

        val matchesQuery = if (query.isEmpty()) {
            true
        } else {
            store.name.lowercase().contains(query) ||
                    store.category.lowercase().contains(query)
        }

        matchesCategory && matchesQuery
    }
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
)
```

### Validação 2: Toggle Dinâmico de Seleção de Categoria (`SearchViewModel.kt`)
```kotlin
fun onCategorySelect(categoryId: String) {
    val newCategory = if (_uiState.value.selectedCategoryId == categoryId && categoryId != "todas") {
        "todas"
    } else {
        categoryId
    }
    _uiState.value = _uiState.value.copy(selectedCategoryId = newCategory)
}
```

### Validação 3: Exibição Condicional do Estado Vazio (`SearchScreen.kt`)
```kotlin
if (stores.isEmpty()) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = ItaSuperHighlightBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nenhuma loja encontrada",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.query.isNotBlank()) {
                    "Não encontramos resultados para \"${uiState.query}\". Tente buscar por outros termos ou selecione outra categoria."
                } else {
                    "Nenhuma loja disponível para o filtro selecionado."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ItaSuperTextSecondary
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.clearQuery()
                    viewModel.onCategorySelect("todas")
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                modifier = Modifier.testTag("reset_search_button")
            ) {
                Text("Ver todas as lojas")
            }
        }
    }
}
```

---

## 5. O QUE NÃO EXISTE OU É INCERTO

1. **Persistência de Buscas Recentes**:
   - **NÃO IMPLEMENTADO**: A lista `recentSearches` reside exclusivamente em memória no `SearchUiState` com itens padrão fictícios e não é gravada em `SharedPreferences`, `Room` ou `Supabase`.
2. **Leitura Real de GPS no `refreshLocation()`**:
   - **NÃO IMPLEMENTADO**: A função `refreshLocation()` executa apenas um `delay(1000)` suspend coroutine e atualiza a String em memória para `"Rua Central, 250 (Atualizado)"` sem solicitar permissões nem acessar hardware de GPS.
3. **Busca por Produtos Indivíduais / Itens de Cardápio**:
   - **NÃO IMPLEMENTADO**: O filtro atual (`matchesQuery`) pesquisa exclusivamente no campo `store.name` e `store.category`. Não há consulta remota à tabela `products` durante a digitação na busca.

---

## 6. NAVEGAÇÃO

| Ação do Usuário | Rota Destino | Comportamento da Pilha (`NavController`) |
| :--- | :--- | :--- |
| Clique em um Card de Loja (`SearchStoreCard`) | `"loja/{storeId}"` | Executa `onNavigateToStore(store.id)` -> `navController.navigate("loja/$storeId")` sem limpar a pilha. |
| Clique nos itens da BottomNav | `"home"`, `"busca"`, `"pedidos"`, `"perfil"` | Executa `onNavigateToRoute(route)` -> `navController.navigate(route)` com verificação de rota corrente. |
