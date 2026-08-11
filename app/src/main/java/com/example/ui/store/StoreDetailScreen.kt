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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AddonGroup
import com.example.data.model.AddonItem
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSecondary
import com.example.ui.theme.ItaSuperWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    viewModel: StoreDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    LaunchedEffect(storeId) {
        viewModel.loadStore(storeId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val cartState by viewModel.cartState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Buscar no cardápio...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("appbar_search_input"),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ItaSuperPrimary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = uiState.store?.name ?: "Detalhes da Loja",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                viewModel.onSearchQueryChange("")
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSearchActive) ItaSuperPrimary.copy(alpha = 0.15f) else ItaSuperSecondary,
                                CircleShape
                            )
                            .testTag("topappbar_search_icon")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar no cardápio",
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val phone = uiState.store?.whatsapp?.ifBlank { "5521999999999" } ?: "5521999999999"
                            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode("Olá! Vim pelo app ItaSuper Delivery.")}")
                            val intent = Intent(Intent.ACTION_VIEW, whatsappUri)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                            .testTag("topappbar_whatsapp_icon")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp da Loja",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Floating Cart Summary Bar
            AnimatedVisibility(visible = cartState.totalItemCount > 0) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = onNavigateToCart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("view_cart_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${cartState.totalItemCount}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Ver Sacola",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = String.format("R$ %.2f", cartState.subtotal).replace(".", ","),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
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
        } else {
            val store = uiState.store

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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Banner & Store Information
                item {
                    StoreHeaderSection(
                        store = store,
                        productsCount = products.size,
                        categoriesCount = if (uiState.menuSections.isNotEmpty()) uiState.menuSections.size else groupedProducts.keys.size
                    )
                }

                // Monte Sua Pizza / Monte Seu Pastel Custom Builder Buttons
                val hasPizzaCategory = store?.let { s ->
                    s.category.lowercase().contains("pizza") || s.secondaryCategories.any { it.lowercase().contains("pizza") }
                } ?: false

                val hasPastelCategory = store?.let { s ->
                    s.category.lowercase().contains("pastel") || s.category.lowercase().contains("pasteis") ||
                    s.secondaryCategories.any { it.lowercase().contains("pastel") || it.lowercase().contains("pasteis") }
                } ?: false

                val showPizzaBuilderButton = hasPizzaCategory && (store?.settings?.pizzaHalfEnabled != false) && products.isNotEmpty()
                val showPastelBuilderButton = hasPastelCategory && (store?.settings?.pastelHalfEnabled != false) && products.isNotEmpty()

                if (showPizzaBuilderButton || showPastelBuilderButton) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showPizzaBuilderButton) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.openBuilder("pizza") }
                                        .testTag("monte_sua_pizza_button"),
                                    colors = CardDefaults.cardColors(containerColor = ItaSuperPrimary.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalPizza,
                                            contentDescription = "Monte Sua Pizza",
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Monte Sua Pizza",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ItaSuperPrimary
                                            )
                                            Text(
                                                text = "Meio a Meio",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            if (showPastelBuilderButton) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.openBuilder("pastel") }
                                        .testTag("monte_seu_pastel_button"),
                                    colors = CardDefaults.cardColors(containerColor = ItaSuperSecondary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = "Monte Seu Pastel",
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Monte Seu Pastel",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ItaSuperPrimary
                                            )
                                            Text(
                                                text = "Meio a Meio",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search inside Store Menu
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("menu_search_input"),
                        placeholder = { Text("Buscar no cardápio de ${store?.name ?: "esta loja"}...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ItaSuperPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }

                // Menu Section Chips (Real menu_sections query)
                val allSectionNames = listOf("Todos") + uiState.menuSections.map { it.name }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(allSectionNames, key = { idx, secName -> "sec_chip_${idx}_$secName" }) { _, secName ->
                            val isSelected = secName.equals(uiState.selectedSectionName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSection(secName) },
                                label = {
                                    Text(
                                        text = secName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ItaSuperPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
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
                    groupedProducts.entries.forEachIndexed { secIndex, (secName, secProducts) ->
                        // Section Header showing Section Name and Item Count
                        item(key = "section_header_${secIndex}_$secName") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = secName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = ItaSuperTextPrimary
                                        )
                                    )
                                    Surface(
                                        color = ItaSuperPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${secProducts.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ItaSuperPrimary,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(secProducts, key = { pIndex, prod -> "prod_${secIndex}_${pIndex}_${prod.id}" }) { _, product ->
                            ProductItemCard(
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
                        decorFitsSystemWindows = false
                    )
                ) {
                    PastelWizardFullScreenContent(
                        uiState = uiState,
                        allProducts = products,
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
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeBuilderModal() },
                    sheetState = sheetState
                ) {
                    CustomBuilderSheetContent(
                        uiState = uiState,
                        allProducts = products,
                        onToggleFlavor = { viewModel.toggleBuilderFlavor(it) },
                        onSelectSize = { viewModel.setBuilderSize(it) },
                        onToggleComplement = { viewModel.toggleBuilderComplement(it) },
                        onNotesChange = { viewModel.updateBuilderNotes(it) },
                        onQuantityIncrement = { viewModel.incrementBuilderQuantity() },
                        onQuantityDecrement = { viewModel.decrementBuilderQuantity() },
                        onAddToCartClick = { viewModel.addBuilderToCart() }
                    )
                }
            }
        }
    }
}

@Composable
fun StoreHeaderSection(
    store: Store?,
    productsCount: Int = 0,
    categoriesCount: Int = 0
) {
    val context = LocalContext.current
    var isHoursExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Banner & Overlapping Logo Container
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            // 1. Banner Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(ItaSuperSecondary)
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
                            .background(ItaSuperPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = ItaSuperPrimary
                        )
                    }
                }
            }

            // 2. Overlapping Circular Logo (100dp)
            Surface(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .size(100.dp)
                    .testTag("store_logo_overlay"),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(4.dp, Color.White)
            ) {
                if (!store?.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = store?.logoUrl,
                        contentDescription = store?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ItaSuperSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = ItaSuperPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(58.dp))

        // Store Details Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Store Title
            Text(
                text = store?.name ?: "Loja ItaSuper",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Row: Category, Rating, Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = store?.category ?: "Alimentação",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Nota",
                        tint = ItaSuperWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${store?.rating ?: 5.0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isOpen = store?.isOpen ?: true
                Surface(
                    color = if (isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isOpen) "Aberto" else "Fechado",
                        color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Address Line + MAPS Button
            val addressText = store?.address?.ifBlank { "Av. 22 de Maio, Centro, Itaboraí - RJ" } ?: "Av. 22 de Maio, Centro, Itaboraí - RJ"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = addressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = ItaSuperTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clickable {
                                val lat = store?.latitude ?: -22.7461
                                val lng = store?.longitude ?: -42.8588
                                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=${Uri.encode(addressText)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                                    context.startActivity(browserIntent)
                                }
                            }
                            .testTag("open_maps_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = ItaSuperPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MAPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 3 Cards Side by Side (Taxa, Tempo, Pedido Mín.)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card Taxa
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ItaSuperSecondary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Taxa",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = store?.deliveryFee ?: "Grátis",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (store?.isFreeDelivery == true) Color(0xFF2E7D32) else ItaSuperTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Card Tempo
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ItaSuperSecondary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tempo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = store?.deliveryTime ?: "30-40 min",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Card Pedido Mín.
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ItaSuperSecondary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pedido Mín.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("R$ %.2f", store?.minOrder ?: 15.0).replace('.', ','),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Seção expansível "Horários"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHoursExpanded = !isHoursExpanded }
                    .testTag("expand_hours_button"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Horários de Funcionamento",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = if (isHoursExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHoursExpanded) "Recolher" else "Expandir",
                            tint = Color.Gray
                        )
                    }

                    if (isHoursExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        val hoursList = store?.openingHours
                        if (!hoursList.isNullOrEmpty()) {
                            hoursList.forEach { oh ->
                                val dayLabel = when {
                                    oh.dayOfWeekStr.isNotBlank() -> oh.dayOfWeekStr.replaceFirstChar { it.uppercase() }
                                    oh.dayOfWeek == 1 -> "Domingo"
                                    oh.dayOfWeek == 2 -> "Segunda-feira"
                                    oh.dayOfWeek == 3 -> "Terça-feira"
                                    oh.dayOfWeek == 4 -> "Quarta-feira"
                                    oh.dayOfWeek == 5 -> "Quinta-feira"
                                    oh.dayOfWeek == 6 -> "Sexta-feira"
                                    oh.dayOfWeek == 7 -> "Sábado"
                                    else -> "Dia da Semana"
                                }
                                val timeLabel = if (oh.isClosedAllDay) "Fechado" else "${oh.openTime} às ${oh.closeTime}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = dayLabel, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (oh.isClosedAllDay) Color.Red else ItaSuperPrimary
                                        )
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Segunda a Domingo", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                Text(text = "11:00 às 23:30", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Seção "Pagamento" com chips (Cartão, Dinheiro, PIX)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Formas de Pagamento Aceitas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val paymentMethods = listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Dinheiro")
                        paymentMethods.forEach { method ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ItaSuperSecondary.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = ItaSuperTextPrimary,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Contador "X Produtos" e "Y Categorias" no rodapé da seção Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = ItaSuperHighlightBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$productsCount Produtos",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperHighlightText,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperHighlightText
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$categoriesCount Categorias",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperHighlightText,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCardClick() }
            .testTag("product_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!product.description.isNullOrBlank() && product.description.trim() != "null") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("R$ %.2f", product.price).replace(".", ","),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperPrimary
                        )

                        if (product.originalPrice != null && product.originalPrice > product.price) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("R$ %.2f", product.originalPrice).replace(".", ","),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    // Botão Pill "Adicionar" ao lado do preço
                    Surface(
                        modifier = Modifier
                            .clickable { onAddClick() }
                            .testTag("add_product_button_${product.id}"),
                        shape = RoundedCornerShape(20.dp),
                        color = ItaSuperPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Adicionar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Clean Product Image (without any button overlay)
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ItaSuperSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            }
        }
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
    onAddToCartClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
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

        // Addon Groups Section
        if (uiState.modalAddonGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            uiState.modalAddonGroups.forEach { group ->
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

        Spacer(modifier = Modifier.height(16.dp))

        // Observações
        OutlinedTextField(
            value = uiState.modalNotes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_notes_input"),
            label = { Text("Alguma observação?") },
            placeholder = { Text("Ex: Sem cebola, capricha no molho...") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

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

            // Submit Add Button
            Button(
                onClick = onAddToCartClick,
                enabled = uiState.canAddToCart,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("confirm_add_to_cart_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Adicionar • ${String.format("R$ %.2f", uiState.modalTotalPrice).replace(".", ",")}",
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
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step == 0) {
                            Button(
                                onClick = onNextStep,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                            ) {
                                Text(
                                    text = "Avançar para Escolha de Sabores",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (step in 1..targetFlavors) {
                            val flavorIndex = step - 1
                            val hasSelectionForThisStep = uiState.wizardSelectedFlavors.getOrNull(flavorIndex) != null

                            OutlinedButton(
                                onClick = onPrevStep,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Anterior", textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onNextStep,
                                enabled = hasSelectionForThisStep,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                            ) {
                                Text(
                                    text = if (step == targetFlavors) "Ir para Complementos" else "Próximo Sabor",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Final step
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                IconButton(onClick = onQuantityDecrement, enabled = uiState.wizardQuantity > 1) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir")
                                }
                                Text(
                                    text = "${uiState.wizardQuantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(onClick = onQuantityIncrement) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar")
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = onAddToCartClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("confirm_pastel_wizard_add_to_cart"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                            ) {
                                Text(
                                    text = "Adicionar • ${String.format("R$ %.2f", uiState.wizardTotalPrice).replace(".", ",")}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
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
                ) {
                    // STEP 0: Quantos Sabores?
                    if (currentStep == 0) {
                Text(
                    text = "Quantos sabores você deseja?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monte seu pastel combinando até $maxFlavors sabores no mesmo pastel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                val options = (2..minOf(4, maxFlavors)).toList()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    options.forEach { optCount ->
                        val selected = targetFlavors == optCount
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTargetFlavors(optCount) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) ItaSuperPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color.LightGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "$optCount Sabores",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = when (optCount) {
                                            2 -> "Meio a Meio (50% cada)"
                                            3 -> "1/3 para cada sabor"
                                            else -> "1/4 para cada sabor"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectTargetFlavors(optCount) },
                                    colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary)
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
                    text = if (isPizza) "Escolha até $maxComplements complementos para sua pizza" else "Escolha até $maxComplements complementos para rechear seu pastel",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                val activeBorders = if (isPizza) uiState.pizzaBorders else uiState.pastelBorders

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
}
}
}
