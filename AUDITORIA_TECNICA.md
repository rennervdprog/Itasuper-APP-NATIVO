# Auditoria Técnica da UI/UX e Funcionalidades Nativas (App ItaSuper - Android / Kotlin Jetpack Compose)

Este documento contém a auditoria completa das telas já implementadas (**Autenticação**, **Home** e **Busca**), detalhando com transparência o que é lógica real, validações com código-fonte funcional, placeholders/mocks, integração com backend e navegação.

---

## TELA 1: Autenticação (`/auth`)

### CAMPOS E BOTÕES:
- **Tabs/Seleção de modo**: "Entrar" e "Criar Conta"
- **Modo Login**:
  - Campo "E-mail"
  - Campo "Senha" (com ícone de olho para alternar visibilidade)
  - Botão "Entrar"
  - Link/Botão "Esqueci minha senha"
- **Modo Cadastro**:
  - Campo "Nome completo"
  - Campo "CPF ou CNPJ" (com aplicação automática de máscara)
  - Campo "WhatsApp com DDD" (com aplicação automática de máscara)
  - Campo "Senha" (mínimo 6 caracteres, com alternância de visibilidade)
  - Campo "PIN de entrega" (4 dígitos numéricos, com alternância de visibilidade)
  - Campo "Confirmar PIN de entrega" (4 dígitos numéricos)
  - Checkbox "Li e concordo com os Termos de Uso" + Link para abrir Bottom Sheet dos Termos
  - Botão "Criar minha conta"
- **Diálogo "Esqueci minha senha"**:
  - Campo "E-mail para recuperação"
  - Botão "Enviar link de recuperação"
  - Botão "Cancelar"
- **Bottom Sheet "Termos de Uso"**:
  - Leitura dos termos + Botão "Concordar e Fechar"

### VALIDAÇÕES IMPLEMENTADAS DE VERDADE (com código funcional):
Todas as validações solicitadas no fluxo do cliente original foram implementadas em código Kotlin reativo (`AuthViewModel` + `Masks.kt`):

1. **Validação de E-mail (Login e Recuperação)**:
```kotlin
if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    _uiState.value = state.copy(errorMessage = "Informe um e-mail válido")
    return
}
```

2. **Validação de Senha (mínimo 6 caracteres)**:
```kotlin
if (password.length < 6) {
    _uiState.value = state.copy(errorMessage = "Senha: mínimo 6 caracteres")
    return
}
```

3. **Validação de Nome Completo (mínimo de duas palavras)**:
```kotlin
val name = state.regName.trim()
if (name.isBlank() || !name.contains(" ")) {
    _uiState.value = state.copy(errorMessage = "Informe seu nome completo")
    return
}
```

4. **Validação Matemática de CPF / CNPJ com Dígitos Verificadores Reais (`Masks.isValidCpfOrCnpj`)**:
```kotlin
if (!Masks.isValidCpfOrCnpj(state.regCpfCnpj)) {
    _uiState.value = state.copy(errorMessage = "CPF ou CNPJ inválido")
    return
}
```
*(Usa algoritmo oficial de módulo 11 para conferência de dígitos verificadores de CPF e CNPJ)*.

5. **Validação de WhatsApp com DDD (`Masks.isValidPhone`)**:
```kotlin
if (!Masks.isValidPhone(state.regWhatsapp)) {
    _uiState.value = state.copy(errorMessage = "Informe um WhatsApp válido com DDD")
    return
}
```

6. **Validação de PIN de Entrega (4 dígitos exatos) e Confirmação de PIN**:
```kotlin
if (state.regPin.length != 4) {
    _uiState.value = state.copy(errorMessage = "Defina um PIN de entrega com 4 dígitos numéricos")
    return
}

if (state.regPin != state.regPinConfirm) {
    _uiState.value = state.copy(errorMessage = "Os PINs informados não coincidem")
    return
}
```

7. **Validação Obrigatoriedade do Aceite dos Termos de Uso**:
```kotlin
if (!state.isTermsAccepted) {
    _uiState.value = state.copy(errorMessage = "Você precisa aceitar os Termos de Uso para continuar")
    return
}
```

### VALIDAÇÕES QUE SÃO PLACEHOLDER/MOCK (não fazem verificação real):
- **Verificação de senha/hash no servidor**: Ao clicar em "Entrar", qualquer e-mail no formato correto e senha com 6 ou mais caracteres é autenticado localmente com sucesso. Não consulta credenciais num servidor remoto ainda.
- **Envio de e-mail de recuperação por SMTP/API**: O diálogo de "Esqueci minha senha" valida o e-mail e exibe o aviso "E-mail de recuperação enviado!", mas não faz o disparo de e-mail externo.

### INTEGRAÇÃO COM BACKEND:
- Os dados da sessão do usuário autenticado são salvos na memória da aplicação através do repositório Kotlin `UserSessionRepository` (via `StateFlow`). **Ainda não está conectado à API remota do Supabase Auth / REST.**

### ESTADO E NAVEGAÇÃO:
- **Navegação 100% funcional**: Quando o login ou cadastro é concluído com sucesso, o app executa o callback `onSuccess()`, redirecionando via Navigation Compose para a rota `"home"` e removendo a rota `"auth"` da pilha de telas para evitar retorno.

### O QUE FALTA para esta tela ficar 100% funcional (não só visual):
- Conectar as chamadas de login e cadastro com o cliente do **Supabase Auth** (`supabase.gotrue.loginWith` / `signUp`).
- Persistir o token JWT / sessão no `DataStore` do Android para manter o usuário logado ao fechar e reabrir o app.
- Conectar o envio de e-mail de recuperação com a API de Auth/Reset do Supabase.

---

## TELA 2: Home do Cliente (`/cliente`)

### CAMPOS E BOTÕES:
- **Header Superior**:
  - Endereço e Bairro atual do cliente
  - Ícone/Botão para abrir editor inline do número do endereço
  - Botão de "Atualizar localização" (refresh)
  - Botão de abrir modal de suporte
  - Botão de notificações
  - Botão de atalho para "Meus Pedidos"
- **Editor Inline do Número**:
  - Campo de texto numérico + Botão "Salvar"
- **Barra de Busca Rápida**:
  - Campo/Card clicável que redireciona para a tela de Busca completa
- **Barra de Filtros por Categoria**:
  - Chips clicáveis em scroll horizontal (Lanches, Pizza, Mercado, Farmácia, Bebidas, etc.)
- **Bento / Banner Hero**:
  - Card de destaque com botão "Ver mais"
- **Card "Último Pedido"**:
  - Resumo do último pedido (nome da loja, data, itens e valor)
  - Botão "Ver loja"
  - Botão "Pedir de novo" (Repetir pedido)
- **Grid de Suas Lojas & Destaques da Região**:
  - Cards clicáveis das lojas parceiras
- **Suporte Técnico**:
  - Botão flutuante para abrir Bottom Sheet com opções de atendimento
- **Bottom Navigation Bar**:
  - 4 abas ativas: Home, Busca, Pedidos, Perfil

### VALIDAÇÕES IMPLEMENTADAS DE VERDADE (com código funcional):
1. **Edição e atualização em tempo real do número do endereço**:
```kotlin
fun updateAddressNumber(newNumber: String) {
    val current = _uiState.value.userAddress
    val baseAddress = current.substringBefore(",").trim()
    _uiState.value = _uiState.value.copy(
        userAddress = "$baseAddress, $newNumber",
        isEditingNumber = false
    )
}
```

2. **Filtro dinâmico da lista de lojas por Categoria**:
```kotlin
fun selectCategory(categoryId: String) {
    val current = _uiState.value.selectedCategory
    val next = if (current == categoryId && categoryId != "todas") "todas" else categoryId
    _uiState.value = _uiState.value.copy(selectedCategory = next)
}
```

3. **Gatilho de abertura/fechamento do Bottom Sheet de Suporte**:
```kotlin
fun toggleSupportSheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showSupportSheet = show)
}
```

### VALIDAÇÕES QUE SÃO PLACEHOLDER/MOCK (não fazem verificação real):
- **Localização via GPS Nativo do Android**: O botão de atualizar localização simula um tempo de carregamento (`delay(1000)`) e atualiza o texto para um endereço estático. Não consome o sensor de GPS do dispositivo (`FusedLocationProviderClient`).
- **Abertura do WhatsApp no Suporte**: As opções do Bottom Sheet de suporte exibem opções e mostram avisos, mas ainda não iniciam uma `Intent.ACTION_VIEW` para o app do WhatsApp.

### INTEGRAÇÃO COM BACKEND:
- A lista de lojas, categorias e informações do último pedido são servidas por `StoreRepository.kt` com dados reativos em memória (`StateFlow`). **Não está conectado a um banco remoto ainda.**

### ESTADO E NAVEGAÇÃO:
- **Navegação 100% funcional com Navigation Compose**:
  - Clicar na busca abre a rota `"busca"`.
  - Clicar em qualquer card de loja navega para a rota `"loja/{storeId}"`.
  - Clicar na barra inferior navega para `"busca"`, `"pedidos"` ou `"perfil"`.
  - Clicar em "Ver loja" no card de último pedido abre a loja correspondente.

### O QUE FALTA para esta tela ficar 100% funcional (não só visual):
- Conectar o `StoreRepository` à tabela de lojas do Supabase/PostgreSQL.
- Integrar com o `FusedLocationProviderClient` da Google Play Services para obter as coordenadas GPS reais e usar Geocoding reverso.
- Ligar as opções do modal de suporte a `Intent` nativa para abrir o WhatsApp do suporte diretamente.

---

## TELA 3: Busca de Lojas e Produtos (`/cliente/busca`)

### CAMPOS E BOTÕES:
- **Topo de Localização**:
  - Endereço de entrega + Botão de atualizar localização
- **Barra de Pesquisa**:
  - `OutlinedTextField` com ícone de busca
  - Botão de limpar texto (X) visível dinamicamente quando há texto digitado
- **Carrossel de Categorias**:
  - Chips interativos para alternar entre "Todas", "Lanches", "Pizza", "Mercado", "Farmácia", "Bebidas"
- **Seção "Buscas Recentes"**:
  - Chips clicáveis contendo termos recentes que preenchem o campo automaticamente
  - Botão "Limpar" para apagar as buscas recentes
- **Contador de Resultados**:
  - Exibe a quantidade exata de lojas encontradas
- **Lista de Resultados**:
  - Cards de loja com nota, tempo de entrega, avaliação, distância e status de entrega grátis
- **Estado Vazio (Sem resultados)**:
  - Ilustração de alerta + mensagem personalizada + Botão "Ver todas as lojas"
- **Bottom Navigation Bar**:
  - Aba "Busca" destacada como ativa

### VALIDAÇÕES IMPLEMENTADAS DE VERDADE (com código funcional):
1. **Filtro Combinado e Reativo por Query e Categoria (`SearchViewModel`)**:
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
}.stateIn(...)
```

2. **Limpeza do campo e reset dos filtros**:
```kotlin
fun clearQuery() {
    _uiState.value = _uiState.value.copy(query = "")
}
```

3. **Preenchimento automático do campo ao tocar em busca recente**:
```kotlin
fun onRecentSearchSelect(term: String) {
    _uiState.value = _uiState.value.copy(query = term)
}
```

4. **Apagar histórico de buscas recentes**:
```kotlin
fun clearRecentSearches() {
    _uiState.value = _uiState.value.copy(recentSearches = emptyList())
}
```

### VALIDAÇÕES QUE SÃO PLACEHOLDER/MOCK (não fazem verificação real):
- **Persistência do Histórico de Buscas**: A lista de buscas recentes é mantida no estado do `ViewModel`. Se o aplicativo for totalmente encerrado da memória, o histórico retorna à lista padrão.

### INTEGRAÇÃO COM BACKEND:
- Os dados consultados são processados dinamicamente via Kotlin Flow sobre a fonte de dados do `StoreRepository`. **Falta integrar a busca via query SQL / ilike no Supabase.**

### ESTADO E NAVEGAÇÃO:
- **Navegação 100% configurada**:
  - Ao clicar em qualquer resultado de loja, navega para `"loja/{storeId}"`.
  - Transição fluida entre as abas na barra inferior.

### O QUE FALTA para esta tela ficar 100% funcional (não só visual):
- Conectar a busca à API/Supabase permitindo pesquisar tanto por nome de loja quanto por itens específicos de cardápio (ex: buscar "Calabresa" traz pizzarias que vendem pizza de calabresa).
- Persistir o histórico de pesquisas recentes em armazenamento local (`DataStore`).

---

## Resumo do Status Global do Projeto

| Tela | Interface M3 | Validações Client-Side | Navegação Compose | Conexão Backend Supabase |
|---|:---:|:---:|:---:|:---:|
| **Autenticação (`/auth`)** | ✅ 100% | ✅ 100% Real | ✅ 100% Real | ⏳ Pendente |
| **Home (`/cliente`)** | ✅ 100% | ✅ 100% Real | ✅ 100% Real | ⏳ Pendente |
| **Busca (`/cliente/busca`)** | ✅ 100% | ✅ 100% Real | ✅ 100% Real | ⏳ Pendente |

---
*Gerado automaticamente pelo assistente de desenvolvimento Android ItaSuper.*
