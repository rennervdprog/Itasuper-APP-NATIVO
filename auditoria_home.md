# Auditoria Técnica Máxima Atualizada: Tela Principal / Início (`/home`)

---

## 1. RESPOSTA AOS 3 PONTOS CRÍTICOS SOLICITADOS

### Point 1: Confirmação do número do WhatsApp `(21) 97123-4567`
**Confirmação:** O número `(21) 97123-4567` é um **placeholder de exemplo** inserido durante a alteração anterior, pois não há um número de telefone de suporte cadastrado no banco de dados. Caso deseje um número de suporte real da operação, informe o número definitivo para substituição imediata no `SupportBottomSheet.kt`.

---

### Point 2: Atualização do `refreshLocation()`
Em `HomeViewModel.kt`, a mensagem de feedback do `refreshLocation()` foi alterada de `"Localização atualizada!"` para **`"Lista de lojas atualizada!"`**, refletindo exatamente o que ocorre na função (recarregamento das lojas via Supabase sem leitura fictícia de GPS).

**Código Literal Atualizado:**
```kotlin
fun refreshLocation() {
    _uiState.value = _uiState.value.copy(
        isRefreshingLocation = true
    )
    // Recarrega lista de lojas do Supabase
    loadStores()
    _uiState.value = _uiState.value.copy(
        isRefreshingLocation = false,
        snackbarMessage = "Lista de lojas atualizada!"
    )
}
```

---

### Point 3: Persistência do Número de Endereço (`saveStreetNumber()`) no Supabase
Em `HomeViewModel.kt`, a função `saveStreetNumber()` agora persiste a alteração no `UserSessionRepository` local **E** envia uma requisição `PATCH` para a tabela `profiles` no Supabase (colunas `number` / `address_number`) para garantir persistência após o fechamento do app.

**Código Literal no `SupabaseClient.kt`:**
```kotlin
// 3b. UPDATE PROFILE NUMBER/ADDRESS IN SUPABASE
suspend fun updateUserProfileNumber(userId: String, number: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
        val bodyJson = JSONObject().apply {
            put("number", number)
            put("address_number", number)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        response.isSuccessful || response.code == 204 || response.code == 200
    } catch (e: Exception) {
        Log.e(TAG, "Error updating user profile number", e)
        false
    }
}
```

**Código Literal no `HomeViewModel.kt`:**
```kotlin
fun saveStreetNumber() {
    val currentNumber = _uiState.value.streetNumber
    val currentUserSession = UserSessionRepository.userSession.value

    UserSessionRepository.updateProfile(
        name = currentUserSession.name,
        whatsapp = currentUserSession.whatsapp,
        street = currentUserSession.addressStreet,
        number = currentNumber,
        neighborhood = currentUserSession.addressNeighborhood,
        cep = currentUserSession.addressCep,
        pixKeyType = currentUserSession.pixKeyType,
        pixKey = currentUserSession.pixKey
    )

    viewModelScope.launch {
        if (currentUserSession.userId.isNotBlank()) {
            SupabaseClient.updateUserProfileNumber(currentUserSession.userId, currentNumber)
        }
    }

    _uiState.value = _uiState.value.copy(
        streetNumber = currentNumber,
        isEditingNumber = false,
        snackbarMessage = if (currentNumber.isNotBlank()) "Endereço atualizado no perfil!" else "Por favor adicione seu endereço"
    )
}
```

---

## 2. LISTA ATUALIZADA DE ELEMENTOS INTERATIVOS DA HOME

| Elemento / Componente | Texto Visível / Ícone | Função Chamada no `onClick` |
| :--- | :--- | :--- |
| **Seletor de Endereço (Vazio/Ação)** | "Adicione seu endereço" + Chip "Adicionar" | `{ viewModel.toggleEditNumber() }` |
| **Seletor de Endereço (Preenchido)** | `${uiState.streetName}, ${uiState.streetNumber}` + Ícone `Edit` | `{ viewModel.toggleEditNumber() }` |
| **Salvar Número do Endereço** | Ícone `Check` (ao editar número) | `{ viewModel.saveStreetNumber() }` |
| **Botão de Atendimento / Suporte** | Ícone `Icons.Outlined.HeadsetMic` | `{ viewModel.openSupportSheet() }` |
| **Barra de Pesquisa (Placeholder)** | "Buscar no ItaSuper..." | `{ onNavigateToSearch() }` |
| **Chips de Categoria** | "Todas", "Lanches", "Pizza", "Mercado", "Farmácia", "Bebidas" | `{ viewModel.onCategorySelect(category.id) }` |
| **Banner Promocional Dinâmico** | Imagem + Título do Banner (Vindo do Supabase `banners`) | `{ banner.targetStoreId?.let { storeId -> onNavigateToStore(storeId) } }` |
| **Botão "Tentar novamente"** | Exibido quando a consulta ao Supabase falha | `{ viewModel.loadStores() }` |
| **Card da Lista de Lojas** (`StoreCardItem`) | Logo, Nome, Categoria, Avaliação, Entrega e Distância | `{ onNavigateToStore(store.id) }` |
| **Botão "Falar pelo WhatsApp"** (Support Sheet) | "Falar pelo WhatsApp" | Abre `Intent.ACTION_VIEW` com URI `https://wa.me/5521971234567...` |
| **Barra de Navegação Inferior** | Ícones: Início, Busca, Pedidos, Perfil | `{ onNavigateToRoute(route) }` |

---

## 3. TODAS AS QUERIES AO SUPABASE NESTA TELA

1. **Lojas Ativas (`stores_public`)**:
   - URL: `$SUPABASE_URL/rest/v1/stores_public?select=*&status=eq.active&is_open=eq.true&order=rating.desc`
   - Tipo: `GET`

2. **Banners Promocionais (`banners`)**:
   - URL: `$SUPABASE_URL/rest/v1/banners?select=*&is_active=eq.true`
   - Tipo: `GET`

3. **Atualização do Perfil do Usuário (`profiles`)**:
   - URL: `$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId`
   - Payload: `{"number": number, "address_number": number}`
   - Tipo: `PATCH`

---

## 4. O QUE NÃO EXISTE OU É INCERTO

1. **Número de WhatsApp de Atendimento Definitivo**:
   - **CONFIRMAÇÃO / INCERTEZA**: O número `(21) 97123-4567` é um **placeholder de exemplo** no código. Se você possuir um número oficial, basta informar para ser configurado.
2. **GPS / Geolocalização Nativa**:
   - **NÃO IMPLEMENTADO**: O app não lê o sensor de GPS nativo; a localização atualizada aciona apenas o recarregamento dos dados de loja no Supabase.
