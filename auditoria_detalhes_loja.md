# Auditoria Técnica Atualizada: Tela de Detalhes da Loja (`loja/{storeId}` — StoreDetailScreen.kt)

---

## 1. TODOS OS ELEMENTOS INTERATIVOS

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Botão Voltar (TopBar)** | Ícone `Icons.AutoMirrored.Filled.ArrowBack` | `onClick = onBackClick` -> `navController.popBackStack()` |
| **Botão Favoritar Loja** | *Removido* | *Removido da interface (conforme regra de paridade)* |
| **Botão "Ver Sacola" (Barra Inferior Flutuante)** | "Ver Sacola" + Quantidade e Subtotal | `onClick = onNavigateToCart` -> `navController.navigate("carrinho")` |
| **Campo de Busca no Cardápio** | Placeholder: "Buscar no cardápio de {nome_da_loja}..." | `onValueChange = { viewModel.onSearchQueryChange(it) }` |
| **Botão Limpar Busca no Cardápio** | Ícone `Icons.Default.Clear` | `onClick = { viewModel.onSearchQueryChange("") }` |
| **Chips de Seção do Cardápio (`FilterChip`)** | Nome da seção (ex: "Todos" + seções reais da tabela `menu_sections`) | `onClick = { viewModel.selectSection(secName) }` |
| **Card de Produto (`ProductItemCard`)** | Nome, Descrição, Preço e Imagem do Produto | `clickable { onCardClick() }` -> `viewModel.openProductModal(product)` |
| **Botão "Adicionar Rápido" (`+`) no Card** | Ícone `Icons.Default.Add` em círculo primário | `onClick = onAddClick` -> `viewModel.addDirectProductToCart(product)` (Abre modal se tiver adicionais obrigatórios ou adiciona direto) |
| **Fechar Modal BottomSheet** | Gesto de arrastar para baixo ou toque fora | `onDismissRequest = { viewModel.closeProductModal() }` |
| **Seleção de Complemento / Addon (`RadioButton` / `Checkbox`)** | Nome do item + Preço adicional (se houver) | `onClick = { onToggleAddonItem(group, addonItem) }` -> `viewModel.toggleAddonItem(group, item)` |
| **Campo de Observações no Modal** | Label: "Alguma observação?" | `onValueChange = onNotesChange` -> `viewModel.updateModalNotes(it)` |
| **Botão Diminuir Quantidade no Modal (`-`)** | Ícone `Icons.Default.Remove` | `onClick = onQuantityDecrement` -> `viewModel.decrementModalQuantity()` |
| **Botão Aumentar Quantidade no Modal (`+`)** | Ícone `Icons.Default.Add` | `onClick = onQuantityIncrement` -> `viewModel.incrementModalQuantity()` |
| **Botão Adicionar ao Carrinho no Modal** | "Adicionar • R$ XX,XX" | `onClick = onAddToCartClick` -> `viewModel.addSelectedProductToCart()` |

---

## 2. CÓDIGO LITERAL DAS FUNÇÕES PRINCIPAIS

### Arquivo: `StoreDetailViewModel.kt`

```kotlin
fun loadStore(storeId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    viewModelScope.launch {
        val storeList = StoreRepository.stores.value
        val store = storeList.find { it.id == storeId } ?: storeList.firstOrNull()

        // Fetch menu_sections from Supabase
        val sections = SupabaseClient.fetchMenuSectionsForStore(storeId)

        // Fetch products from Supabase (already sorted by name & filtered)
        val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
        _rawProducts.value = remoteProducts

        // Fetch addon data
        allAddonGroups = SupabaseClient.fetchAddonGroupsForStore(storeId)
        productAddonGroupsMap = SupabaseClient.fetchProductAddonGroupsMap()
        allAddonItems = SupabaseClient.fetchAddonItemsForStore()

        _uiState.value = _uiState.value.copy(
            store = store,
            isLoading = false,
            menuSections = sections,
            selectedSectionName = "Todos"
        )
    }
}

fun toggleAddonItem(group: AddonGroup, item: AddonItem) {
    val currentSelectedMap = _uiState.value.modalSelectedAddonsMap.toMutableMap()
    val currentSelected = (currentSelectedMap[group.id] ?: emptyList()).toMutableList()

    if (group.maxSelect == 1) {
        if (currentSelected.contains(item) && group.minSelect == 0) {
            currentSelected.clear()
        } else {
            currentSelected.clear()
            currentSelected.add(item)
        }
    } else {
        if (currentSelected.contains(item)) {
            currentSelected.remove(item)
        } else {
            if (currentSelected.size < group.maxSelect) {
                currentSelected.add(item)
            }
        }
    }

    currentSelectedMap[group.id] = currentSelected
    _uiState.value = _uiState.value.copy(modalSelectedAddonsMap = currentSelectedMap, modalError = null)
}

fun addSelectedProductToCart() {
    val state = _uiState.value
    val product = state.selectedProductForModal ?: return
    
    // Validate required groups
    for (group in state.modalAddonGroups) {
        val selected = state.modalSelectedAddonsMap[group.id] ?: emptyList()
        if (selected.size < group.minSelect) {
            _uiState.value = state.copy(
                modalError = "Selecione pelo menos ${group.minSelect} opção em '${group.name}'"
            )
            return
        }
    }

    val storeName = state.store?.name ?: "Loja"

    val selectedAddonsList = mutableListOf<SelectedAddonItem>()
    for (group in state.modalAddonGroups) {
        val selectedItems = state.modalSelectedAddonsMap[group.id] ?: emptyList()
        for (item in selectedItems) {
            selectedAddonsList.add(
                SelectedAddonItem(
                    itemId = item.id,
                    itemName = item.name,
                    itemPrice = item.price,
                    groupId = group.id,
                    groupName = group.name,
                    priceReplacesBase = group.priceReplacesBase
                )
            )
        }
    }

    CartRepository.addProduct(
        product = product,
        storeName = storeName,
        quantity = state.modalQuantity,
        notes = state.modalNotes,
        selectedAddons = selectedAddonsList
    )
    closeProductModal()
}
```

### Arquivo: `CartRepository.kt`

```kotlin
fun addProduct(
    product: Product,
    storeName: String,
    quantity: Int = 1,
    notes: String = "",
    selectedAddons: List<SelectedAddonItem> = emptyList()
) {
    val current = _cartState.value
    
    // If adding from a different store, reset cart to new store
    if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
        _cartState.value = CartState(
            storeId = product.storeId,
            storeName = storeName,
            items = listOf(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
        )
        return
    }

    val existingIndex = current.items.indexOfFirst { 
        it.product.id == product.id && it.selectedAddons == selectedAddons && it.notes == notes
    }
    val updatedItems = current.items.toMutableList()

    if (existingIndex >= 0) {
        val existing = updatedItems[existingIndex]
        val newQty = existing.quantity + quantity
        updatedItems[existingIndex] = existing.copy(quantity = newQty)
    } else {
        updatedItems.add(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
    }

    _cartState.value = current.copy(
        storeId = product.storeId,
        storeName = storeName,
        items = updatedItems
    )
}
```

---

## 3. TODAS AS QUERIES AO SUPABASE NESTA TELA

1. **Seções do Cardápio (`menu_sections`)**:
   - **Tabela**: `menu_sections`
   - **Colunas**: `id, name, sort_order`
   - **Filtro & Ordenação**: `store_id=eq.$storeId&order=sort_order.asc`
   - **Código Exato**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/menu_sections?store_id=eq.$storeId&order=sort_order.asc"
     ```

2. **Produtos da Loja (`products`)**:
   - **Tabela**: `products`
   - **Colunas Selecionadas**: `.select("*")`
   - **Filtro & Ordenação**: `store_id=eq.$storeId&order=name.asc`
   - **Filtros no Cliente**:
     - `metadata.pdv_only == true` -> ignora
     - `metadata.hidden == true` -> ignora
     - `sold_by_weight == true` -> ignora
     - `name.isBlank()` -> ignora
   - **Código Exato**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/products?select=*&store_id=eq.$storeId&order=name.asc"
     ```

3. **Grupos de Adicionais (`addon_groups`)**:
   - **Tabela**: `addon_groups`
   - **Colunas**: `id, name, min_select, max_select, sort_order, price_replaces_base, product_id, store_id`
   - **Filtro & Ordenação**: `store_id=eq.$storeId&order=sort_order.asc`
   - **Código Exato**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/addon_groups?store_id=eq.$storeId&order=sort_order.asc"
     ```

4. **Associação de Grupos a Produtos (`product_addon_groups`)**:
   - **Tabela**: `product_addon_groups`
   - **Colunas**: `product_id, addon_group_id`
   - **Código Exato**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/product_addon_groups?select=*"
     ```

5. **Itens de Adicionais (`addon_items`)**:
   - **Tabela**: `addon_items`
   - **Colunas**: `id, group_id (ou addon_group_id), name, price, sort_order, is_available`
   - **Ordenação**: `order=sort_order.asc`
   - **Código Exato**:
     ```kotlin
     val url = "$SUPABASE_URL/rest/v1/addon_items?select=*&order=sort_order.asc"
     ```

---

## 4. VALIDAÇÕES E REGRAS DE NEGÓCIO IMPLEMENTADAS

1. **Validação de Adicionais Obrigatórios (`minSelect`)**:
   - Verifica se a quantidade de itens selecionados em cada grupo é `>= group.minSelect`. Exibe mensagem de erro no modal caso a condição não seja atendida.
2. **Limite Máximo de Seleção (`maxSelect`)**:
   - Se `maxSelect == 1`, funciona como rádio (única escolha).
   - Se `maxSelect > 1`, funciona como checkbox (múltiplas escolhas até o limite `maxSelect`).
3. **Substituição do Preço Base (`priceReplacesBase`)**:
   - Se um grupo selecionado possuir `priceReplacesBase == true`, o valor do item selecionado substitui o preço base do produto no cálculo final do preço unitário.
4. **Filtragem Rorosa de Produtos Ocultos/PDV/Por Peso**:
   - Produtos com `metadata.pdv_only = true`, `metadata.hidden = true`, `sold_by_weight = true` ou sem nome são filtrados antes de renderizar no cardápio.
5. **Remoção do Favoritar Loja**:
   - O botão de favoritar foi removido da TopBar para manter paridade com a versão web.

---

## 5. NAVEGAÇÃO E FLUXO DA TELA

1. **Voltar (`onBackClick`)**: `navController.popBackStack()`
2. **Ir para Sacola (`onNavigateToCart`)**: `navController.navigate("carrinho")`
