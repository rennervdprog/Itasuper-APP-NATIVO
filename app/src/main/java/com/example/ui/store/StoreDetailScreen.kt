package com.example.ui.store

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.store?.name ?: "Detalhes da Loja",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Banner & Store Information
                item {
                    StoreHeaderSection(store = store)
                }

                // Search inside Store Menu
                item {
                    PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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

                // Category Tabs
                if (uiState.categories.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.categories) { cat ->
                                val isSelected = cat == uiState.selectedCategory
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = {
                                        Text(
                                            text = cat,
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
                }

                // Products Count Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cardápio (${products.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Products Items
                if (products.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum produto encontrado neste filtro.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onCardClick = { viewModel.openProductModal(product) },
                            onAddClick = { viewModel.addDirectProductToCart(product) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Modal Bottom Sheet for Product Detail
        if (uiState.selectedProductForModal != null) {
            val product = uiState.selectedProductForModal!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeProductModal() },
                sheetState = sheetState
            ) {
                ProductDetailSheetContent(
                    product = product,
                    quantity = uiState.modalQuantity,
                    notes = uiState.modalNotes,
                    onQuantityIncrement = { viewModel.incrementModalQuantity() },
                    onQuantityDecrement = { viewModel.decrementModalQuantity() },
                    onNotesChange = { viewModel.updateModalNotes(it) },
                    onAddToCartClick = { viewModel.addSelectedProductToCart() }
                )
            }
        }
    }
}

@Composable
fun StoreHeaderSection(store: Store?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Banner Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
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

        // Store Details Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store?.name ?: "Loja ItaSuper",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = store?.category ?: "Alimentação",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Status Badge
                val isOpen = store?.isOpen ?: true
                Surface(
                    color = if (isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isOpen) "Aberto" else "Fechado",
                        color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Chips Row (Rating, Time, Fee, Min order)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Nota",
                        tint = ItaSuperWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${store?.rating ?: 5.0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Time
                Text(
                    text = "• ${store?.deliveryTime ?: "30-40 min"}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                // Fee
                Text(
                    text = "• Taxa: ${store?.deliveryFee ?: "Grátis"}",
                    fontSize = 14.sp,
                    color = if (store?.isFreeDelivery == true) Color(0xFF2E7D32) else Color.DarkGray,
                    fontWeight = if (store?.isFreeDelivery == true) FontWeight.Bold else FontWeight.Normal
                )
            }
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

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("R$ %.2f", product.price).replace(".", ","),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary
                    )

                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("R$ %.2f", product.originalPrice).replace(".", ","),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Add button / Image
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(8.dp))
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

                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ItaSuperPrimary, CircleShape)
                        .testTag("add_product_button_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar ao carrinho",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailSheetContent(
    product: Product,
    quantity: Int,
    notes: String,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddToCartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = String.format("R$ %.2f", product.price).replace(".", ","),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ItaSuperPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Observações
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_notes_input"),
            label = { Text("Alguma observação?") },
            placeholder = { Text("Ex: Sem cebola, capricha no molho...") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

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
                IconButton(onClick = onQuantityDecrement, enabled = quantity > 1) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir")
                }
                Text(
                    text = "$quantity",
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
            val total = product.price * quantity
            Button(
                onClick = onAddToCartClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("confirm_add_to_cart_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Adicionar • ${String.format("R$ %.2f", total).replace(".", ",")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
