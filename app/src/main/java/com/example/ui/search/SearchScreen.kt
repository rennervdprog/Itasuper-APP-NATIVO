package com.example.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DiscoverProduct
import com.example.data.model.Store
import com.example.data.repository.SearchCategory
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.permissions.LocationAndPermissionsDialog
import com.example.ui.permissions.PermissionUtils
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ManropeFontFamily
import java.util.Locale

// ===== Tokens visuais do protótipo (ideias-para-aba-descobrir.html) =====
// O protótipo usa --brand:#d92b1f; aqui o brand é o token real do produto (ItaSuperPrimary).
private val DscBg = Color(0xFFFFFCF9)      // --phone-bg
private val DscInk = Color(0xFF221812)     // --phone-ink
private val DscInk2 = Color(0xFF6F6154)    // --phone-ink-2
private val DscLine = Color(0xFFEEE3D6)     // --phone-line
private val DscChipBg = Color(0xFFF4EDE5)   // --chip-bg
private val DscSkel = Color(0xFFEADFD2)     // --skel
private val DscAmber = Color(0xFFE8930C)    // --amber (estrelas)
private val DscGreen = Color(0xFF177A3D)    // --green
private val DscBrown = Color(0xFF8A4B0F)    // banner-3

private fun dscMoney(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", value).replace('.', ',')

private fun dscRating(rating: Double): String =
    String.format(Locale("pt", "BR"), "%.1f", rating).replace('.', ',')

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToStore: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    initialCategoryId: String? = null
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
    val promoBanners by viewModel.promoBanners.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val orderAgainItems by viewModel.orderAgainItems.collectAsStateWithLifecycle()
    val dailyDeals by viewModel.dailyDeals.collectAsStateWithLifecycle()
    val utilityStores by viewModel.utilityStores.collectAsStateWithLifecycle()
    val recommendedStores by viewModel.recommendedStores.collectAsStateWithLifecycle()
    val couponStoreIds by viewModel.couponStoreIds.collectAsStateWithLifecycle()
    var showPermissionsDialog by remember { mutableStateOf(false) }

    LocationAndPermissionsDialog(
        showDialog = showPermissionsDialog,
        onDismiss = { showPermissionsDialog = false },
        onLocationPermissionResult = { granted ->
            if (granted) viewModel.requestGpsLocation(context)
        }
    )

    LaunchedEffect(Unit) { viewModel.synchronizeLocation(context) }

    // A categoria chega pela rota quando a Home envia o cliente para o catálogo
    // completo de um destaque; o ViewModel garante que só é aplicada uma vez.
    LaunchedEffect(initialCategoryId) { viewModel.applyInitialCategory(initialCategoryId) }

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "busca",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = DscBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DscBg)
                .padding(paddingValues)
                .imePadding()
        ) {
            DiscoverAppBar(
                city = city,
                query = uiState.rawQuery,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onLocationClick = {
                    if (PermissionUtils.hasLocationPermission(context)) {
                        viewModel.requestGpsLocation(context)
                    } else {
                        showPermissionsDialog = true
                    }
                },
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
                    recentSearches = recentSearches,
                    onClearAll = viewModel::clearDiscovery,
                    onRecentSearch = viewModel::onRecentSearchSelect,
                    onNavigateToStore = onNavigateToStore
                )
            } else {
                DiscoverLanding(
                    isLoadingDiscovery = uiState.isLoadingDiscovery,
                    quickFilter = uiState.activeQuickFilter,
                    categories = viewModel.searchCategories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    featuredProducts = featuredProducts,
                    trendingStores = trendingStores,
                    promoBanners = promoBanners,
                    collections = collections,
                    orderAgainItems = orderAgainItems,
                    dailyDeals = dailyDeals,
                    utilityStores = utilityStores,
                    recommendedStores = recommendedStores,
                    couponStoreIds = couponStoreIds,
                    onQuickFilter = viewModel::toggleQuickFilter,
                    onCategory = viewModel::onCategorySelect,
                    onToggleCollectionSaved = viewModel::toggleCollectionSaved,
                    onReorder = viewModel::reorderItem,
                    onNavigateToStore = onNavigateToStore
                )
            }
        }
    }
}

/** Appbar do protótipo: "Entregar em" + endereço + barra de busca. */
@Composable
private fun DiscoverAppBar(
    city: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLocationClick: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DscBg)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Entregar em",
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = DscInk2
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLocationClick)
                .padding(vertical = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = city.ifBlank { "Sua cidade" },
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = DscInk
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DscChipBg)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = DscInk2,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                textStyle = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontSize = 14.sp,
                    color = DscInk
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSubmit()
                    focusManager.clearFocus()
                }),
                cursorBrush = SolidColor(ItaSuperPrimary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar restaurantes e pratos",
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontSize = 14.sp,
                                color = DscInk2
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
            if (query.isNotBlank()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpar busca",
                        tint = DscInk2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** Feed da Descoberta na ordem exata do protótipo. */
@Composable
private fun DiscoverLanding(
    isLoadingDiscovery: Boolean,
    quickFilter: DiscoverQuickFilter?,
    categories: List<SearchCategory>,
    selectedCategoryId: String?,
    featuredProducts: List<DiscoverProduct>,
    trendingStores: List<Store>,
    promoBanners: List<PromoBanner>,
    collections: List<DiscoverCollection>,
    orderAgainItems: List<OrderAgainItem>,
    dailyDeals: List<DailyDeal>,
    utilityStores: List<Store>,
    recommendedStores: List<Store>,
    couponStoreIds: Set<String>,
    onQuickFilter: (DiscoverQuickFilter) -> Unit,
    onCategory: (String) -> Unit,
    onToggleCollectionSaved: (String) -> Unit,
    onReorder: (OrderAgainItem) -> Unit,
    onNavigateToStore: (String) -> Unit
) {
    // "Perto de você": usa as lojas em alta; se vazio, cai para as recomendadas.
    val nearStores = trendingStores.ifEmpty { recommendedStores }
    val allStoresById = remember(trendingStores, recommendedStores, utilityStores) {
        (trendingStores + recommendedStores + utilityStores).associateBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        // Filtros rápidos
        item {
            DiscoverFilterChips(
                selected = quickFilter,
                onClick = onQuickFilter
            )
        }
        // Categorias (grade 2 linhas)
        item {
            DiscoverCategoryGrid(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = onCategory
            )
        }
        if (isLoadingDiscovery && featuredProducts.isEmpty() && nearStores.isEmpty()) {
            item { DiscoverShimmerLoading() }
        } else {
            // Carrossel de promos
            if (promoBanners.isNotEmpty()) {
                item {
                    PromoCarousel(
                        banners = promoBanners,
                        onBannerClick = { banner -> onNavigateToStore(banner.storeId) }
                    )
                }
            }
            // Peça de novo
            if (orderAgainItems.isNotEmpty()) {
                item { PSecHeader(title = "Peça de novo") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orderAgainItems, key = { it.productId }) { item ->
                            DishCard(item = item, onReorder = { onReorder(item) })
                        }
                    }
                }
            }
            // Perto de você (lista vertical)
            if (nearStores.isNotEmpty()) {
                item {
                    NearYouSection(
                        stores = nearStores,
                        couponStoreIds = couponStoreIds,
                        onNavigateToStore = onNavigateToStore
                    )
                }
            }
            // Coleções
            if (collections.isNotEmpty()) {
                item { PSecHeader(title = "Coleções") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(collections, key = { it.id }) { collection ->
                            ProtoCollectionCard(
                                collection = collection,
                                onToggleSave = { onToggleCollectionSaved(collection.id) },
                                onClick = {
                                    collection.stores.firstOrNull()?.let { store ->
                                        onNavigateToStore(store.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // Ofertas do dia
            if (dailyDeals.isNotEmpty()) {
                item {
                    DealsSection(
                        deals = dailyDeals,
                        storesById = allStoresById,
                        onDealClick = { deal ->
                            deal.storeId?.let { storeId -> onNavigateToStore(storeId) }
                        }
                    )
                }
            }
            // Mercados e mais
            if (utilityStores.isNotEmpty()) {
                item {
                    UtilitySection(
                        stores = utilityStores,
                        onNavigateToStore = onNavigateToStore
                    )
                }
            }
        }
    }
}

/** Filtros rápidos do protótipo (.chips / .fchip). */
@Composable
private fun DiscoverFilterChips(
    selected: DiscoverQuickFilter?,
    onClick: (DiscoverQuickFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverQuickFilter.entries.toList()) { filter ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) DscInk else DscLine,
                        shape = RoundedCornerShape(50)
                    )
                    .background(if (isSelected) DscInk else Color.Transparent)
                    .clickable { onClick(filter) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    text = filter.label,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelected) DscBg else DscInk
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

private fun categoryDrawableId(categoryId: String): Int? = when (categoryId.lowercase()) {
    "pizzaria", "pizza" -> R.drawable.ic_preview_pizza
    "acai", "açaí" -> R.drawable.ic_preview_acai
    "marmita" -> R.drawable.ic_preview_marmita
    "bebidas", "bebida" -> R.drawable.ic_preview_drink
    "farmacia", "farmacias", "farmácia" -> R.drawable.ic_preview_pharmacy
    "lanches", "lanche", "hamburguer", "hambúrguer", "burger" -> R.drawable.ic_preview_burger
    else -> null
}

/** Grade de categorias do protótipo: 2 linhas com rolagem horizontal (.cats). */
@Composable
private fun DiscoverCategoryGrid(
    categories: List<SearchCategory>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (categories.isEmpty()) return
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedId == category.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(62.dp)
                    .clickable { onSelect(category.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = if (isSelected) 1.05f else 1f
                            scaleY = if (isSelected) 1.05f else 1f
                        }
                        .clip(CircleShape)
                        .background(DscChipBg)
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) ItaSuperPrimary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = categoryDrawableId(category.id)
                    if (iconRes != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(iconRes),
                            contentDescription = category.name,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = category.name.take(1).uppercase(Locale("pt", "BR")),
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = category.name,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = if (isSelected) ItaSuperPrimary else DscInk
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Cabeçalho de seção do protótipo (.psec-head). */
@Composable
private fun PSecHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.5.sp,
                color = DscInk
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (action != null) {
            Text(
                text = action,
                modifier = Modifier.clickable(
                    enabled = onActionClick != null,
                    onClick = { onActionClick?.invoke() }
                ),
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ItaSuperPrimary
                )
            )
        }
    }
}

/** Carrossel de promos do protótipo (.promo): tag + título + subtítulo, dots abaixo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromoCarousel(
    banners: List<PromoBanner>,
    onBannerClick: (PromoBanner) -> Unit
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            val next = (pagerState.currentPage + 1) % banners.size
            try {
                pagerState.animateScrollToPage(next)
            } catch (_: Exception) {
                break
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            pageSpacing = 10.dp
        ) { page ->
            val banner = banners[page]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(banner.backgroundColor))
                    .clickable { onBannerClick(banner) }
                    .padding(18.dp)
                    .heightIn(min = 118.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = banner.tag.uppercase(Locale("pt", "BR")),
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.08.em,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = banner.title,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = banner.subtitle,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.92f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(
                            width = if (selected) 20.dp else 7.dp,
                            height = 7.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) ItaSuperPrimary else DscLine)
                )
            }
        }
    }
}

/** Card "Peça de novo" do protótipo (.dish-card). */
@Composable
private fun DishCard(item: OrderAgainItem, onReorder: () -> Unit) {
    Column(modifier = Modifier.width(148.dp)) {
        Box(
            modifier = Modifier
                .size(width = 148.dp, height = 104.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DscChipBg),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_ita_bag),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = item.productName,
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                color = DscInk
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.storeName,
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                color = DscInk2
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dscMoney(item.price),
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = DscInk
                ),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ItaSuperPrimary)
                    .clickable(onClick = onReorder)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("reorder_${item.productId}")
            ) {
                Text(
                    text = "Pedir de novo",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/** "Perto de você" do protótipo: lista vertical (.rest-list) com "Ver tudo". */
@Composable
private fun NearYouSection(
    stores: List<Store>,
    couponStoreIds: Set<String>,
    onNavigateToStore: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) stores else stores.take(3)

    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(
            title = "Perto de você",
            action = if (stores.size > 3) "Ver tudo" else null,
            onActionClick = if (stores.size > 3) {
                { expanded = !expanded }
            } else null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            visible.forEach { store ->
                RestCard(
                    store = store,
                    hasCoupon = store.id in couponStoreIds,
                    onClick = { onNavigateToStore(store.id) }
                )
            }
        }
    }
}

/** Card de restaurante vertical do protótipo (.rest-card). */
@Composable
private fun RestCard(
    store: Store,
    hasCoupon: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("discover_rest_${store.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(DscChipBg)
        ) {
            val imageUrl = store.bannerUrl.ifBlank { store.logoUrl }
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_ita_store),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            if (hasCoupon) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ItaSuperPrimary)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Cupom",
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    )
                }
            }
            if (!store.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fechado",
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = store.name,
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = DscInk
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "★ ${dscRating(store.rating)}",
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = DscAmber
                )
            )
            if (store.deliveryTime.isNotBlank()) {
                Text(
                    text = " · ${store.deliveryTime}",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 12.5.sp,
                        color = DscInk2
                    )
                )
            }
            if (store.isFreeDelivery) {
                Text(
                    text = " · ",
                    style = TextStyle(fontFamily = ManropeFontFamily, fontSize = 12.5.sp, color = DscInk2)
                )
                Text(
                    text = "Entrega grátis",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = DscGreen
                    )
                )
            } else if (store.deliveryFee.isNotBlank()) {
                Text(
                    text = " · Entrega ${store.deliveryFee}",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 12.5.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Card de coleção do protótipo (.col-card): foto + gradiente + Salvar. */
@Composable
private fun ProtoCollectionCard(
    collection: DiscoverCollection,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(218.dp)
            .heightIn(min = 138.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DscChipBg)
            .clickable(onClick = onClick)
            .testTag("discover_collection_${collection.id}")
    ) {
        val bgUrl = collection.stores.firstOrNull()
            ?.let { it.bannerUrl.ifBlank { it.logoUrl } }
            .orEmpty()
        if (bgUrl.isNotBlank()) {
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD20F0804),
                            Color(0x400F0804),
                            Color(0x000F0804)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = collection.title,
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = collection.subtitle,
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (collection.saved) ItaSuperPrimary
                        else Color.White.copy(alpha = 0.94f)
                    )
                    .clickable(onClick = onToggleSave)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (collection.saved) "Salva" else "Salvar",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (collection.saved) Color.White else DscInk
                    )
                )
            }
        }
    }
}

/** "Ofertas do dia" do protótipo: título + countdown + grade 2 colunas. */
@Composable
private fun DealsSection(
    deals: List<DailyDeal>,
    storesById: Map<String, Store>,
    onDealClick: (DailyDeal) -> Unit
) {
    val soonestExpiry = remember(deals) {
        deals.mapNotNull { it.endsAtMillis.takeIf { millis -> millis > 0 } }.minOrNull()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(
            title = "Ofertas do dia",
            trailing = if (soonestExpiry != null) {
                { ProtoCountdownPill(endsAtMillis = soonestExpiry) }
            } else null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            deals.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { deal ->
                        Box(modifier = Modifier.weight(1f)) {
                            val store = deal.storeId?.let { storesById[it] }
                            val imageUrl = store?.bannerUrl.ifNullOrBlank { store?.logoUrl.orEmpty() }
                            ProtoDealCard(
                                deal = deal,
                                imageUrl = imageUrl,
                                storeName = store?.name.orEmpty(),
                                onClick = { onDealClick(deal) }
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun String?.ifNullOrBlank(default: () -> String): String =
    if (this.isNullOrBlank()) default() else this

/** Card de oferta do protótipo (.deal). */
@Composable
private fun ProtoDealCard(
    deal: DailyDeal,
    imageUrl: String,
    storeName: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DscChipBg)
            .clickable(onClick = onClick)
            .testTag("daily_deal_${deal.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (deal.tall) 138.dp else 92.dp)
                .background(DscLine),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = deal.discountBadge,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Text(
                text = deal.discountBadge,
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = ItaSuperPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = deal.title,
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = DscInk
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val sub = storeName.ifBlank { deal.description }.ifBlank { null }
            if (sub != null) {
                Text(
                    text = sub,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 11.5.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Contagem regressiva do protótipo (.countdown): pílula escura "Termina em HH:MM:SS". */
@Composable
private fun ProtoCountdownPill(endsAtMillis: Long) {
    var remaining by remember(endsAtMillis) { mutableStateOf(endsAtMillis - System.currentTimeMillis()) }
    LaunchedEffect(endsAtMillis) {
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining = endsAtMillis - System.currentTimeMillis()
        }
    }
    if (remaining <= 0) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DscInk)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Termina em ${formatCountdown(remaining)}",
            style = TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                color = DscBg
            ),
            maxLines = 1
        )
    }
}

/** "Mercados e mais" do protótipo: sub-chips + linhas compactas (.store-row). */
@Composable
private fun UtilitySection(
    stores: List<Store>,
    onNavigateToStore: (String) -> Unit
) {
    val subCats = listOf("Mercado", "Farmácia", "Pet", "Bebidas")
    var selected by remember { mutableStateOf(subCats.first()) }
    val filtered = remember(stores, selected) {
        val norm = selected.lowercase(Locale("pt", "BR"))
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ã", "a").replace("õ", "o")
        val matched = stores.filter { store ->
            val cats = (listOf(store.category) + store.secondaryCategories)
                .joinToString(" ").lowercase(Locale("pt", "BR"))
            cats.contains(norm) || (norm == "bebidas" && cats.contains("bebida"))
        }
        matched.ifEmpty { stores }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(title = "Mercados e mais")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subCats) { cat ->
                val isSelected = selected == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) DscInk else DscLine,
                            shape = RoundedCornerShape(50)
                        )
                        .background(if (isSelected) DscInk else Color.Transparent)
                        .clickable { selected = cat }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isSelected) DscBg else DscInk
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            filtered.forEach { store ->
                ProtoUtilityStoreRow(
                    store = store,
                    onClick = { onNavigateToStore(store.id) }
                )
            }
        }
    }
}

/** Linha compacta de loja do protótipo (.store-row). */
@Composable
private fun ProtoUtilityStoreRow(store: Store, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("utility_store_${store.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoUrl = store.logoUrl.ifBlank { store.bannerUrl }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(DscChipBg),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = store.name.take(1).uppercase(Locale("pt", "BR")),
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DscGreen)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.name,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = DscInk
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("★ ${dscRating(store.rating)}")
                        if (store.deliveryTime.isNotBlank()) append(" · ${store.deliveryTime}")
                    },
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 12.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            store.distanceKm?.let { km ->
                Text(
                    text = String.format(Locale("pt", "BR"), "%.1f km", km).replace('.', ','),
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DscInk2
                    )
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(1.dp)
                .background(DscLine)
        )
    }
}

/** Shimmer animado reutilizável. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "discover_shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(DscSkel, DscSkel.copy(alpha = 0.55f), DscSkel),
        start = Offset(offset * 400f - 200f, 0f),
        end = Offset(offset * 400f + 200f, 0f)
    )
    this.background(brush)
}

/** Skeleton do protótipo (.skel-wrap): círculos, banner, linhas e fotos no formato do conteúdo. */
@Composable
private fun DiscoverShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .shimmer()
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(18.dp))
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(13.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(13.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(13.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmer()
        )
    }
}

/** Tela de resultados da busca, nas cores do protótipo. */
@Composable
private fun DiscoverResults(
    query: String,
    selectedCategory: SearchCategory?,
    selectedQuickFilter: DiscoverQuickFilter?,
    products: List<DiscoverProduct>,
    stores: List<Store>,
    recentSearches: List<String>,
    onClearAll: () -> Unit,
    onRecentSearch: (String) -> Unit,
    onNavigateToStore: (String) -> Unit
) {
    if (query.length < 2 && selectedCategory == null && selectedQuickFilter == null) {
        // Busca vazia: mostra buscas recentes
        if (recentSearches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Busque por restaurantes e pratos",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 14.sp,
                        color = DscInk2
                    )
                )
            }
            return
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            PSecHeader(title = "Buscas recentes")
            recentSearches.take(6).forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecentSearch(term) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = DscInk2,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = term,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontSize = 14.sp,
                            color = DscInk
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    if (products.isEmpty() && stores.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nenhuma loja bate com esse filtro. Toca de novo no filtro pra limpar.",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontSize = 13.5.sp,
                        color = DscInk2
                    )
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Voltar para Descobrir",
                    modifier = Modifier
                        .clickable(onClick = onClearAll)
                        .testTag("clear_discovery_button"),
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Voltar para Descobrir",
                modifier = Modifier
                    .clickable(onClick = onClearAll)
                    .testTag("clear_discovery_button")
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ItaSuperPrimary
                )
            )
        }
        if (stores.isNotEmpty()) {
            item {
                PSecHeader(title = "Lojas")
            }
            items(stores, key = { it.id }) { store ->
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    RestCard(
                        store = store,
                        hasCoupon = false,
                        onClick = { onNavigateToStore(store.id) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        if (products.isNotEmpty()) {
            item {
                PSecHeader(title = "Pratos")
            }
            items(products, key = { it.id }) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStore(product.storeId) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DscChipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DscInk
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = product.storeName,
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontSize = 13.sp,
                                color = DscInk2
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = dscMoney(product.price),
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(1.dp)
                        .background(DscLine)
                )
            }
        }
    }
}
