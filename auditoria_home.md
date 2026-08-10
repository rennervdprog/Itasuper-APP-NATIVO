# Auditoria Técnica Máxima Atualizada: Tela Principal / Início (`/home`)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Seletor de Endereço (Vazio/Ação)** | "Adicione seu endereço" + Chip "Adicionar" | `{ viewModel.toggleEditNumber() }` |
| **Seletor de Endereço (Preenchido)** | `${uiState.streetName}, ${uiState.streetNumber}` + Ícone `Edit` | `{ viewModel.toggleEditNumber() }` |
| **Botão de Atendimento / Suporte** | Ícone `Icons.Outlined.HeadsetMic` | `{ viewModel.openSupportSheet() }` |
| **Barra de Pesquisa (Placeholder)** | "Buscar no ItaSuper..." | `{ onNavigateToSearch() }` |
| **Chips de Categoria** | "Todas", "Lanches", "Pizza", "Mercado", "Farmácia", "Bebidas" | `{ viewModel.onCategorySelect(category.id) }` |
| **Banner Promocional Dinâmico** | Imagem + Título + Descrição do Banner | `{ banner.targetStoreId?.let { storeId -> onNavigateToStore(storeId) } }` |
| **Cards da Seção Bento (Fallback)** | "Ofertas ItaSuper", "Sem taxa de serviço", "Entrega direta da loja" | Clique no card / botão de navegação para lojas |
| **Card de Último Pedido** | "Ver loja" / "Repetir pedido" | `onNavigateToStore(lastOrder.storeId)` / `{ viewModel.onRepeatLastOrder() }` |
| **Cards de Lojas Favoritas/Destaque** | Nome e imagem da loja destacada | `{ onNavigateToStore(store.id) }` |
| **Botão de Reagendamento / Tentar Novamente** | "Tentar novamente" (Em estado de erro) | `onRetry = viewModel::loadStores` |
| **Card da Lista de Lojas** (`StoreCardItem`) | Logo, Nome, Categoria, Avaliação, Entrega e Distância | `onClick = { onNavigateToStore(store.id) }` |
| **Botão "Falar pelo WhatsApp"** (Support Sheet) | "Falar pelo WhatsApp" (Atendimento oficial (21) 97123-4567) | `val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/5521971234567..."))` |
| **Botão "Central de Ajuda"** (Support Sheet) | "Central de Ajuda" | `{ onDismiss() }` |
| **Barra de Navegação Inferior** (`ItaSuperBottomNavBar`) | Ícones: Início, Busca, Pedidos, Perfil | `{ onNavigateToRoute(route) }` |

---

## 2. CÓDIGO LITERAL DE CADA FUNÇÃO

### Funções no `HomeViewModel.kt`:

```kotlin
package com.example.ui.home

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

data class HomeUiState(
    val streetName: String = "",
    val streetNumber: String = "",
    val isEditingNumber: Boolean = false,
    val selectedCategory: String = "todas",
    val searchQuery: String = "",
    val categories: List<CategoryItem> = StoreRepository.categories,
    val stores: List<Store> = emptyList(),
    val favoriteStores: List<Store> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val lastOrder: LastOrder? = null,
    val showSupportSheet: Boolean = false,
    val snackbarMessage: String? = null,
    val isRefreshingLocation: Boolean = false,
    val isLoadingStores: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val userSession = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            streetName = userSession.addressStreet,
            streetNumber = userSession.addressNumber,
            lastOrder = StoreRepository.lastOrder.value
        )

        // Observe stores flow reactively
        viewModelScope.launch {
            StoreRepository.stores.collect { updatedStores ->
                _uiState.value = _uiState.value.copy(
                    stores = updatedStores,
                    favoriteStores = updatedStores.take(2)
                )
                filterStores()
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
            val currentStores = StoreRepository.stores.value

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
            }
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            val remoteBanners = SupabaseClient.fetchBanners()
            _uiState.value = _uiState.value.copy(banners = remoteBanners)
        }
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
        val currentCategory = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()
        val allStores = StoreRepository.stores.value

        val filtered = allStores.filter { store ->
            val matchCategory = if (currentCategory == "todas") true else {
                store.category.equals(currentCategory, ignoreCase = true)
            }
            val matchQuery = if (query.isEmpty()) true else {
                store.name.lowercase().contains(query) || store.category.lowercase().contains(query)
            }
            matchCategory && matchQuery
        }

        _uiState.value = _uiState.value.copy(stores = filtered)
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
        _uiState.value = _uiState.value.copy(
            streetNumber = currentNumber,
            isEditingNumber = false,
            snackbarMessage = if (currentNumber.isNotBlank()) "Endereço atualizado com sucesso!" else "Por favor adicione seu endereço"
        )
    }

    fun refreshLocation() {
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = true
        )
        loadStores()
        _uiState.value = _uiState.value.copy(
            isRefreshingLocation = false,
            snackbarMessage = "Localização atualizada!"
        )
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

    fun onRepeatLastOrder() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "Itens do último pedido adicionados ao carrinho!")
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
```

### Funções no `StoreRepository.kt`:

```kotlin
package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StoreRepository {

    val categories = listOf(
        CategoryItem("todas", "Todas", "apps"),
        CategoryItem("lanches", "Lanches", "fastfood"),
        CategoryItem("pizza", "Pizza", "local_pizza"),
        CategoryItem("mercado", "Mercado", "shopping_cart"),
        CategoryItem("farmacia", "Farmácia", "medical_services"),
        CategoryItem("bebidas", "Bebidas", "local_bar")
    )

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    suspend fun refreshStoresFromSupabase(): Boolean {
        val activeStores = SupabaseClient.fetchActiveStores()
        _stores.value = activeStores
        return activeStores.isNotEmpty()
    }

    private val _lastOrder = MutableStateFlow<LastOrder?>(null)
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
```

---

## 3. TODA QUERY AO SUPABASE NESTA TELA

### Query 1: Lojas Ativas (`stores_public`)
- **View Consultada**: `stores_public`
- **Colunas Selecionadas**: `*` (todas)
- **Filtros Aplicados**: `status=eq.active&is_open=eq.true`
- **Ordenação (order by)**: `order=rating.desc`
- **Tipo**: Leitura (`GET`)
- **Código Kotlin Exato**:

```kotlin
val url = "$SUPABASE_URL/rest/v1/stores_public?select=*&status=eq.active&is_open=eq.true&order=rating.desc"

val request = Request.Builder()
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
```

---

### Query 2: Banners Promocionais (`banners`)
- **Tabela Consultada**: `banners`
- **Colunas Selecionadas**: `*` (todas)
- **Filtros Aplicados**: `is_active=eq.true`
- **Ordenação**: Nenhuma ordenação explícita enviada na URL
- **Tipo**: Leitura (`GET`)
- **Código Kotlin Exato**:

```kotlin
val url = "$SUPABASE_URL/rest/v1/banners?select=*&is_active=eq.true"

val request = Request.Builder()
    .url(url)
    .addHeader("apikey", SUPABASE_ANON_KEY)
    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
    .get()
    .build()

var response = httpClient.newCall(request).execute()
var responseText = response.body?.string() ?: ""

if (!response.isSuccessful) {
    val fallbackUrl = "$SUPABASE_URL/rest/v1/banners?select=*"
    val fallbackRequest = Request.Builder()
        .url(fallbackUrl)
        .addHeader("apikey", SUPABASE_ANON_KEY)
        .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
        .get()
        .build()
    response = httpClient.newCall(fallbackRequest).execute()
    responseText = response.body?.string() ?: ""
}
```

---

## 4. VALIDAÇÕES — CÓDIGO LITERAL

### Validação 1: Estado de Erro / Lojas Vazias na Tela
```kotlin
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
}
```

### Validação 2: Exibição do Endereço sem Hardcode de Cidade Fictícia
```kotlin
if (uiState.streetName.isBlank()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { viewModel.toggleEditNumber() }
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = ItaSuperPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Adicione seu endereço",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ItaSuperPrimary
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            color = ItaSuperPrimary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Adicionar",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
```

### Validação 3: Filtragem de Lojas em Memória
```kotlin
private fun filterStores() {
    val currentCategory = _uiState.value.selectedCategory
    val query = _uiState.value.searchQuery.trim().lowercase()
    val allStores = StoreRepository.stores.value

    val filtered = allStores.filter { store ->
        val matchCategory = if (currentCategory == "todas") true else {
            store.category.equals(currentCategory, ignoreCase = true)
        }
        val matchQuery = if (query.isEmpty()) true else {
            store.name.lowercase().contains(query) || store.category.lowercase().contains(query)
        }
        matchCategory && matchQuery
    }

    _uiState.value = _uiState.value.copy(stores = filtered)
}
```

---

## 5. O QUE NÃO EXISTE OU É INCERTO

1. **Dados Mocks de Lojas (`getMockStores()`)**:
   - **NÃO EXISTE / DELETADO COMPLETAMENTE**: A função e todos os dados fictícios foram excluídos do `StoreRepository.kt`. Se o Supabase falhar, o app mostra a mensagem legível ao usuário em vez de forjar dados.
2. **GPS em Tempo Real**:
   - **NÃO IMPLEMENTADO**: O app lê e altera o endereço da sessão do usuário (`UserSessionRepository`), mas não solicita permissões de localização do Android (`FusedLocationProviderClient`).

---

## 6. NAVEGAÇÃO

| Ação do Usuário | Rota Destino | Comportamento da Pilha (`NavController`) |
| :--- | :--- | :--- |
| Clique em "Buscar no ItaSuper..." | `"busca"` | `onNavigateToSearch()` -> `navController.navigate("busca")` |
| Clique em Banner Promocional | `"loja/{targetStoreId}"` | `onNavigateToStore(storeId)` -> `navController.navigate("loja/$storeId")` |
| Clique em Card da Lista de Lojas | `"loja/{storeId}"` | `onNavigateToStore(store.id)` -> `navController.navigate("loja/${store.id}")` |
| Clique na BottomNav "Início" | `"home"` | `onNavigateToRoute("home")` |
| Clique na BottomNav "Busca" | `"busca"` | `onNavigateToRoute("busca")` |
| Clique na BottomNav "Pedidos" | `"pedidos"` | `onNavigateToRoute("pedidos")` |
| Clique na BottomNav "Perfil" | `"perfil"` | `onNavigateToRoute("perfil")` |
