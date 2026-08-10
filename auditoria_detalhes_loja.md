# Auditoria Técnica Máxima: Tela de Detalhes da Loja (`loja/{storeId}` — StoreDetailScreen.kt)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Botão Voltar (TopBar)** | Ícone `Icons.AutoMirrored.Filled.ArrowBack` | `onClick = onBackClick` -> `navController.popBackStack()` |
| **Botão Favoritar (TopBar)** | Ícone `Icons.Default.Favorite` / `FavoriteBorder` | `onClick = { isFavorite = !isFavorite }` (Estado Compose `remember`) |
| **Botão "Ver Sacola" (Barra Inferior Flutuante)** | "Ver Sacola" + Quantidade e Subtotal | `onClick = onNavigateToCart` -> `navController.navigate("carrinho")` |
| **Campo de Busca no Cardápio** | Placeholder: "Buscar no cardápio de {nome_da_loja}..." | `onValueChange = { viewModel.onSearchQueryChange(it) }` |
| **Botão Limpar Busca no Cardápio** | Ícone `Icons.Default.Clear` | `onClick = { viewModel.onSearchQueryChange("") }` |
| **Chips de Categoria (`FilterChip`)** | Nome da categoria (ex: "Todos", "Lanches", "Bebidas") | `onClick = { viewModel.selectCategory(cat) }` |
| **Card de Produto (`ProductItemCard`)** | Nome, Descrição, Preço e Imagem do Produto | `clickable { onCardClick() }` -> `viewModel.openProductModal(product)` |
| **Botão "Adicionar Rápido" (`+`) no Card** | Ícone `Icons.Default.Add` em círculo primário | `onClick = onAddClick` -> `viewModel.addDirectProductToCart(product)` |
| **Fechar Modal BottomSheet** | Gesto de arrastar para baixo ou toque fora | `onDismissRequest = { viewModel.closeProductModal() }` |
| **Campo de Observações no Modal** | Label: "Alguma observação?" | `onValueChange = onNotesChange` -> `viewModel.updateModalNotes(it)` |
| **Botão Diminuir Quantidade no Modal (`-`)** | Ícone `Icons.Default.Remove` | `onClick = onQuantityDecrement` -> `viewModel.decrementModalQuantity()` |
| **Botão Aumentar Quantidade no Modal (`+`)** | Ícone `Icons.Default.Add` | `onClick = onQuantityIncrement` -> `viewModel.incrementModalQuantity()` |
| **Botão Adicionar ao Carrinho no Modal** | "Adicionar • R$ XX,XX" | `onClick = onAddToCartClick` -> `viewModel.addSelectedProductToCart()` |

---

## 2. CÓDIGO LITERAL DE CADA FUNÇÃO

### Arquivo: `StoreDetailViewModel.kt`

```kotlin
fun loadStore(storeId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    viewModelScope.launch {
        // Find store in repository or fetch refreshed list
        val storeList = StoreRepository.stores.value
        val store = storeList.find { it.id == storeId } ?: storeList.firstOrNull()

        // Fetch products from Supabase
        val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
        _rawProducts.value = remoteProducts

        val categoryList = if (remoteProducts.isNotEmpty()) {
            val list = mutableListOf("Todos")
            list.addAll(remoteProducts.map { it.category }.distinct())
            list
        } else {
            emptyList()
        }

        _uiState.value = _uiState.value.copy(
            store = store,
            isLoading = false,
            categories = categoryList,
            selectedCategory = "Todos"
        )
    }
}

fun selectCategory(category: String) {
    _uiState.value = _uiState.value.copy(selectedCategory = category)
}

fun onSearchQueryChange(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
}

fun openProductModal(product: Product) {
    _uiState.value = _uiState.value.copy(
        selectedProductForModal = product,
        modalQuantity = 1,
        modalNotes = ""
    )
}

fun closeProductModal() {
    _uiState.value = _uiState.value.copy(selectedProductForModal = null)
}

fun incrementModalQuantity() {
    _uiState.value = _uiState.value.copy(modalQuantity = _uiState.value.modalQuantity + 1)
}

fun decrementModalQuantity() {
    if (_uiState.value.modalQuantity > 1) {
        _uiState.value = _uiState.value.copy(modalQuantity = _uiState.value.modalQuantity - 1)
    }
}

fun updateModalNotes(notes: String) {
    _uiState.value = _uiState.value.copy(modalNotes = notes)
}

fun addSelectedProductToCart() {
    val product = _uiState.value.selectedProductForModal ?: return
    val storeName = _uiState.value.store?.name ?: "Loja"
    CartRepository.addProduct(
        product = product,
        storeName = storeName,
        quantity = _uiState.value.modalQuantity,
        notes = _uiState.value.modalNotes
    )
    closeProductModal()
}

fun addDirectProductToCart(product: Product) {
    val storeName = _uiState.value.store?.name ?: "Loja"
    CartRepository.addProduct(
        product = product,
        storeName = storeName,
        quantity = 1
    )
}
```

### Arquivo: `CartRepository.kt`

```kotlin
fun addProduct(product: Product, storeName: String, quantity: Int = 1, notes: String = "") {
    val current = _cartState.value
    
    // If adding from a different store, reset cart to new store
    if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
        _cartState.value = CartState(
            storeId = product.storeId,
            storeName = storeName,
            items = listOf(CartItem(product, quantity, notes))
        )
        return
    }

    val existingIndex = current.items.indexOfFirst { it.product.id == product.id }
    val updatedItems = current.items.toMutableList()

    if (existingIndex >= 0) {
        val existing = updatedItems[existingIndex]
        val newQty = existing.quantity + quantity
        updatedItems[existingIndex] = existing.copy(quantity = newQty, notes = notes)
    } else {
        updatedItems.add(CartItem(product, quantity, notes))
    }

    _cartState.value = current.copy(
        storeId = product.storeId,
        storeName = storeName,
        items = updatedItems
    )
}
```

---

## 3. TODA QUERY AO SUPABASE NESTA TELA

1. **Query de Produtos da Loja (`products`)**:
   - **Tabela/View Consultada**: `products`
   - **Colunas Selecionadas**: `*` (todas as colunas da tabela `products`)
   - **Filtros Aplicados**: `store_id=eq.$storeId` (onde `$storeId` é o ID da loja recebido pela rota)
   - **Ordenação (order by)**: Nenhuma explicita na URL REST
   - **Tipo de Operação**: Leitura (GET / select)
   - **Código Kotlin Exato da Chamada**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/products?store_id=eq.$storeId"

     val request = Request.Builder()
         .url(url)
         .addHeader("apikey", SUPABASE_ANON_KEY)
         .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
         .addHeader("Content-Type", "application/json")
         .get()
         .build()

     val response = httpClient.newCall(request).execute()
     ```

2. **Obtenção dos Dados da Loja (`stores_public`)**:
   - **Tabela/View Consultada**: `stores_public` (via `StoreRepository.stores.value`)
   - **Tipo de Operação**: Leitura (GET / select) de lojas em cache reativo no `StoreRepository`.

---

## 4. VALIDAÇÕES — CÓDIGO LITERAL

1. **Validação de Quantidade Mínima no Modal (Não permite menor que 1)**:
   ```kotlin
   fun decrementModalQuantity() {
       if (_uiState.value.modalQuantity > 1) {
           _uiState.value = _uiState.value.copy(modalQuantity = _uiState.value.modalQuantity - 1)
       }
   }
   ```

2. **Validação de Produto Selecionado ao Confirmar Adição no Modal**:
   ```kotlin
   fun addSelectedProductToCart() {
       val product = _uiState.value.selectedProductForModal ?: return
       // ...
   }
   ```

3. **Validação de Conflito de Lojas no Carrinho (Troca automática ao adicionar de loja diferente)**:
   ```kotlin
   if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
       _cartState.value = CartState(
           storeId = product.storeId,
           storeName = storeName,
           items = listOf(CartItem(product, quantity, notes))
       )
       return
   }
   ```

4. **Filtro Combinado de Produtos por Categoria e Busca Textual**:
   ```kotlin
   val query = state.searchQuery.trim().lowercase()
   val category = state.selectedCategory

   products.filter { product ->
       val matchesCategory = if (category == "Todos") true else product.category.equals(category, ignoreCase = true)
       val matchesQuery = if (query.isBlank()) true else {
           product.name.lowercase().contains(query) || product.description.lowercase().contains(query)
       }
       matchesCategory && matchesQuery
   }
   ```

---

## 5. O QUE NÃO EXISTE OU É INCERTO

1. **Favoritar Loja (`isFavorite`)**: **NÃO PERSISTIDO NO BACKEND**. O estado de favorito nesta tela é mantido apenas em memória local do Composable via `remember { mutableStateOf(false) }` e é perdido ao fechar a tela.
2. **Opções/Complementos de Produtos (Opções de Pizza, Borda, Molhos, etc.)**: **NÃO IMPLEMENTADO**. Apenas observação em texto livre (`modalNotes`).
3. **Avaliação Detalhada e Comentários da Loja**: **NÃO IMPLEMENTADO**. Exibe apenas a nota média geral (`rating`) vinda de `stores_public`.

---

## 6. NAVEGAÇÃO

1. **Botão de Voltar (ArrowBack)**:
   - **Rota**: `onBackClick` invoca `navController.popBackStack()`
   - **Pilha de Navegação**: Remove a tela `loja/{storeId}` do topo da pilha de navegação e retorna à tela anterior (Home ou Busca).

2. **Botão "Ver Sacola" (`view_cart_button`)**:
   - **Rota**: `onNavigateToCart` invoca `navController.navigate("carrinho")`
   - **Pilha de Navegação**: Adiciona a tela de carrinho (`carrinho`) no topo da pilha de navegação.

3. **Navegação de Entrada para esta tela**:
   - **Origem**: `HomeScreen` ou `SearchScreen` via `onNavigateToStore = { storeId -> navController.navigate("loja/$storeId") }`.
