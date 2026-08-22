package com.example.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DiscoverProduct
import com.example.data.model.Store
import com.example.data.repository.SearchCategory
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperBackground
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary
import com.example.ui.theme.ManropeFontFamily
import java.util.Locale

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToStore: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val city by viewModel.activeCity.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val featuredProducts by viewModel.featuredProducts.collectAsStateWithLifecycle()
    val searchableProducts by viewModel.searchableProducts.collectAsStateWithLifecycle()
    val trendingStores by viewModel.trendingStores.collectAsStateWithLifecycle()
    val filteredStores by viewModel.filteredStores.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.synchronizeLocation(context) }

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "busca",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = ItaSuperBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            DiscoverHeader(
                city = city,
                query = uiState.rawQuery,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onSubmit = {
                    viewModel.submitSearch(uiState.rawQuery)
                    focusManager.clearFocus()
                }
            )

            if (isSearchActive) {
                DiscoverResults(
                    query = uiState.debouncedQuery,
                    selectedCategory = viewModel.searchCategories.firstOrNull { it.id == uiState.selectedCategoryId },
                    selectedQuickFilter = uiState.activeQuickFilter,
                    products = searchableProducts,
                    stores = filteredStores,
                    onClearAll = viewModel::clearDiscovery,
                    onNavigateToStore = onNavigateToStore
                )
            } else {
                DiscoverLanding(
                    city = city,
                    quickFilter = uiState.activeQuickFilter,
                    categories = viewModel.searchCategories,
                    featuredProducts = featuredProducts,
                    trendingStores = trendingStores,
                    recentSearches = recentSearches,
                    onQuickFilter = viewModel::toggleQuickFilter,
                    onCategory = viewModel::onCategorySelect,
                    onRecentSearch = viewModel::onRecentSearchSelect,
                    onClearRecentSearches = viewModel::clearRecentSearches,
                    onNavigateToStore = onNavigateToStore
                )
            }
        }
    }
}

@Composable
private fun DiscoverHeader(
    city: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ItaSuperBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Descobrir",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = ItaSuperTextPrimary,
                fontSize = 20.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_map_pin),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = city.ifBlank { "Sua cidade" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ManropeFontFamily,
                                    fontWeight = FontWeight.Bold,
                color = ItaSuperTextPrimary,
                fontSize = 15.sp

                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("search_input"),
            placeholder = {
                Text(
                    text = "Buscar produtos, lojas e categorias",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFontFamily,
                        color = ItaSuperTextSecondary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.ic_lucide_search), contentDescription = null, tint = Color.Unspecified)
            },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = onClearQuery, modifier = Modifier.testTag("clear_search_button")) {
                        Icon(painter = painterResource(R.drawable.ic_ita_close), contentDescription = "Limpar busca", tint = Color.Unspecified)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE1E1E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() })
        )
    }
}

@Composable
private fun DiscoverLanding(
    city: String,
    quickFilter: DiscoverQuickFilter?,
    categories: List<SearchCategory>,
    featuredProducts: List<DiscoverProduct>,
    trendingStores: List<Store>,
    recentSearches: List<String>,
    onQuickFilter: (DiscoverQuickFilter) -> Unit,
    onCategory: (String) -> Unit,
    onRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onNavigateToStore: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            DiscoverQuickFilters(
                selected = quickFilter,
                onClick = onQuickFilter
            )
        }
        item {
            DiscoverCuratedCategories(
                categories = categories,
                onSelect = onCategory
            )
        }
        if (featuredProducts.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "Em alta em ${city.ifBlank { "sua cidade" }}",
                    action = "Ver tudo"
                )
            }
            itemsIndexed(featuredProducts, key = { index, product -> "discover_product_${index}_${product.id}" }) { _, product ->
                DiscoverProductCard(product = product, onClick = { onNavigateToStore(product.storeId) })
            }
        } else if (trendingStores.isNotEmpty()) {
            item { DiscoverSectionHeader(title = "Em alta em ${city.ifBlank { "sua cidade" }}") }
            itemsIndexed(trendingStores.take(4), key = { index, store -> "discover_store_${index}_${store.id}" }) { _, store ->
                DiscoverStoreCard(store = store, onClick = { onNavigateToStore(store.id) })
            }
        }
        if (recentSearches.isNotEmpty()) {
            item {
                DiscoverRecentSearches(
                    searches = recentSearches,
                    onSearch = onRecentSearch,
                    onClear = onClearRecentSearches
                )
            }
        }
    }
}

@Composable
private fun DiscoverQuickFilters(
    selected: DiscoverQuickFilter?,
    onClick: (DiscoverQuickFilter) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DiscoverQuickFilter.entries.toList()) { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onClick(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = if (filter == DiscoverQuickFilter.DELIVERY_AVAILABLE) {
                        { Icon(painter = painterResource(R.drawable.ic_ita_delivery), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Color(0xFFFFF4EC),
                        selectedLabelColor = ItaSuperPrimary,
                        selectedLeadingIconColor = ItaSuperPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFFE1E1E1),
                        selectedBorderColor = ItaSuperPrimary,
                        enabled = true,
                        selected = selected == filter
                    )
                )
            }
        }
    }
}

@Composable
private fun DiscoverCuratedCategories(
    categories: List<SearchCategory>,
    onSelect: (String) -> Unit
) {
    val wanted = listOf("pizzaria", "acai", "marmita", "farmacias")
    val curated = wanted.mapNotNull { wantedId ->
        categories.firstOrNull { it.id.equals(wantedId, ignoreCase = true) }
    }
    if (curated.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Explorar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = ItaSuperTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ItaSuperPrimary)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            curated.forEach { category ->
                val iconRes = when (category.id) {
                    "pizzaria" -> R.drawable.ic_lucide_pizza
                    "acai" -> R.drawable.ic_lucide_soup
                    "farmacias" -> R.drawable.ic_lucide_pharmacy
                    else -> R.drawable.ic_lucide_package_open
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(category.id) }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = category.name,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF262626),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverSectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = ItaSuperTextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperPrimary,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun DiscoverProductCard(product: DiscoverProduct, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(painter = painterResource(R.drawable.ic_ita_bag), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.storeName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily,
                        color = ItaSuperTextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = String.format(Locale("pt", "BR"), "R$ %.2f", product.price).replace('.', ','),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperPrimary,
                        fontSize = 15.sp
                    )
                )
            }
            Icon(painter = painterResource(R.drawable.ic_ita_chevron_right), contentDescription = "Abrir loja", tint = Color.Unspecified, modifier = Modifier.size(23.dp))
        }
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
    }
}

@Composable
private fun DiscoverStoreCard(store: Store, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (store.logoUrl.isNotBlank() || store.bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = store.logoUrl.ifBlank { store.bannerUrl },
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(painter = painterResource(R.drawable.ic_ita_store), contentDescription = null, tint = Color.Unspecified)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, color = ItaSuperTextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOf(store.category, store.deliveryTime, store.deliveryFee).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFontFamily, color = ItaSuperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(painter = painterResource(R.drawable.ic_ita_chevron_right), contentDescription = "Abrir loja", tint = Color.Unspecified, modifier = Modifier.size(23.dp))
        }
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
    }
}

@Composable
private fun DiscoverRecentSearches(
    searches: List<String>,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Buscas recentes",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, color = ItaSuperTextPrimary)
            )
            Text(
                text = "Limpar",
                modifier = Modifier.clickable(onClick = onClear),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
            )
        }
        Spacer(modifier = Modifier.height(9.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searches.take(6)) { term ->
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSearch(term) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E1E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_ita_clock), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                        Text(term, fontFamily = ManropeFontFamily, fontSize = 13.sp, color = ItaSuperTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverResults(
    query: String,
    selectedCategory: SearchCategory?,
    selectedQuickFilter: DiscoverQuickFilter?,
    products: List<DiscoverProduct>,
    stores: List<Store>,
    onClearAll: () -> Unit,
    onNavigateToStore: (String) -> Unit
) {
    if (products.isEmpty() && stores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFF4EC), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(R.drawable.ic_lucide_search), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Nada encontrado", style = MaterialTheme.typography.titleMedium.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, color = ItaSuperTextPrimary))
                Spacer(modifier = Modifier.height(5.dp))
                Text("Tente outro termo ou ajuste a descoberta.", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFontFamily, color = ItaSuperTextSecondary))
                Spacer(modifier = Modifier.height(15.dp))
                Text("Voltar para Descobrir", modifier = Modifier.clickable(onClick = onClearAll).testTag("clear_discovery_button"), style = MaterialTheme.typography.labelLarge.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, color = ItaSuperPrimary))
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearAll)
                    .testTag("clear_discovery_button")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ita_chevron_right),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp).graphicsLayer(rotationZ = 180f)
                )
                Text(
                    text = "Voltar para Descobrir",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary,
                        fontSize = 13.sp
                    )
                )
            }
        }
        item {
            val label = selectedCategory?.name ?: selectedQuickFilter?.label ?: query.ifBlank { "Resultados" }
            Text(
                text = "Resultados para $label",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, color = ItaSuperTextPrimary),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
            )
        }
        if (products.isNotEmpty()) {
            item {
                Text("Produtos", style = MaterialTheme.typography.labelLarge.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, color = ItaSuperTextSecondary), modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
            itemsIndexed(products, key = { index, product -> "result_product_${index}_${product.id}" }) { _, product ->
                DiscoverProductCard(product = product, onClick = { onNavigateToStore(product.storeId) })
            }
        }
        if (stores.isNotEmpty()) {
            item {
                Text("Lojas", style = MaterialTheme.typography.labelLarge.copy(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, color = ItaSuperTextSecondary), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
            itemsIndexed(stores, key = { index, store -> "result_store_${index}_${store.id}" }) { _, store ->
                DiscoverStoreCard(store = store, onClick = { onNavigateToStore(store.id) })
            }
        }
    }
}
