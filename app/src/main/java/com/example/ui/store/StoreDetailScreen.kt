package com.example.ui.store

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.automirrored.filled.Chat
import com.example.data.model.MenuSection
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperHighlightText
import com.example.ui.theme.ItaSuperTextPrimary
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.data.model.PastelBorder
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.R
import com.example.core.network.ConnectivityMonitor
import com.example.data.model.AddonGroup
import com.example.data.model.AddonItem
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSecondary
import com.example.ui.theme.ItaSuperWarning

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    viewModel: StoreDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    // Sem busca na tela, o catálogo completo fica disponível para a navegação por seção.
    val products by viewModel.allProducts.collectAsState()
    val allStoreProducts by viewModel.allProducts.collectAsState()
    val cartState by viewModel.cartState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val context = LocalContext.current
    val connectivityMonitor = remember(context) { ConnectivityMonitor(context) }
    val isOnline by connectivityMonitor.isOnline.collectAsState()
    var retryAfterReconnect by remember { mutableStateOf(false) }
    LaunchedEffect(storeId, isOnline) {
        if (!isOnline) {
            retryAfterReconnect = true
            viewModel.showOffline(storeId)
        } else if (retryAfterReconnect || uiState.store == null) {
            retryAfterReconnect = false
            viewModel.loadStore(storeId)
        }
    }
    val menuListState = rememberLazyListState()
    val menuScrollScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = cartState.totalItemCount > 0) {
                Surface(
                    shadowElevation = 7.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFAFAFA))
                            .clickable(onClick = onNavigateToCart)
                            .testTag("view_cart_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = ItaSuperPrimary.copy(alpha = 0.11f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = ItaSuperPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ver sacola",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Surface(
                            shape = CircleShape,
                            color = ItaSuperPrimary
                        ) {
                            Text(
                                text = "${cartState.totalItemCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format("R$ %.2f", cartState.subtotal).replace(".", ","),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(13.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ItaSuperPrimary)
            }
        } else if (uiState.store == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Não foi possível abrir esta loja",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "Verifique sua conexão e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF686868)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (isOnline) viewModel.loadStore(storeId) else viewModel.showOffline(storeId)
                    }
                ) {
                    Text("Tentar novamente")
                }
            }
        } else {
            val store = uiState.store
            val isPharmacyStore = store?.let { currentStore ->
                currentStore.category.equals("farmacias", ignoreCase = true) ||
                    currentStore.secondaryCategories.any { it.equals("farmacias", ignoreCase = true) }
            } ?: false
            val hasPizzaCategory = store?.let { currentStore ->
                currentStore.category.lowercase().contains("pizza") ||
                    currentStore.secondaryCategories.any { it.lowercase().contains("pizza") }
            } ?: false
            val hasPastelCategory = store?.let { currentStore ->
                currentStore.category.lowercase().contains("pastel") || currentStore.category.lowercase().contains("pasteis") ||
                    currentStore.secondaryCategories.any { it.lowercase().contains("pastel") || it.lowercase().contains("pasteis") }
            } ?: false
            val builderType = when {
                hasPastelCategory && store?.settings?.pastelHalfEnabled != false && products.isNotEmpty() -> "pastel"
                hasPizzaCategory && store?.settings?.pizzaHalfEnabled != false && products.isNotEmpty() -> "pizza"
                else -> null
            }
            val isDeliveryUnavailable = store?.let {
                it.deliveryMode.equals("own", ignoreCase = true) && it.hasAvailableDriver == false
            } == true

            // Group products by menu_section / category
            val groupedProducts = remember(products, uiState.menuSections) {
                val map = LinkedHashMap<String, MutableList<Product>>()

                fun resolveSectionName(prod: Product): String {
                    if (!prod.sectionId.isNullOrBlank()) {
                        val found = uiState.menuSections.find { it.id == prod.sectionId }
                        if (found != null) return found.name
                    }
                    return if (prod.category.isNotBlank()) prod.category else "Geral"
                }

                // Initialize menuSections keys first
                uiState.menuSections.forEach { sec ->
                    map[sec.name] = mutableListOf()
                }

                // Populate products into map
                products.forEach { prod ->
                    val secName = resolveSectionName(prod)
                    map.getOrPut(secName) { mutableListOf() }.add(prod)
                }

                map.filterValues { it.isNotEmpty() }
            }
            val productSections = remember(groupedProducts) {
                groupedProducts.entries
                    .filterNot { (sectionName, _) -> sectionName.equals("Destaques", ignoreCase = true) }
                    .map { it.key to it.value }
            }
            val allSectionNames = listOf("Todos") + productSections.map { it.first }
            val featuredProducts = products.take(6)
            val sectionStartIndexByName = remember(productSections, builderType, featuredProducts, isDeliveryUnavailable) {
                val starts = LinkedHashMap<String, Int>()
                var itemIndex = 1 // Hero
                if (isDeliveryUnavailable) itemIndex += 1
                if (builderType != null) itemIndex += 1
                if (featuredProducts.isNotEmpty()) {
                    itemIndex += 1 + featuredProducts.chunked(3).size // título + linhas da grade
                }
                productSections.forEach { (sectionName, sectionProducts) ->
                    starts[sectionName] = itemIndex
                    itemIndex += 1 + sectionProducts.size // título da seção + linhas de produtos
                }
                starts
            }
            val activeSectionName by remember(menuListState, sectionStartIndexByName) {
                derivedStateOf {
                    sectionStartIndexByName
                        .filterValues { it <= menuListState.firstVisibleItemIndex }
                        .maxByOrNull { it.value }
                        ?.key ?: "Todos"
                }
            }
            val showContextualTopBar by remember(menuListState, sectionStartIndexByName) {
                derivedStateOf {
                    val firstMenuSectionIndex = sectionStartIndexByName.values.minOrNull() ?: Int.MAX_VALUE
                    menuListState.firstVisibleItemIndex >= firstMenuSectionIndex
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = menuListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                item {
                    StoreShowcaseHeader(
                        store = store,
                        onBackClick = onBackClick,
                        onContactClick = {
                            val phone = store?.whatsapp?.filter { it.isDigit() }.orEmpty()
                            if (phone.isNotBlank()) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone"))
                                    )
                                }
                            }
                        },
                        onOpenMaps = {
                            val mapQuery = store?.address?.takeIf { it.isNotBlank() }
                                ?: listOfNotNull(
                                    store?.addressStreet?.takeIf { it.isNotBlank() },
                                    store?.addressNumber?.takeIf { it.isNotBlank() },
                                    store?.addressNeighborhood?.takeIf { it.isNotBlank() },
                                    store?.addressCity?.takeIf { it.isNotBlank() }
                                ).joinToString(", ")
                            if (mapQuery.isNotBlank()) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(mapQuery)}"))
                                    )
                                }
                            }
                        },
                        onInfoClick = onNavigateToInfo
                    )
                }

                if (isPharmacyStore) {
                    item(key = "pharmacy_store_notice") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE8F7F3),
                            border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0F766E).copy(alpha = 0.14f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF0F766E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Catálogo de farmácia",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF115E59)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Itens liberados podem ser adicionados à sacola. Produtos sujeitos à validação aparecem com aviso e não entram no checkout comum.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF356B66),
                                            lineHeight = 17.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (isDeliveryUnavailable) {
                    item(key = "delivery_unavailable_notice") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ItaSuperWarning.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, ItaSuperWarning.copy(alpha = 0.32f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = ItaSuperWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Entrega indisponível no momento",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ItaSuperTextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = store?.deliveryAvailabilityMessage
                                            ?.ifBlank { "Esta loja está sem entregador disponível. Você ainda pode fazer seu pedido para retirada." }
                                            ?: "Esta loja está sem entregador disponível. Você ainda pode fazer seu pedido para retirada.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ItaSuperSecondary,
                                            lineHeight = 17.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // A personalização continua como uma única vitrine por loja.
                if (builderType != null) {
                    item {
                        StoreBuilderHighlight(
                            title = if (builderType == "pastel") "Monte seu pastel" else "Monte sua pizza",
                            subtitle = if (builderType == "pastel") {
                                "Escolha recheios e adicionais do seu jeito"
                            } else {
                                "Escolha tamanho, sabores e adicionais do seu jeito"
                            },
                            imageUrl = products.firstOrNull {
                                if (builderType == "pastel") it.isPastelFlavor else it.pizzaCategoryId.isNotBlank()
                            }?.imageUrl.orEmpty(),
                            icon = if (builderType == "pastel") Icons.Default.Restaurant else Icons.Default.LocalPizza,
                            testTag = if (builderType == "pastel") "monte_seu_pastel_button" else "monte_sua_pizza_button",
                            onClick = { viewModel.openBuilder(builderType) }
                        )
                    }
                }

                // Apenas Destaques usa vitrine em grade. O cardápio permanece em linhas horizontais.
                if (featuredProducts.isNotEmpty()) {
                    item(key = "highlights_title") {
                        Text(
                            text = "Destaques",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = ItaSuperTextPrimary
                            ),
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 10.dp)
                        )
                    }
                    featuredProducts.chunked(3).forEachIndexed { rowIndex, productRow ->
                        item(key = "highlights_grid_$rowIndex") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                productRow.forEach { product ->
                                    ProductShowcaseTile(
                                        product = product,
                                        onCardClick = { viewModel.openProductModal(product) },
                                        onAddClick = { viewModel.addDirectProductToCart(product) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - productRow.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Products Items Grouped by Section
                if (products.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank() || uiState.selectedSectionName != "Todos") {
                                    "Nenhum produto encontrado neste filtro."
                                } else {
                                    "Esta loja ainda não cadastrou produtos"
                                },
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    productSections.forEachIndexed { secIndex, (secName, secProducts) ->
                            item(key = "section_header_${secIndex}_$secName") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = secName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = ItaSuperTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${secProducts.size} itens",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = Color(0xFF777777)
                                        )
                                    )
                                }
                            }
                            itemsIndexed(secProducts, key = { productIndex, product ->
                                "product_row_${secIndex}_${productIndex}_${product.id}"
                            }) { _, product ->
                                ProductMenuRow(
                                    product = product,
                                    onCardClick = { viewModel.openProductModal(product) },
                                    onAddClick = { viewModel.addDirectProductToCart(product) }
                                )
                            }
                        }
                }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                AnimatedVisibility(
                    visible = showContextualTopBar,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    StoreStickyMenuHeader(
                        storeName = store?.name ?: "Loja ItaSuper",
                        sectionNames = allSectionNames,
                        activeSectionName = activeSectionName,
                        onBackClick = onBackClick,
                        onSectionClick = { sectionName ->
                            val targetIndex = if (sectionName == "Todos") {
                                sectionStartIndexByName.values.minOrNull() ?: 0
                            } else {
                                sectionStartIndexByName[sectionName] ?: 0
                            }
                            menuScrollScope.launch {
                                menuListState.animateScrollToItem(targetIndex)
                            }
                        }
                    )
                }
            }
        }

        // Modal Bottom Sheet for Product Detail & Addons
        if (uiState.selectedProductForModal != null) {
            val product = uiState.selectedProductForModal!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeProductModal() },
                sheetState = sheetState
            ) {
                ProductDetailSheetContent(
                    product = product,
                    uiState = uiState,
                    onToggleAddonItem = { group, item -> viewModel.toggleAddonItem(group, item) },
                    onQuantityIncrement = { viewModel.incrementModalQuantity() },
                    onQuantityDecrement = { viewModel.decrementModalQuantity() },
                    onNotesChange = { viewModel.updateModalNotes(it) },
                    onNextCustomization = { viewModel.nextProductModalStep() },
                    onBackToDetails = { viewModel.previousProductModalStep() },
                    onAddToCartClick = { viewModel.addSelectedProductToCart() }
                )
            }
        }

        // Modal or Full Screen Dialog for Monte Sua Pizza / Monte Seu Pastel Custom Builder
        if (uiState.showBuilderModal) {
            if (uiState.builderType == "pastel") {
                Dialog(
                    onDismissRequest = { viewModel.closeBuilderModal() },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        // O diálogo desenha até as bordas; o Compose reserva status, navegação e teclado.
                        decorFitsSystemWindows = false
                    )
                ) {
                    PastelWizardFullScreenContent(
                        uiState = uiState,
                        allProducts = allStoreProducts,
                        onClose = { viewModel.closeBuilderModal() },
                        onSelectTargetFlavors = { viewModel.setWizardTargetFlavors(it) },
                        onSelectFlavorForStep = { slot, prod -> viewModel.selectWizardFlavor(slot, prod) },
                        onNextStep = { viewModel.nextWizardStep() },
                        onPrevStep = { viewModel.prevWizardStep() },
                        onToggleComplement = { viewModel.toggleWizardComplement(it) },
                        onNotesChange = { viewModel.updateWizardNotes(it) },
                        onQuantityIncrement = { viewModel.incrementWizardQuantity() },
                        onQuantityDecrement = { viewModel.decrementWizardQuantity() },
                        onAddToCartClick = { viewModel.addPastelWizardToCart() }
                    )
                }
            } else {
                Dialog(
                    onDismissRequest = { viewModel.closeBuilderModal() },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        // Mantém os insets reais disponíveis para navigationBarsPadding e imePadding.
                        decorFitsSystemWindows = false
                    )
                ) {
                    PizzaWizardFullScreenContent(
                        uiState = uiState,
                        allProducts = allStoreProducts,
                        onClose = { viewModel.closeBuilderModal() },
                        onSelectSize = { id, name -> viewModel.setPizzaWizardSize(id, name) },
                        onSelectTargetFlavors = { viewModel.setPizzaWizardTargetFlavors(it) },
                        onSelectFlavor = { slot, product -> viewModel.selectPizzaWizardFlavor(slot, product) },
                        onToggleAddon = { group, item -> viewModel.togglePizzaWizardAddon(group, item) },
                        onSelectBorder = { viewModel.selectPizzaWizardBorder(it) },
                        onNotesChange = { viewModel.updatePizzaWizardNotes(it) },
                        onNext = { viewModel.nextPizzaWizardStep() },
                        onPrevious = { viewModel.prevPizzaWizardStep() },
                        onQuantityIncrement = { viewModel.incrementPizzaWizardQuantity() },
                        onQuantityDecrement = { viewModel.decrementPizzaWizardQuantity() },
                        onAddToCart = { viewModel.addPizzaWizardToCart() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreStickyMenuHeader(
    storeName: String,
    sectionNames: List<String>,
    activeSectionName: String,
    onBackClick: () -> Unit,
    onSectionClick: (String) -> Unit
) {
    val categoryRowState = rememberLazyListState()
    LaunchedEffect(activeSectionName, sectionNames) {
        val activeIndex = sectionNames.indexOfFirst { it.equals(activeSectionName, ignoreCase = true) }
        if (activeIndex >= 0) categoryRowState.animateScrollToItem(activeIndex)
    }

    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBackClick),
                    shape = CircleShape,
                    color = Color(0xFFF5F5F5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = ItaSuperTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARDÁPIO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF8A8A8A)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFF1F1F1))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
            ) {
                LazyRow(
                    state = categoryRowState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sectionNames, key = { index, name -> "sticky_section_${index}_$name" }) { _, sectionName ->
                        StoreMenuFilterPill(
                            label = sectionName,
                            selected = sectionName.equals(activeSectionName, ignoreCase = true),
                            onClick = { onSectionClick(sectionName) }
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFECECEC))
        }
    }
}

@Composable
private fun StoreBuilderHighlight(
    title: String,
    subtitle: String,
    imageUrl: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE9E9E9)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF4EA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF707070)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Montar agora",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ItaSuperPrimary
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreMenuFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFFFF0E6) else Color.White,
        border = BorderStroke(1.dp, if (selected) ItaSuperPrimary.copy(alpha = 0.55f) else Color(0xFFE5E5E5))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ItaSuperPrimary, CircleShape)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) ItaSuperPrimary else Color(0xFF343434),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StoreInfoMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = ItaSuperTextPrimary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = ItaSuperPrimary.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7A7A7A),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoreShowcaseHeader(
    store: Store?,
    onBackClick: () -> Unit,
    onContactClick: () -> Unit,
    onOpenMaps: () -> Unit,
    onInfoClick: () -> Unit
) {
    val isOpen = store?.isOpen == true
    val feeText = store?.deliveryFee?.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"
    val timeText = store?.deliveryTime?.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .background(Color(0xFF4B1A11))
        ) {
            if (!store?.bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = store?.bannerUrl,
                    contentDescription = store?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ItaSuperPrimary.copy(alpha = 0.82f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoreHeroActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Voltar",
                    onClick = onBackClick
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoreHeroActionButton(
                        icon = Icons.Default.FavoriteBorder,
                        description = "Favoritar loja",
                        onClick = { }
                    )
                    StoreHeroActionButton(
                        icon = Icons.Default.Share,
                        description = "Contato da loja",
                        onClick = onContactClick,
                        enabled = !store?.whatsapp.isNullOrBlank()
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 188.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 66.dp, end = 16.dp, bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInfoClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store?.name ?: "Loja ItaSuper",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Mais informações",
                        tint = Color(0xFF4A4A4A),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = store?.category?.takeIf { it.isNotBlank() } ?: "Alimentação",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color(0xFF686868)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$timeText  •  Taxa $feeText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = if (isOpen) Color(0xFFF1FAF3) else Color(0xFFFFF3F3),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isOpen) Color(0xFF16A34A) else Color(0xFFE5484D), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isOpen) "Aberta" else "Fechada",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isOpen) Color(0xFF16803A) else Color(0xFFC9343C)
                                )
                            )
                        }
                    }
                }

            }
        }

        StoreShowcaseLogo(
            store = store,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp)
        )
    }
}

@Composable
private fun StoreHeroActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.56f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun StoreShowcaseLogo(store: Store?, modifier: Modifier = Modifier) {
    val imageUrl = store?.logoUrl?.trim().orEmpty()
    var imageFailed by remember(store?.id, imageUrl) {
        mutableStateOf(imageUrl.isBlank() || imageUrl.equals("null", ignoreCase = true))
    }
    val initial = store?.name?.trim()?.take(1)?.uppercase().orEmpty().ifBlank { "I" }
    val useDarkPlaceholder = initial == "C"
    val placeholderBackground = if (useDarkPlaceholder) Color(0xFF741914) else Color(0xFFFFE8A8)
    val placeholderForeground = if (useDarkPlaceholder) Color.White else ItaSuperPrimary

    Surface(
        modifier = modifier.size(82.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (imageFailed) placeholderBackground else Color(0xFFFFE9AD),
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!imageFailed) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = store?.name,
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            }
            if (imageFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            lineHeight = 32.sp,
                            color = placeholderForeground
                        )
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_home_placeholder_smile),
                        contentDescription = null,
                        tint = placeholderForeground,
                        modifier = Modifier
                            .offset(y = (-7).dp)
                            .size(31.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductShowcaseTile(
    product: Product,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requiresPharmacyValidation = product.requiresPrescription || product.isControlled || product.pharmacySaleMode != "platform_checkout"
    Column(
        modifier = modifier
            .clickable { onCardClick() }
            .testTag("product_item_${product.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF4F4F4))
        ) {
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFFB9B9B9),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(25.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clickable { if (requiresPharmacyValidation) onCardClick() else onAddClick() }
                    .testTag("add_product_button_${product.id}"),
                shape = CircleShape,
                color = if (requiresPharmacyValidation) Color(0xFF0F766E) else ItaSuperPrimary,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (requiresPharmacyValidation) Icons.Default.Search else Icons.Default.Add,
                        contentDescription = if (requiresPharmacyValidation) "Ver detalhes de ${product.name}" else "Adicionar ${product.name}",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = ItaSuperTextPrimary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (product.requiresPrescription || product.isControlled || product.isGeneric) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = when {
                    product.isControlled -> "Validação da farmácia"
                    product.requiresPrescription -> "Receita obrigatória"
                    else -> "Genérico"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (product.isGeneric) Color(0xFF2563EB) else Color(0xFF0F766E),
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = String.format("R$ %.2f", product.price).replace(".", ","),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = ItaSuperPrimary
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun ProductMenuRow(
    product: Product,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("product_item_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.description.isNotBlank() && !product.description.equals("null", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF737373)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = String.format("R$ %.2f", product.price).replace(".", ","),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF4F4F4))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color(0xFFB9B9B9),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clickable { onAddClick() }
                        .testTag("add_product_button_${product.id}"),
                    shape = CircleShape,
                    color = ItaSuperPrimary,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar ${product.name}",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFFEEEEEE),
            thickness = 1.dp
        )
    }
}

@Composable
fun ProductDetailSheetContent(
    product: Product,
    uiState: StoreDetailUiState,
    onToggleAddonItem: (AddonGroup, AddonItem) -> Unit,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onNotesChange: (String) -> Unit,
    onNextCustomization: () -> Unit,
    onBackToDetails: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isCustomizationStep = uiState.modalTotalSteps > 1 && uiState.modalStep == 2
    val visibleAddonGroups = if (isCustomizationStep) {
        uiState.modalAddonGroups.filter { it.minSelect > 0 }
    } else {
        uiState.modalAddonGroups.filter { it.minSelect == 0 }
    }
    val requiresPharmacyValidation = product.requiresPrescription || product.isControlled || product.pharmacySaleMode != "platform_checkout"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        if (uiState.modalTotalSteps > 1) {
            Text(
                text = "ETAPA ${uiState.modalStep} DE ${uiState.modalTotalSteps}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { uiState.modalStep.toFloat() / uiState.modalTotalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = ItaSuperPrimary,
                trackColor = Color(0xFFF1EEE9)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (isCustomizationStep) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDetails) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = ItaSuperTextPrimary)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "PERSONALIZAÇÃO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary
                    )
                    Text(
                        text = "Escolha os obrigatórios",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Selecione as opções necessárias para finalizar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!product.description.isNullOrBlank() && product.description.trim() != "null") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = String.format("R$ %.2f", product.price).replace(".", ","),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ItaSuperPrimary
            )

            if (requiresPharmacyValidation || product.isGeneric || product.dosage.isNotBlank() || product.activeIngredient.isNotBlank() || product.manufacturer.isNotBlank() || product.packQuantity.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (requiresPharmacyValidation) Color(0xFFFFF4E5) else Color(0xFFE8F7F3),
                    border = BorderStroke(1.dp, if (requiresPharmacyValidation) Color(0xFFF59E0B).copy(alpha = 0.30f) else Color(0xFF0F766E).copy(alpha = 0.20f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (requiresPharmacyValidation) {
                            Text(
                                text = "Validação pela farmácia necessária",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                            )
                            Text(
                                text = "Este item é somente para consulta e não entra no checkout comum do ItaSuper.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E), lineHeight = 17.sp)
                            )
                        }
                        if (product.isGeneric) Text("Genérico", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)))
                        listOfNotNull(
                            product.activeIngredient.takeIf { it.isNotBlank() }?.let { "Princípio ativo: $it" },
                            product.dosage.takeIf { it.isNotBlank() }?.let { "Dosagem: $it" },
                            product.pharmaForm.takeIf { it.isNotBlank() }?.let { "Apresentação: $it" },
                            product.packQuantity.takeIf { it.isNotBlank() }?.let { "Embalagem: $it" },
                            product.manufacturer.takeIf { it.isNotBlank() }?.let { "Fabricante: $it" }
                        ).forEach { detail ->
                            Text(detail, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF356B66)))
                        }
                    }
                }
            }
        }

        if (uiState.modalAddonsLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Carregando opções do produto...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Na primeira etapa ficam apenas extras opcionais; obrigatórios ficam isolados na segunda.
        if (visibleAddonGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            visibleAddonGroups.forEach { group ->
                val items = uiState.modalAddonItemsMap[group.id] ?: emptyList()
                val selectedItems = uiState.modalSelectedAddonsMap[group.id] ?: emptyList()

                if (items.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    color = if (group.minSelect > 0) ItaSuperPrimary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (group.minSelect > 0) "OBRIGATÓRIO" else "OPCIONAL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (group.minSelect > 0) ItaSuperPrimary else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            val ruleText = if (group.maxSelect == 1) {
                                "Escolha 1 opção"
                            } else {
                                "Escolha até ${group.maxSelect} opções"
                            }
                            Text(
                                text = ruleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            items.forEach { addonItem ->
                                val isSelected = selectedItems.contains(addonItem)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleAddonItem(group, addonItem) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (group.maxSelect == 1) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onToggleAddonItem(group, addonItem) },
                                                colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary)
                                            )
                                        } else {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleAddonItem(group, addonItem) },
                                                colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = addonItem.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    if (addonItem.price > 0.0) {
                                        Text(
                                            text = "+ ${String.format("R$ %.2f", addonItem.price).replace(".", ",")}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ItaSuperPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isCustomizationStep) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.modalNotes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_notes_input"),
                label = { Text("Observações") },
                placeholder = { Text("Ex: Sem cebola, bem passado...") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (!uiState.modalError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.modalError!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quantity Selector & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onQuantityDecrement, enabled = uiState.modalQuantity > 1) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir")
                }
                Text(
                    text = "${uiState.modalQuantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(onClick = onQuantityIncrement) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Na etapa de detalhes o produto apenas avança; a validação ocorre na personalização.
            Button(
                onClick = if (!isCustomizationStep && uiState.modalTotalSteps > 1) onNextCustomization else onAddToCartClick,
                enabled = !requiresPharmacyValidation && !uiState.modalAddonsLoading && (if (!isCustomizationStep && uiState.modalTotalSteps > 1) true else uiState.canAddToCart),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("confirm_add_to_cart_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = when {
                        requiresPharmacyValidation -> "Validação pela farmácia necessária"
                        uiState.modalAddonsLoading -> "Carregando opções..."
                        !isCustomizationStep && uiState.modalTotalSteps > 1 -> "Próximo: Personalizar"
                        else -> "Adicionar • ${String.format("R$ %.2f", uiState.modalTotalPrice).replace(".", ",")}"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBuilderSheetContent(
    uiState: StoreDetailUiState,
    allProducts: List<Product>,
    onToggleFlavor: (Product) -> Unit,
    onSelectSize: (String) -> Unit,
    onToggleComplement: (AddonItem) -> Unit,
    onNotesChange: (String) -> Unit,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    val store = uiState.store ?: return
    val type = uiState.builderType
    val scrollState = rememberScrollState()

    val isPizza = type == "pizza"
    val title = if (isPizza) "Monte Sua Pizza" else "Monte Seu Pastel"
    val maxFlavors = if (isPizza) store.settings.pizzaMaxFlavors else store.settings.pastelMaxFlavors
    val priceMode = if (isPizza) store.settings.pizzaPriceMode else store.settings.pastelPriceMode
    val singleSize = if (isPizza) store.settings.pizzaSingleSize else store.settings.pastelSingleSize

    val priceModeText = when (priceMode.lowercase()) {
        "media" -> "Média dos sabores"
        "soma" -> "Soma dos sabores"
        else -> "Sabor mais caro"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Escolha até $maxFlavors sabores ($priceModeText)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Surface(
                color = ItaSuperPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${uiState.builderSelectedFlavors.size}/$maxFlavors Sabores",
                    color = ItaSuperPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Size Selection if not singleSize
        if (!singleSize) {
            Text(
                text = "Tamanho",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val sizes = listOf("Broto", "Média", "Grande", "Gigante")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sizes.forEach { sz ->
                    val selected = uiState.builderSelectedSize.equals(sz, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSize(sz) },
                        label = { Text(sz) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ItaSuperPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sabores Section
        Text(
            text = "Sabores Disponíveis (Escolha de 1 a $maxFlavors)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        allProducts.forEach { product ->
            val isSelected = uiState.builderSelectedFlavors.any { it.id == product.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onToggleFlavor(product) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(1.dp, ItaSuperPrimary) else BorderStroke(0.5.dp, Color.LightGray),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleFlavor(product) },
                            colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (product.description.isNotBlank()) {
                                Text(
                                    text = product.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        text = String.format("R$ %.2f", product.price).replace(".", ","),
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Observações
        OutlinedTextField(
            value = uiState.builderNotes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("builder_notes_input"),
            label = { Text("Alguma observação?") },
            placeholder = { Text("Ex: Sem cebola, massa bem assada...") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        if (!uiState.builderErrorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.builderErrorMessage!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quantity Selector & Submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onQuantityDecrement, enabled = uiState.builderQuantity > 1) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir")
                }
                Text(
                    text = "${uiState.builderQuantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(onClick = onQuantityIncrement) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onAddToCartClick,
                enabled = uiState.builderSelectedFlavors.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("confirm_builder_add_to_cart"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Adicionar • ${String.format("R$ %.2f", uiState.builderTotalPrice).replace(".", ",")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastelWizardFullScreenContent(
    uiState: StoreDetailUiState,
    allProducts: List<Product>,
    onClose: () -> Unit,
    onSelectTargetFlavors: (Int) -> Unit,
    onSelectFlavorForStep: (Int, Product) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onToggleComplement: (PastelBorder) -> Unit,
    onNotesChange: (String) -> Unit,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    val store = uiState.store ?: return
    val scrollState = rememberScrollState()
    val maxFlavors = store.settings.pastelMaxFlavors
    val maxComplements = store.settings.pastelMaxComplements

    BackHandler {
        onPrevStep()
    }

    // Filter pastel products
    val filteredPastelProducts = remember(allProducts, uiState.menuSections) {
        val hasCuratedFlavors = allProducts.any { it.isPastelFlavor }
        if (hasCuratedFlavors) {
            allProducts.filter { it.isPastelFlavor && it.isAvailable }
        } else {
            val beverageKeywords = listOf("bebida", "drink", "suco", "refrigerante", "água", "agua", "cerveja", "energético", "energetico")
            val sectionMap = uiState.menuSections.associateBy { it.id }

            allProducts.filter { p ->
                if (!p.isAvailable) return@filter false
                if (p.isBeverage) return@filter false

                val secName = p.sectionId?.let { sectionMap[it]?.name } ?: p.category
                val secNameLower = secName.lowercase()
                val catLower = p.category.lowercase()

                val isBeverageSection = beverageKeywords.any { kw ->
                    secNameLower.contains(kw) || catLower.contains(kw)
                }
                !isBeverageSection
            }
        }
    }

    // Products grouped by section
    val productsBySection = remember(filteredPastelProducts, uiState.menuSections) {
        val sectionMap = uiState.menuSections.associateBy { it.id }
        val grouped = LinkedHashMap<String, MutableList<Product>>()

        for (p in filteredPastelProducts) {
            val secName = p.sectionId?.let { sectionMap[it]?.name } ?: p.category
            val headerName = if (secName.isBlank() || secName.equals("null", ignoreCase = true)) "Pastéis" else secName
            grouped.getOrPut(headerName) { mutableListOf() }.add(p)
        }
        grouped
    }

    val step = uiState.wizardStep
    val targetFlavors = uiState.wizardTargetFlavors

    val totalSteps = if (maxFlavors > 2) targetFlavors + 1 else targetFlavors
    val currentStepNumber = if (maxFlavors > 2) step + 1 else step
    val progress = if (totalSteps > 0) (currentStepNumber.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "pastel_progress_anim"
    )

    LaunchedEffect(step) {
        scrollState.animateScrollTo(0)
    }

    val stepTitle = when {
        step == 0 -> "Etapa 1 de $totalSteps: Quantos Sabores?"
        step in 1..targetFlavors -> {
            val ordinal = when (step) { 1 -> "1º"; 2 -> "2º"; 3 -> "3º"; else -> "${step}º" }
            val stepIdx = if (maxFlavors > 2) step + 1 else step
            "Etapa $stepIdx de $totalSteps: Escolha o $ordinal Sabor"
        }
        else -> "Etapa $totalSteps de $totalSteps: Complementos & Observações"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Monte Seu Pastel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stepTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onPrevStep) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = ItaSuperPrimary,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(180)))
                    }.using(SizeTransform(clip = false))
                },
                label = "wizard_step_anim"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = 164.dp)
                ) {
                    // STEP 0: Quantos Sabores?
                    if (currentStep == 0) {
                        Text(
                            text = "Quantos sabores você deseja?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monte seu pastel combinando até $maxFlavors sabores no mesmo pastel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF757575),
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        val options = (2..minOf(4, maxFlavors)).toList()
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            options.forEach { optCount ->
                                val selected = targetFlavors == optCount
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 104.dp)
                                        .clickable { onSelectTargetFlavors(optCount) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$optCount Sabores",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ItaSuperTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = when (optCount) {
                                                    2 -> "Meio a Meio (50% cada)"
                                                    3 -> "1/3 para cada sabor"
                                                    else -> "1/4 para cada sabor"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF757575)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        RadioButton(
                                            selected = selected,
                                            onClick = { onSelectTargetFlavors(optCount) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = ItaSuperPrimary,
                                                unselectedColor = Color(0xFF737373)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // STEP 1..N: Escolha dos Sabores
                    else if (currentStep in 1..targetFlavors) {
                        val flavorIndex = currentStep - 1
                        val ordinalText = when (currentStep) { 1 -> "1º"; 2 -> "2º"; 3 -> "3º"; else -> "${currentStep}º" }
                        val fractionStr = when (targetFlavors) {
                            2 -> "½"
                            3 -> "⅓"
                            4 -> "¼"
                            else -> "1/$targetFlavors"
                        }

                        // Top Progress Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ItaSuperPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Composição Atual:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                for (i in 0 until targetFlavors) {
                                    val flavorObj = uiState.wizardSelectedFlavors.getOrNull(i)
                                    val slotOrdinal = when (i + 1) { 1 -> "1º"; 2 -> "2º"; 3 -> "3º"; else -> "${i+1}º" }
                                    val isCurrentSlot = i == flavorIndex

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$fractionStr Pastel - ${flavorObj?.name ?: "($slotOrdinal sabor a escolher)"}",
                                            fontWeight = if (isCurrentSlot) FontWeight.Bold else FontWeight.Normal,
                                            color = if (flavorObj != null) Color.Black else if (isCurrentSlot) ItaSuperPrimary else Color.Gray,
                                            fontSize = 14.sp
                                        )
                                        if (flavorObj != null) {
                                            Text(
                                                text = String.format("R$ %.2f", flavorObj.price).replace(".", ","),
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = ItaSuperPrimary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val priceModeLabel = when (store.settings.pastelPriceMode.lowercase()) {
                                        "media" -> "Média dos valores"
                                        "soma" -> "Soma dos valores"
                                        else -> "Maior valor entre os escolhidos"
                                    }
                                    Text(
                                        text = "Valor base ($priceModeLabel):",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = String.format("R$ %.2f", uiState.wizardUnitPrice).replace(".", ","),
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Escolha o $ordinalText sabor ($fractionStr do pastel)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredPastelProducts.isEmpty()) {
                            Text(
                                text = "Nenhum sabor de pastel disponível nesta loja.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            productsBySection.forEach { (sectionHeader, sectionProducts) ->
                                Text(
                                    text = sectionHeader.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                )

                                sectionProducts.forEach { product ->
                                    val isSelected = uiState.wizardSelectedFlavors.getOrNull(flavorIndex)?.id == product.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSelectFlavorForStep(flavorIndex, product) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) ItaSuperPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(if (isSelected) 2.dp else 0.5.dp, if (isSelected) ItaSuperPrimary else Color.LightGray),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onSelectFlavorForStep(flavorIndex, product) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = product.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    if (!product.description.isNullOrBlank() && product.description.trim() != "null") {
                                                        Text(
                                                            text = product.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = String.format("R$ %.2f", product.price).replace(".", ","),
                                                fontWeight = FontWeight.Bold,
                                                color = ItaSuperPrimary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!uiState.wizardErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.wizardErrorMessage!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // FINAL STEP: Complementos & Notes
                    else {
                        val fractionStr = when (targetFlavors) {
                            2 -> "½"
                            3 -> "⅓"
                            4 -> "¼"
                            else -> "1/$targetFlavors"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ItaSuperPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Sabores Selecionados:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                uiState.wizardSelectedFlavors.filterNotNull().forEach { flav ->
                                    Text(
                                        text = "• $fractionStr ${flav.name} (${String.format("R$ %.2f", flav.price).replace(".", ",")})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Complementos Extras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.builderType == "pizza") "Escolha até $maxComplements complementos para sua pizza" else "Escolha até $maxComplements complementos para rechear seu pastel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val activeBorders = if (uiState.builderType == "pizza") uiState.pizzaBorders else uiState.pastelBorders

                        if (activeBorders.isEmpty()) {
                            Text(
                                text = "Nenhum complemento extra cadastrado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            activeBorders.forEach { border ->
                                val isChecked = uiState.wizardSelectedComplements.any { it.id == border.id }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onToggleComplement(border) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(if (isChecked) 1.5.dp else 0.5.dp, if (isChecked) ItaSuperPrimary else Color.LightGray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { onToggleComplement(border) },
                                                colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = border.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Text(
                                            text = if (border.price > 0) "+ ${String.format("R$ %.2f", border.price).replace(".", ",")}" else "Grátis",
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = uiState.wizardNotes,
                            onValueChange = onNotesChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pastel_wizard_notes_input"),
                            label = { Text("Alguma observação?") },
                            placeholder = { Text("Ex: Pastel bem frito, sem pimenta...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!uiState.wizardErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.wizardErrorMessage!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            PastelWizardSafeFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 64.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                step = step,
                targetFlavors = targetFlavors,
                quantity = uiState.wizardQuantity,
                totalPrice = uiState.wizardTotalPrice,
                hasSelectionForCurrentFlavor = step !in 1..targetFlavors || uiState.wizardSelectedFlavors.getOrNull(step - 1) != null,
                onNext = onNextStep,
                onPrevious = onPrevStep,
                onIncrement = onQuantityIncrement,
                onDecrement = onQuantityDecrement,
                onAdd = onAddToCartClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizzaWizardFullScreenContent(
    uiState: StoreDetailUiState,
    allProducts: List<Product>,
    onClose: () -> Unit,
    onSelectSize: (String?, String?) -> Unit,
    onSelectTargetFlavors: (Int) -> Unit,
    onSelectFlavor: (Int, Product) -> Unit,
    onToggleAddon: (AddonGroup, AddonItem) -> Unit,
    onSelectBorder: (PastelBorder) -> Unit,
    onNotesChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onAddToCart: () -> Unit
) {
    val store = uiState.store ?: return
    val settings = store.settings
    val scrollState = rememberScrollState()
    val catalogSizes = remember(settings.pizzaSizesCatalog) {
        settings.pizzaSizesCatalog.filter { it.active && it.maxFlavors >= 2 }
    }
    val legacySizes = remember(allProducts) {
        allProducts.flatMap { it.legacyPizzaSizes }
            .distinctBy { it.name }
    }
    val selectedCatalogSize = catalogSizes.firstOrNull { it.id == uiState.pizzaWizardSelectedSizeId }
    val selectedSizeMax = selectedCatalogSize?.maxFlavors ?: settings.pizzaMaxFlavors
    val maxFlavors = minOf(4, settings.pizzaMaxFlavors, selectedSizeMax).coerceAtLeast(2)
    val targetFlavors = uiState.pizzaWizardTargetFlavors.coerceIn(2, maxFlavors)
    val currentStep = uiState.pizzaWizardStep
    val activeBorders = uiState.pizzaBorders.filter { it.isAvailable }
    val hasAddons = uiState.pizzaWizardAddonGroups.isNotEmpty()
    val addonsStep = if (hasAddons) targetFlavors + 1 else -1
    val borderStep = if (activeBorders.isNotEmpty()) targetFlavors + if (hasAddons) 2 else 1 else -1
    val totalSteps = targetFlavors + if (hasAddons) 1 else 0 + if (activeBorders.isNotEmpty()) 1 else 0
    val lastContentStep = when {
        borderStep > 0 -> borderStep
        addonsStep > 0 -> addonsStep
        else -> targetFlavors
    }
    val isFinalStep = currentStep == lastContentStep
    val allFlavors = uiState.pizzaWizardSelectedFlavors.filterNotNull()
    val fraction = when (targetFlavors) {
        2 -> "½"
        3 -> "⅓"
        else -> "¼"
    }

    val menuSections = remember(uiState.menuSections) { uiState.menuSections.associateBy { it.id } }
    val pizzaProducts = remember(allProducts, uiState.pizzaWizardSelectedSizeId, menuSections) {
        val beverageKeywords = listOf("bebida", "drink", "suco", "refrigerante", "água", "agua", "cerveja", "energético", "energetico")
        allProducts.filter { product ->
            val sectionName = product.sectionId?.let { menuSections[it]?.name }.orEmpty()
            val isBeverageSection = beverageKeywords.any { keyword ->
                sectionName.contains(keyword, ignoreCase = true) || product.category.contains(keyword, ignoreCase = true)
            }
            product.isAvailable &&
                !product.isBeverage &&
                !isBeverageSection &&
                !product.pizzaUnavailableSizeIds.contains(uiState.pizzaWizardSelectedSizeId)
        }
    }
    val currentFlavorIndex = if (currentStep in 1..targetFlavors) currentStep - 1 else -1
    val currentSelectedId = uiState.pizzaWizardSelectedFlavors.getOrNull(currentFlavorIndex)?.id
    val alreadySelectedElsewhere = uiState.pizzaWizardSelectedFlavors
        .mapIndexedNotNull { index, product -> product?.id?.takeIf { index != currentFlavorIndex } }
        .toSet()
    val visibleFlavorProducts = pizzaProducts.filter { it.id !in alreadySelectedElsewhere }
    val flavorsBySection = remember(visibleFlavorProducts, menuSections) {
        visibleFlavorProducts.groupBy { product ->
            product.sectionId?.let { menuSections[it]?.name }
                ?.takeIf { it.isNotBlank() }
                ?: product.category.ifBlank { "Sabores" }
        }
    }

    fun money(value: Double): String = "R$ %.2f".format(value).replace('.', ',')
    val canAdvance = when {
        currentStep == 0 -> true
        currentStep in 1..targetFlavors -> uiState.pizzaWizardSelectedFlavors.getOrNull(currentStep - 1) != null
        currentStep == addonsStep -> uiState.pizzaWizardAddonGroups.all { group ->
            uiState.pizzaWizardSelectedAddonsMap[group.id].orEmpty().size >= group.minSelect
        }
        else -> true
    }

    BackHandler { onPrevious() }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Monte sua Pizza", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
                if (currentStep > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .background(
                                        if (index + 1 <= currentStep) ItaSuperPrimary else Color.LightGray.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 164.dp)
            ) {
            if (currentStep == 0) {
                Text("Comece por aqui", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Escolha o tamanho e quantos sabores diferentes você quer na sua pizza.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(20.dp))

                if (!settings.pizzaSingleSize && (catalogSizes.isNotEmpty() || legacySizes.isNotEmpty())) {
                    Text("Tamanho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (catalogSizes.isNotEmpty()) {
                            catalogSizes.forEach { size ->
                                val selected = size.id == uiState.pizzaWizardSelectedSizeId
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onSelectSize(size.id, size.name) },
                                    colors = CardDefaults.cardColors(containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selected, onClick = { onSelectSize(size.id, size.name) }, colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary))
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(size.name, fontWeight = FontWeight.Bold)
                                            val detail = listOf(size.description.takeIf { it.isNotBlank() }, "até ${size.maxFlavors} sabores").filterNotNull().joinToString(" • ")
                                            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
                            legacySizes.forEach { size ->
                                val selected = size.name == uiState.pizzaWizardSelectedSizeName
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onSelectSize(null, size.name) },
                                    colors = CardDefaults.cardColors(containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selected, onClick = { onSelectSize(null, size.name) }, colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary))
                                        Spacer(Modifier.width(8.dp))
                                        Text(size.name, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Text("Quantos sabores?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Cada parte terá a mesma proporção na pizza.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    (2..maxFlavors).forEach { count ->
                        val selected = targetFlavors == count
                        Card(
                            modifier = Modifier.weight(1f).clickable { onSelectTargetFlavors(count) },
                            colors = CardDefaults.cardColors(containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$count", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text(if (count == 2) "Meio a meio" else "$count sabores", style = MaterialTheme.typography.labelMedium)
                                Text(if (count == 2) "½ cada" else if (count == 3) "⅓ cada" else "¼ cada", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                if (allFlavors.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ItaSuperPrimary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Sua pizza", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                            Spacer(Modifier.height(5.dp))
                            allFlavors.forEach { flavor ->
                                Text("$fraction ${flavor.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            uiState.pizzaWizardSelectedBorder?.let { border ->
                                Text("Borda: ${border.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            if (allFlavors.size == targetFlavors) {
                                Spacer(Modifier.height(6.dp))
                                Text("Total unitário: ${money(uiState.pizzaWizardUnitPrice)}", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                when {
                    currentStep in 1..targetFlavors -> {
                        val ordinal = when (currentStep) { 1 -> "1º"; 2 -> "2º"; 3 -> "3º"; else -> "4º" }
                        Text("Escolha o $ordinal sabor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("$fraction da pizza", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(Modifier.height(14.dp))
                        if (flavorsBySection.isEmpty()) {
                            Text("Nenhum sabor disponível para este tamanho.", color = Color.Gray, modifier = Modifier.padding(vertical = 32.dp))
                        } else {
                            flavorsBySection.forEach { (section, sectionProducts) ->
                                Text(section.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ItaSuperPrimary, modifier = Modifier.padding(vertical = 6.dp))
                                sectionProducts.forEach { product ->
                                    val selected = product.id == currentSelectedId
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectFlavor(currentFlavorIndex, product) },
                                        colors = CardDefaults.cardColors(containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selected, onClick = { onSelectFlavor(currentFlavorIndex, product) }, colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary))
                                            if (product.imageUrl.isNotBlank()) {
                                                AsyncImage(model = product.imageUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                                Spacer(Modifier.width(10.dp))
                                            } else {
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(product.name, fontWeight = FontWeight.Bold)
                                                if (product.description.isNotBlank()) Text(product.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(money(product.price), fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    currentStep == addonsStep -> {
                        Text("Adicionais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Escolha as opções para sua pizza.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        uiState.pizzaWizardAddonGroups.forEach { group ->
                            val selectedItems = uiState.pizzaWizardSelectedAddonsMap[group.id].orEmpty()
                            Text(
                                text = buildString {
                                    append(group.name)
                                    if (group.minSelect > 0) append(" • mínimo ${group.minSelect}")
                                    if (group.maxSelect > 0) append(" • ${selectedItems.size}/${group.maxSelect}")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 5.dp)
                            )
                            uiState.pizzaWizardAddonItemsMap[group.id].orEmpty().forEach { item ->
                                val checked = selectedItems.any { it.id == item.id }
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onToggleAddon(group, item) },
                                    colors = CardDefaults.cardColors(containerColor = if (checked) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) ItaSuperPrimary else Color(0xFFE2E0DD)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = checked, onCheckedChange = { onToggleAddon(group, item) }, colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary))
                                        Spacer(Modifier.width(8.dp))
                                        Text(item.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                        Text(if (item.price > 0) "+ ${money(item.price)}" else "Grátis", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                                    }
                                }
                            }
                        }
                    }
                    currentStep == borderStep -> {
                        Text("Escolha a borda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        activeBorders.forEach { border ->
                            val selected = uiState.pizzaWizardSelectedBorder?.id == border.id
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectBorder(border) },
                                colors = CardDefaults.cardColors(containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E0DD)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selected, onClick = { onSelectBorder(border) }, colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary))
                                    Spacer(Modifier.width(8.dp))
                                    Text(border.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text(if (border.price > 0) "+ ${money(border.price)}" else "Grátis", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                                }
                            }
                        }
                    }
                }

                if (isFinalStep) {
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = uiState.pizzaWizardNotes,
                        onValueChange = onNotesChange,
                        modifier = Modifier.fillMaxWidth().testTag("pizza_wizard_notes_input"),
                        label = { Text("Observações") },
                        placeholder = { Text("Ex: Sem cebola, massa bem assada...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (!uiState.pizzaWizardErrorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(uiState.pizzaWizardErrorMessage!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(110.dp))
        }

        PizzaWizardSafeFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                    .padding(bottom = 64.dp)
                .navigationBarsPadding()
                .imePadding(),
            isFinalStep = isFinalStep,
            canAdvance = canAdvance,
            currentStep = currentStep,
            quantity = uiState.pizzaWizardQuantity,
            totalPrice = uiState.pizzaWizardTotalPrice,
            onNext = onNext,
            onPrevious = onPrevious,
            onIncrement = onQuantityIncrement,
            onDecrement = onQuantityDecrement,
            onAdd = onAddToCart
        )
        }
    }
}


@Composable
private fun PastelWizardSafeFooter(
    modifier: Modifier,
    step: Int,
    targetFlavors: Int,
    quantity: Int,
    totalPrice: Double,
    hasSelectionForCurrentFlavor: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            if (step == 0) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) { Text("Avançar", fontWeight = FontWeight.Bold) }
            } else if (step in 1..targetFlavors) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Anterior") }
                Button(
                    onClick = onNext,
                    enabled = hasSelectionForCurrentFlavor,
                    modifier = Modifier.weight(1.5f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) { Text(if (step == targetFlavors) "Complementos" else "Próximo", fontWeight = FontWeight.Bold) }
            } else {
                IconButton(onClick = onDecrement, enabled = quantity > 1, modifier = Modifier.border(1.dp, Color.LightGray, CircleShape)) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                }
                Text("$quantity", fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrement, modifier = Modifier.border(1.dp, Color.LightGray, CircleShape)) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                }
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) { Text("Adicionar • ${String.format("R$ %.2f", totalPrice).replace(".", ",")}", fontWeight = FontWeight.Bold, maxLines = 1) }
            }
    }
}

@Composable
private fun PizzaWizardSafeFooter(
    modifier: Modifier,
    isFinalStep: Boolean,
    canAdvance: Boolean,
    currentStep: Int,
    quantity: Int,
    totalPrice: Double,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            if (!isFinalStep) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier.heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Anterior") }
                }
                Button(
                    onClick = onNext,
                    enabled = canAdvance,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) { Text(if (currentStep == 0) "Continuar" else "Próximo", fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Anterior") }
                IconButton(onClick = onDecrement, enabled = quantity > 1, modifier = Modifier.border(1.dp, Color.LightGray, CircleShape)) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                }
                Text("$quantity", fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrement, modifier = Modifier.border(1.dp, Color.LightGray, CircleShape)) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                }
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) { Text("Adicionar", fontWeight = FontWeight.Bold) }
            }
    }
}
