package com.example.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.CartState
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StoreDetailUiState(
    val store: Store? = null,
    val isLoading: Boolean = true,
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val categories: List<String> = emptyList(),
    val selectedProductForModal: Product? = null,
    val modalQuantity: Int = 1,
    val modalNotes: String = ""
)

class StoreDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StoreDetailUiState())
    val uiState: StateFlow<StoreDetailUiState> = _uiState.asStateFlow()

    private val _rawProducts = MutableStateFlow<List<Product>>(emptyList())

    val cartState: StateFlow<CartState> = CartRepository.cartState

    val filteredProducts: StateFlow<List<Product>> = combine(
        _rawProducts,
        _uiState
    ) { products, state ->
        val query = state.searchQuery.trim().lowercase()
        val category = state.selectedCategory

        products.filter { product ->
            val matchesCategory = if (category == "Todos") true else product.category.equals(category, ignoreCase = true)
            val matchesQuery = if (query.isBlank()) true else {
                product.name.lowercase().contains(query) || product.description.lowercase().contains(query)
            }
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadStore(storeId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Find store in repository or fetch refreshed list
            val storeList = StoreRepository.stores.value
            val store = storeList.find { it.id == storeId } ?: storeList.firstOrNull()

            // Fetch products from Supabase or generate defaults
            val remoteProducts = SupabaseClient.fetchProductsForStore(storeId)
            val products = if (remoteProducts.isNotEmpty()) {
                remoteProducts
            } else {
                generateDefaultProductsForStore(storeId, store?.name ?: "", store?.category ?: "")
            }

            _rawProducts.value = products

            val categoryList = mutableListOf("Todos")
            val productCategories = products.map { it.category }.distinct()
            categoryList.addAll(productCategories)

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

    private fun generateDefaultProductsForStore(storeId: String, storeName: String, category: String): List<Product> {
        val list = mutableListOf<Product>()
        val catLower = category.lowercase()

        if (catLower.contains("pizza") || storeId.contains("pizza")) {
            list.add(Product("p1", storeId, "Pizza Calabresa Especial", "Calabresa artesanal fatiada, molho de tomate fresco, mussarela, cebola roxa e azeitonas pretas.", 44.90, 49.90, "Pizzas"))
            list.add(Product("p2", storeId, "Pizza Quatro Queijos", "Mussarela cremosa, provolone curado, gorgonzola e catupiry original.", 48.90, null, "Pizzas"))
            list.add(Product("p3", storeId, "Pizza Portuguesa Tradicional", "Presunto de peru, ovos cozidos, ervilhas, cebola, mussarela e orégano.", 46.90, null, "Pizzas"))
            list.add(Product("p4", storeId, "Pizza Doce Nutella com Morango", "Base de massa fina crocante com ganache de Nutella e morangos frescos.", 39.90, null, "Sobremesas"))
            list.add(Product("p5", storeId, "Guaraná Antarctica 2L", "Refrigerante gelado embalagem de 2 litros.", 11.90, null, "Bebidas"))
            list.add(Product("p6", storeId, "Coca-Cola Zero 1.5L", "Refrigerante gelado sem açúcar.", 10.90, null, "Bebidas"))
        } else if (catLower.contains("mercado") || catLower.contains("superm") || storeId.contains("mercado") || storeId.contains("itasuper")) {
            list.add(Product("m1", storeId, "Arroz Branco Tipo 1 5kg", "Arroz de alta qualidade, grãos selecionados e soltinhos.", 28.90, 32.90, "Mercearia"))
            list.add(Product("m2", storeId, "Feijão Preto Tipo 1 1kg", "Feijão novo, cozimento rápido e caldo encorpado.", 8.50, null, "Mercearia"))
            list.add(Product("m3", storeId, "Leite Integral UHT 1L", "Leite puro, rico em cálcio.", 4.99, null, "Laticínios"))
            list.add(Product("m4", storeId, "Café Torrado e Moído 500g", "Aroma marcante e sabor intenso.", 18.90, null, "Matinais"))
            list.add(Product("m5", storeId, "Banana Prata Kg", "Frutas frescas da região de Itaboraí.", 6.90, null, "Hortifruti"))
            list.add(Product("m6", storeId, "Detergente Líquido 500ml", "Lava-louças neutro de alta eficiência.", 2.99, null, "Limpeza"))
        } else if (catLower.contains("farm") || storeId.contains("farm")) {
            list.add(Product("f1", storeId, "Dipirona Monoidratada 500mg", "Analgésico e antipirético com 20 comprimidos.", 6.90, null, "Medicamentos"))
            list.add(Product("f2", storeId, "Vitamina C Efervescente 1000mg", "Tubo com 10 comprimidos sabor laranja.", 14.90, 19.90, "Vitaminas"))
            list.add(Product("f3", storeId, "Protetor Solar FPS 50 120ml", "Toque seco e resistente à água.", 39.90, null, "Perfumaria"))
            list.add(Product("f4", storeId, "Sabonete Líquido Facial 150ml", "Limpeza profunda e controle de oleosidade.", 24.90, null, "Higiene"))
        } else {
            // Default Lanches / Restaurante / Geral
            list.add(Product("l1", storeId, "Pastel de Carne Especial", "Massa crocante com recheio generoso de carne moída temperada, ovo e azeitona.", 14.90, 16.90, "Mais Pedidos"))
            list.add(Product("l2", storeId, "Pastel de Frango com Catupiry", "Massa frita na hora com frango desfiado suculento e catupiry cremoso.", 15.90, null, "Mais Pedidos"))
            list.add(Product("l3", storeId, "Pastel de Queijo Mussarela", "Massa leve e crocante recheada com muito queijo mussarela derretido.", 12.90, null, "Salgados"))
            list.add(Product("l4", storeId, "X-Tudo Mega ItaSuper", "Hambúrguer artesanal 180g, queijo, bacon, ovo, presunto, alface, tomate e maionese da casa.", 28.90, 32.90, "Lanches"))
            list.add(Product("l5", storeId, "Caldo de Cana 500ml", "Caldo de cana puro e gelado feito na hora.", 8.00, null, "Bebidas"))
            list.add(Product("l6", storeId, "Coca-Cola lata 350ml", "Refrigerante gelado lata.", 6.50, null, "Bebidas"))
        }

        return list
    }
}
