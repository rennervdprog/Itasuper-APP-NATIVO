# Auditoria Técnica Máxima: Tela Principal / Início (`/home`)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Seletor de Endereço** (`AddressSelectorBar`) | Exibe o endereço do usuário (ex: "Rua Central, 100 - Centro") | `onAddressClick()` (Navega para perfil ou abre seleção de endereço) |
| **Botão de Atendimento / Suporte** (`SupportIconButton`) | Ícone `Icons.Outlined.HeadsetMic` | `{ showSupportSheet = true }` |
| **Barra de Pesquisa (Placeholder)** (`SearchBarPlaceholder`) | "Buscar em mercados, farmácias e mais..." | `onSearchClick()` (Navega para a rota `"busca"`) |
| **Chips de Categoria** (`CategoriesFilterSection`) | "Todas", "Mercados", "Farmácias", "Hortifruti", "Bebidas", "Pet" | `onCategorySelected(category)` -> `viewModel.setCategoryFilter(category)` |
| **Banner Promocional** (`PromoBannersCarousel`) | Banners de ofertas ("Frete Grátis", "Super Promoção", etc.) | `onBannerClick(banner.targetStoreId)` -> `onStoreClick(id)` |
| **Card de Último Pedido** (`LastOrderCard`) | "Ver loja" / "Acompanhar" | `onStoreClick(order.storeId)` / `onOrderTrackerClick(order.id)` |
| **Cards de Lojas em Destaque** (`FeaturedStoresSection`) | Nome e imagem da loja destacada | `onStoreClick(store.id)` (Navega para `"loja/{store.id}"`) |
| **Chip de Filtro "Entrega Grátis"** | "Entrega Grátis" | `viewModel.toggleFreeDeliveryFilter()` |
| **Chip de Filtro "Abertos Agora"** | "Abertos" | `viewModel.toggleOpenOnlyFilter()` |
| **Chip de Ordenação** | "Ordenar por" | `{ showSortMenu = true }` |
| **Opção Menu "Nota"** | "Nota (maior para menor)" | `viewModel.setSortOption(SortOption.RATING)` |
| **Opção Menu "Distância"** | "Distância (mais próxima)" | `viewModel.setSortOption(SortOption.DISTANCE)` |
| **Opção Menu "Tempo de Entrega"** | "Tempo de entrega (mais rápido)" | `viewModel.setSortOption(SortOption.DELIVERY_TIME)` |
| **Card da Lista de Lojas** (`StoreItemCard`) | Card da loja com logo, nome, nota, distância e tempo | `onStoreClick(store.id)` -> `onNavigateToRoute("loja/${store.id}")` |
| **Botão "Chamar no WhatsApp"** (Support Sheet) | "Chamar no WhatsApp" | Abre `Intent.ACTION_VIEW` com URI `https://wa.me/5521999999999...` |
| **Botão "Central de Ajuda"** (Support Sheet) | "Central de Ajuda" | Exibe Toast / Feedback na tela |
| **Botão "Fechar"** (Support Sheet) | Ícone fechar | `onDismiss()` |
| **Barra de Navegação Inferior** (`ItaSuperBottomNavBar`) | Ícones: Início, Busca, Pedidos, Perfil | `onNavigateToRoute(route)` |

---

## 2. CÓDIGO LITERAL DE CADA FUNÇÃO

### Functions no `HomeViewModel.kt`:

```kotlin
package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption {
    DEFAULT,
    RATING,
    DELIVERY_TIME,
    DISTANCE
}

data class HomeUiState(
    val selectedCategory: String = "Todas",
    val selectedSortOption: SortOption = SortOption.DEFAULT,
    val isFreeDeliveryOnly: Boolean = false,
    val isOpenOnly: Boolean = false,
    val searchQuery: String = "",
    val isLoadingStores: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val userAddress: StateFlow<String> = MutableStateFlow("")

    val stores: StateFlow<List<Store>> = StoreRepository.stores

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStores = true)
            StoreRepository.fetchStoresFromRemote()
            _uiState.value = _uiState.value.copy(isLoadingStores = false)
        }
    }

    fun setCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(selectedSortOption = sortOption)
    }

    fun toggleFreeDeliveryFilter() {
        _uiState.value = _uiState.value.copy(isFreeDeliveryOnly = !_uiState.value.isFreeDeliveryOnly)
    }

    fun toggleOpenOnlyFilter() {
        _uiState.value = _uiState.value.copy(isOpenOnly = !_uiState.value.isOpenOnly)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
```

### Funções no `StoreRepository.kt`:

```kotlin
package com.example.data.repository

import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StoreRepository {

    private val _stores = MutableStateFlow<List<Store>>(getMockStores())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    suspend fun fetchStoresFromRemote() {
        val remoteStores = SupabaseClient.fetchStores()
        if (remoteStores.isNotEmpty()) {
            _stores.value = remoteStores
        }
    }

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }

    private fun getMockStores(): List<Store> {
        return listOf(
            Store(
                id = "s1",
                name = "Supermercado Guanabara",
                category = "Mercados",
                rating = 4.8,
                deliveryTime = "30-45 min",
                deliveryFee = 5.99,
                isFreeDelivery = false,
                isOpen = true,
                distanceKm = 1.2,
                imageUrl = "https://picsum.photos/seed/guanabara/400/250"
            ),
            Store(
                id = "s2",
                name = "Farmácia Pacheco",
                category = "Farmácias",
                rating = 4.9,
                deliveryTime = "15-25 min",
                deliveryFee = 0.0,
                isFreeDelivery = true,
                isOpen = true,
                distanceKm = 0.8,
                imageUrl = "https://picsum.photos/seed/pacheco/400/250"
            ),
            Store(
                id = "s3",
                name = "Hortifruti Natural da Terra",
                category = "Hortifruti",
                rating = 4.7,
                deliveryTime = "25-35 min",
                deliveryFee = 4.50,
                isFreeDelivery = false,
                isOpen = true,
                distanceKm = 2.1,
                imageUrl = "https://picsum.photos/seed/hortifruti/400/250"
            ),
            Store(
                id = "s4",
                name = "Adega & Bebidas Itaboraí",
                category = "Bebidas",
                rating = 4.6,
                deliveryTime = "20-30 min",
                deliveryFee = 0.0,
                isFreeDelivery = true,
                isOpen = false,
                distanceKm = 3.5,
                imageUrl = "https://picsum.photos/seed/adega/400/250"
            )
        )
    }
}
```

### Ação no `SupportBottomSheet.kt`:

```kotlin
Button(
    onClick = {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/5521999999999?text=Olá,%20preciso%20de%20ajuda%20no%20ItaSuper!")
        )
        context.startActivity(intent)
    },
    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("support_whatsapp_button")
) {
    Icon(
        imageVector = Icons.Default.Chat,
        contentDescription = null,
        tint = Color.White
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("Chamar no WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
```

---

## 3. TODA QUERY AO SUPABASE NESTA TELA

### Tabela / Endpoint Consultado:
- **Tabela**: `stores`
- **Colunas selecionadas**: `*` (Todas as colunas via `GET /rest/v1/stores?select=*`)
- **Filtros aplicados**: Nenhum filtro SQL remoto no endpoint de consulta inicial (filtra na memória via Kotlin Coroutines / Flow).
- **Ordenação (order by)**: Nenhuma na query HTTP. Ordenação é realizada em memória Kotlin.
- **Tipo de requisição**: Leitura (`GET`).

### Código Kotlin LITERAL no `SupabaseClient.kt`:

```kotlin
// 3. FETCH STORES LIST
suspend fun fetchStores(): List<Store> = withContext(Dispatchers.IO) {
    try {
        val url = "$SUPABASE_URL/rest/v1/stores?select=*"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val jsonText = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(jsonText)
            val list = mutableListOf<Store>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Store(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", "Loja"),
                        category = obj.optString("category", "Mercados"),
                        rating = obj.optDouble("rating", 4.5),
                        deliveryTime = obj.optString("delivery_time", "20-30 min"),
                        deliveryFee = obj.optDouble("delivery_fee", 0.0),
                        isFreeDelivery = obj.optBoolean("is_free_delivery", false),
                        isOpen = obj.optBoolean("is_open", true),
                        distanceKm = obj.optDouble("distance_km", 1.0),
                        imageUrl = obj.optString("image_url", ""),
                        bannerUrl = obj.optString("banner_url", "")
                    )
                )
            }
            list
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching stores", e)
        emptyList()
    }
}
```

---

## 4. VALIDAÇÕES — CÓDIGO LITERAL

Código literal de filtragem e ordenação no `HomeScreen.kt`:

```kotlin
val filteredStores = stores.filter { store ->
    val matchesCategory = uiState.selectedCategory == "Todas" || store.category.equals(uiState.selectedCategory, ignoreCase = true)
    val matchesFreeDelivery = !uiState.isFreeDeliveryOnly || store.isFreeDelivery || store.deliveryFee == 0.0
    val matchesOpen = !uiState.isOpenOnly || store.isOpen
    val matchesSearch = uiState.searchQuery.isBlank() || store.name.contains(uiState.searchQuery, ignoreCase = true)

    matchesCategory && matchesFreeDelivery && matchesOpen && matchesSearch
}.let { list ->
    when (uiState.selectedSortOption) {
        SortOption.RATING -> list.sortedByDescending { it.rating }
        SortOption.DELIVERY_TIME -> list.sortedBy { it.deliveryTime }
        SortOption.DISTANCE -> list.sortedBy { it.distanceKm }
        SortOption.DEFAULT -> list
    }
}
```

Código literal de montagem da string de endereço do usuário:

```kotlin
val userSession by UserSessionRepository.userSession.collectAsState()
val displayAddress = if (userSession.addressStreet.isNotBlank()) {
    "${userSession.addressStreet}, ${userSession.addressNumber} - ${userSession.addressNeighborhood}"
} else {
    "Rua Central, 100 - Centro, Itaboraí"
}
```

---

## 5. O QUE NÃO EXISTE OU É INCERTO

1. **Localização em Tempo Real via GPS (FusedLocationProviderClient)**:
   - **NÃO IMPLEMENTADO**: O app não faz leitura do GPS do dispositivo nesta tela; o endereço exibido no topo vem da sessão local (`UserSessionRepository`) ou do mock estático.
2. **Banners Promocionais Dinâmicos do Supabase**:
   - **NÃO IMPLEMENTADO**: Os banners da seção de carrossel são definidos de forma estática no próprio arquivo `HomeScreen.kt`.
3. **Telefone do Suporte WhatsApp**:
   - **NÃO TENHO CERTEZA**: O link do WhatsApp utiliza o número genérico `5521999999999`. É funcional no sentido de disparar a Intent do navegador/WhatsApp, mas não aponta para uma conta de atendimento oficial cadastrada no banco.

---

## 6. NAVEGAÇÃO

| Ação do Usuário | Rota Destino | Comportamento da Pilha (`NavController`) |
| :--- | :--- | :--- |
| Clique em Barra de Pesquisa | `"busca"` | `navController.navigate("busca")` (mantém tela de início na pilha para retorno) |
| Clique em Card de Loja / Banner | `"loja/{storeId}"` | `navController.navigate("loja/$storeId")` |
| Clique em "Acompanhar Pedido" | `"pedidos"` | `navController.navigate("pedidos")` |
| Clique na BottomNav "Início" | `"home"` | `popUpTo("home") { saveState = true }`, `launchSingleTop = true`, `restoreState = true` |
| Clique na BottomNav "Busca" | `"busca"` | `navController.navigate("busca")` |
| Clique na BottomNav "Pedidos" | `"pedidos"` | `navController.navigate("pedidos")` |
| Clique na BottomNav "Perfil" | `"perfil"` | `navController.navigate("perfil")` |
