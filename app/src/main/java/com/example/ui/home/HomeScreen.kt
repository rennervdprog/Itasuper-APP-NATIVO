package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Banner
import com.example.data.model.CategoryItem
import com.example.data.model.Order
import com.example.data.model.Store
import com.example.data.repository.UserSessionRepository
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperHighlightText
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSecondary
import com.example.ui.theme.ItaSuperSuccess
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary
import com.example.ui.theme.ItaSuperWarning

import com.example.data.model.DiscoverProduct
import com.example.ui.permissions.LocationAndPermissionsDialog
import com.example.ui.permissions.LocationOnboardingPreferences
import com.example.ui.permissions.LocationPermissionOnboarding
import com.example.ui.permissions.NotificationOnboardingPreferences
import com.example.ui.permissions.NotificationPermissionOnboarding
import com.example.ui.permissions.PermissionUtils
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.SoraFontFamily

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStore: (String) -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val userSession by UserSessionRepository.userSession.collectAsState()
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showLocationOnboarding by rememberSaveable(userSession.userId) {
        mutableStateOf(
            userSession.isLoggedIn &&
                LocationOnboardingPreferences.shouldShow(context, userSession.userId)
        )
    }
    var showNotificationOnboarding by rememberSaveable(userSession.userId) {
        mutableStateOf(
            userSession.isLoggedIn &&
                NotificationOnboardingPreferences.shouldShow(context, userSession.userId)
        )
    }

    if (showLocationOnboarding) {
        LocationPermissionOnboarding(
            userId = userSession.userId,
            onFinished = { locationGranted ->
                showLocationOnboarding = false
                if (locationGranted) viewModel.fetchGpsLocation(context)
            }
        )
        return
    }

    if (showNotificationOnboarding) {
        NotificationPermissionOnboarding(
            userId = userSession.userId,
            onFinished = { showNotificationOnboarding = false }
        )
        return
    }

    LocationAndPermissionsDialog(
        showDialog = showPermissionsDialog,
        onDismiss = { showPermissionsDialog = false },
        onLocationPermissionResult = { locationGranted ->
            if (locationGranted) viewModel.fetchGpsLocation(context)
        }
    )

    LocationOrAddressDialog(
        visible = uiState.showAddressChoiceDialog,
        onDismiss = viewModel::closeLocationOrAddressDialog,
        onAllowLocation = {
            viewModel.closeLocationOrAddressDialog()
            showPermissionsDialog = true
        },
        onRegisterAddress = viewModel::openAddressForm
    )

    AddressRegistrationSheet(
        visible = uiState.showAddressForm,
        draft = uiState.addressDraft,
        isLookingUpCep = uiState.isLookingUpCep,
        isSaving = uiState.isSavingAddress,
        errorMessage = uiState.addressFormError,
        onDismiss = viewModel::closeAddressForm,
        onDraftChange = viewModel::updateAddressDraft,
        onLookupCep = viewModel::lookupAddressByCep,
        onSave = viewModel::saveAddress
    )

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "home",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeaderSection(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToOrders = onNavigateToOrders,
                onNavigateToNotifications = { onNavigateToRoute("notificacoes") },
                onNavigateToCart = { onNavigateToRoute("carrinho") },
                onRequestPermissions = viewModel::openLocationOrAddressDialog
            )

            HomeSearchSection(
                query = uiState.searchQuery,
                activeCity = uiState.activeCity,
                onQueryChange = viewModel::onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(22.dp))

            HomeExplorationSection(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::onCategorySelect
            )

            // Atalhos exclusivamente para lojas em que o cliente já comprou.
            if (uiState.recentStores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                HomeFavoriteStoresSection(
                    favoriteStores = uiState.recentStores,
                    onStoreClick = onNavigateToStore
                )
            }

            if (uiState.affordableProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HomeAffordableProductsSection(
                    products = uiState.affordableProducts,
                    onProductClick = { product -> onNavigateToStore(product.storeId) },
                    onViewMore = { onNavigateToRoute("busca") }
                )
            }

            if (uiState.repeatProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HomeRepeatProductsSection(
                    products = uiState.repeatProducts,
                    onProductClick = { product -> onNavigateToStore(product.storeId) },
                    onViewMore = onNavigateToOrders
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            HomeStoreListSection(
                stores = uiState.stores,
                regionalStoreCount = uiState.regionalStoreCount,
                hasActiveFilters = uiState.selectedCategory != "todas" ||
                    uiState.searchQuery.isNotBlank() ||
                    uiState.isFreeFeeFilterActive ||
                    uiState.isDirectDeliveryFilterActive,
                isLoading = uiState.isLoadingStores,
                errorMessage = uiState.errorMessage,
                requiresAddress = uiState.requiresAddress,
                activeCity = uiState.activeCity,
                selectedCategory = uiState.selectedCategory,
                isFreeFeeActive = uiState.isFreeFeeFilterActive,
                isDirectDeliveryActive = uiState.isDirectDeliveryFilterActive,
                storeSort = uiState.storeSort,
                onCategorySelect = viewModel::onCategorySelect,
                onToggleFreeFee = viewModel::toggleFreeFeeFilter,
                onToggleDirectDelivery = viewModel::toggleDirectDeliveryFilter,
                onStoreSortSelect = viewModel::onStoreSortSelect,
                onRetry = viewModel::loadStores,
                onInformAddress = viewModel::openLocationOrAddressDialog,
                onStoreClick = onNavigateToStore
            )

            // A primeira dobra termina na lista, como no layout aprovado. O catálogo completo
            // continua acessível pela Busca, sem introduzir uma vitrine adicional nesta Home.
            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Support Bottom Sheet Modal
    if (uiState.showSupportSheet) {
        SupportBottomSheet(
            onDismiss = viewModel::closeSupportSheet
        )
    }
}

@Composable
private fun HomeHeaderSection(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCart: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ENTREGAR EM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ItaSuperTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                val activeCity = uiState.activeCity.trim()
                if (activeCity.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onRequestPermissions() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_map_pin),
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Informe sua cidade",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.openLocationOrAddressDialog() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_map_pin),
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeCity,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ItaSuperTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_down),
                            contentDescription = "Alterar cidade",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeTopActionButton(
                    iconRes = R.drawable.ic_lucide_bell,
                    contentDescription = "Abrir avisos",
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.testTag("header_notifications_button")
                )
                HomeTopActionButton(
                    iconRes = R.drawable.ic_lucide_shopping_bag,
                    contentDescription = "Abrir sacola",
                    onClick = onNavigateToCart,
                    modifier = Modifier.testTag("header_cart_button")
                )
            }
        }

        // Animated Inline Street Number Editor
        AnimatedVisibility(
            visible = uiState.isEditingNumber,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.streetNumber,
                    onValueChange = viewModel::onStreetNumberChange,
                    label = { Text("Número do Endereço") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveStreetNumber() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inline_number_input"),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = viewModel::saveStreetNumber,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("save_number_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Salvar")
                }
            }
        }
    }
}

@Composable
private fun HomeTopActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE8E8E8), CircleShape)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HomeSearchSection(
    query: String,
    activeCity: String,
    onQueryChange: (String) -> Unit
) {
    val placeholder = activeCity.takeIf { it.isNotBlank() }
        ?.let { "Buscar em $it" }
        ?: "Buscar lojas e categorias"

                Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .testTag("home_search_input"),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5E5))
            ) {

        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_search),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = ItaSuperTextPrimary
                ),
                singleLine = true,
                cursorBrush = SolidColor(ItaSuperPrimary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = ManropeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = Color(0xFF5F5F5F)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeExplorationSection(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val visibleCategories = buildList {
        // Item local de limpeza de filtro: não altera nem depende do catálogo do Supabase.
        add(CategoryItem(id = "todas", name = "Todos", iconName = "layout_grid"))
        addAll(categories.filterNot { it.id.equals("todas", ignoreCase = true) }.take(5))
    }
    if (visibleCategories.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
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

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(visibleCategories, key = { index, category -> "home_explore_${index}_${category.id}" }) { _, category ->
                HomeExplorationCategoryItem(
                    category = category,
                    selected = category.id.equals(selectedCategory, ignoreCase = true),
                    onClick = { onCategorySelect(category.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeExplorationCategoryItem(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconRes = when (category.iconName.lowercase()) {
        "layout_grid", "todas", "todos", "all" -> R.drawable.ic_lucide_layout_grid
        "fastfood", "lanche", "hamburger", "hamburguer" -> R.drawable.ic_lucide_hamburger
        "local_pizza", "pizza" -> R.drawable.ic_lucide_pizza
        "restaurant" -> R.drawable.ic_lucide_package_open
        "bakery_dining", "pastel" -> R.drawable.ic_lucide_sandwich
        "icecream", "acai", "açaí" -> R.drawable.ic_lucide_soup
        "local_bar", "bebidas" -> R.drawable.ic_lucide_martini
        "shopping_cart", "mercado", "market" -> R.drawable.ic_lucide_shopping_basket
        else -> R.drawable.ic_lucide_hamburger
    }
    val label = category.name.ifBlank { category.id.replaceFirstChar { it.uppercase() } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick)
            .testTag("explore_category_${category.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) Color(0xFFFFF8F3) else Color.White,
            border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ItaSuperPrimary else Color(0xFFE8E8E8)
            ),
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = ManropeFontFamily,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) ItaSuperPrimary else Color(0xFF262626),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeQuickFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFFF8F3) else Color.White,
        border = BorderStroke(1.dp, if (selected) ItaSuperPrimary else Color(0xFFE3E3E3))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) ItaSuperPrimary else Color(0xFF262626),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Filtro selecionado",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeCompactBannerSection(banners: List<Banner>, onStoreClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(banners, key = { index, banner -> "home_banner_${index}_${banner.id}" }) { _, banner ->
            Surface(
                modifier = Modifier
                    .width(284.dp)
                    .height(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !banner.targetStoreId.isNullOrBlank()) {
                        banner.targetStoreId?.let(onStoreClick)
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title.ifBlank { "Campanha ItaSuper" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HomeHeroBentoSection(
    banners: List<Banner>,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit,
    onStoreClick: (String) -> Unit
) {
    val activeBanner = banners.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Grande (Left)
        Card(
            modifier = Modifier
                .weight(1.8f)
                .height(160.dp)
                .clickable {
                    activeBanner?.targetStoreId?.let { storeId ->
                        onStoreClick(storeId)
                    }
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeBanner != null && activeBanner.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = activeBanner.imageUrl,
                        contentDescription = activeBanner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = activeBanner.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeBanner.description.isNotBlank()) {
                            Text(
                                text = activeBanner.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(ItaSuperPrimary, ItaSuperPrimary.copy(alpha = 0.85f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "OFERTAS ITASUPER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Os melhores estabelecimentos de Itaboraí",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ver ofertas",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = ItaSuperPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mini Cards Fixos (Right Column)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mini Card 1: Sem taxa
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleFreeFee() }
                    .testTag("filter_card_sem_taxa"),
                shape = RoundedCornerShape(16.dp),
                color = if (isFreeFeeActive) ItaSuperPrimary.copy(alpha = 0.15f) else ItaSuperHighlightBg,
                border = BorderStroke(
                    width = if (isFreeFeeActive) 2.dp else 1.dp,
                    color = if (isFreeFeeActive) ItaSuperPrimary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isFreeFeeActive) ItaSuperPrimary else ItaSuperHighlightText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sem taxa",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isFreeFeeActive) ItaSuperPrimary else ItaSuperTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "de entrega",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Mini Card 2: Entrega direta
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleDirectDelivery() }
                    .testTag("filter_card_entrega_direta"),
                shape = RoundedCornerShape(16.dp),
                color = if (isDirectDeliveryActive) ItaSuperPrimary.copy(alpha = 0.15f) else ItaSuperSecondary,
                border = BorderStroke(
                    width = if (isDirectDeliveryActive) 2.dp else 1.dp,
                    color = if (isDirectDeliveryActive) ItaSuperPrimary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entrega",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDirectDeliveryActive) ItaSuperPrimary else ItaSuperTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "direta da loja",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHighlightsBentoSection(
    stores: List<Store>,
    onStoreClick: (String) -> Unit
) {
    val topStores = stores.sortedByDescending { it.rating }.take(3)
    if (topStores.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Destaques",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SoraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
        }

        val mainStore = topStores[0]
        val sideStore1 = topStores.getOrNull(1)
        val sideStore2 = topStores.getOrNull(2)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Store Card (2 cols x 2 rows effect)
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clickable { onStoreClick(mainStore.id) }
                    .testTag("highlight_main_store_${mainStore.id}"),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = mainStore.bannerUrl.ifBlank { mainStore.logoUrl },
                        contentDescription = mainStore.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    // Badge "Destaque" top left
                    Surface(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopStart),
                        color = ItaSuperPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Destaque",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = mainStore.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ItaSuperWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${mainStore.rating}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperWarning,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${mainStore.category}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Side Column (2 stacked smaller stores)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOfNotNull(sideStore1, sideStore2).forEach { store ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onStoreClick(store.id) }
                            .testTag("highlight_side_store_${store.id}"),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = store.logoUrl.ifBlank { store.bannerUrl },
                                contentDescription = store.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ItaSuperWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${store.rating}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperWarning,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDiscoverProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (String) -> Unit
) {
    val productsWithImages = products.filter { product ->
        product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)
    }
    if (productsWithImages.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Descubra",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
            Text(
                text = "SELECIONADO PRA VOCÊ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            productsWithImages.chunked(2).forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowProducts.forEach { product ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onProductClick(product.storeId) }
                                .testTag("discover_product_${product.id}"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(ItaSuperHighlightBg)
                                ) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Badge "Aberta" com bolinha verde (produtos já vêm só de lojas abertas)
                                    Surface(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.TopStart),
                                        color = Color.White.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Aberta",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = ItaSuperTextPrimary,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    // Badge de categoria da loja
                                    if (product.storeCategory.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopEnd),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = product.storeCategory.replace("_", " ").uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 9.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = SoraFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        minLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = product.storeName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ItaSuperTextSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = ItaSuperPrimary,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = String.format("R$ %.2f", product.price).replace('.', ','),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = SoraFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryChipsSection(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(categories, key = { idx, cat -> "cat_${idx}_${cat.id}" }) { _, cat ->
            val isSelected = cat.id == selectedCategory

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCategorySelect(cat.id) }
                    .testTag("chip_category_${cat.id}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) ItaSuperPrimary else ItaSuperSecondary,
                border = if (!isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (cat.iconName) {
                        "fastfood" -> Icons.Default.Fastfood
                        "local_pizza" -> Icons.Default.LocalPizza
                        "shopping_cart" -> Icons.Default.ShoppingCart
                        "medical_services" -> Icons.Default.MedicalServices
                        "local_bar" -> Icons.Default.LocalBar
                        else -> Icons.Default.Apps
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else ItaSuperPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else ItaSuperTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLastOrderSection(
    order: Order,
    isReordering: Boolean,
    onViewStore: () -> Unit,
    onReorder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = ItaSuperTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ÚLTIMO PEDIDO",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextSecondary,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Store Avatar Logo
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ItaSuperHighlightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = order.createdAt.take(10).ifBlank { "Pedido anterior" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = "R$ %.2f".format(order.total).replace('.', ','),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperHighlightText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewStore,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("view_last_store_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver loja",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Button(
                        onClick = onReorder,
                        enabled = !isReordering,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("reorder_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReordering) "Adicionando..." else "Pedir de novo",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreLogoThumbnail(
    store: Store,
    modifier: Modifier,
    circular: Boolean
) {
    val imageUrl = store.logoUrl.ifBlank { store.bannerUrl }.trim()
    var imageFailed by remember(store.id, imageUrl) {
        mutableStateOf(imageUrl.isBlank() || imageUrl.equals("null", ignoreCase = true))
    }
    val shape = if (circular) CircleShape else RoundedCornerShape(16.dp)
    val initial = store.name.trim().take(1).uppercase().ifBlank { "I" }
    val useDarkPlaceholder = initial == "C"
    val placeholderBackground = if (useDarkPlaceholder) Color(0xFF741914) else Color(0xFFFFE8A8)
    val placeholderForeground = if (useDarkPlaceholder) Color.White else ItaSuperPrimary

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (imageFailed) placeholderBackground else ItaSuperHighlightBg),
        contentAlignment = Alignment.Center
    ) {
        if (!imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = store.name,
                contentScale = ContentScale.Crop,
                onError = { imageFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (imageFailed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = initial,
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (circular) 18.sp else 24.sp,
                    lineHeight = if (circular) 18.sp else 23.sp,
                    color = placeholderForeground
                )
                Icon(
                    painter = painterResource(R.drawable.ic_home_placeholder_smile),
                    contentDescription = null,
                    tint = placeholderForeground,
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .size(if (circular) 17.dp else 22.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeFavoriteStoresSection(
    favoriteStores: List<Store>,
    onStoreClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(25.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ita_store),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Suas lojas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF1F1F1F)
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = "compradas recentemente",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF6D6D6D)
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.height(42.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(favoriteStores.take(2), key = { idx, store -> "fav_${idx}_${store.id}" }) { _, store ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(155.dp)
                        .clickable { onStoreClick(store.id) }
                        .testTag("fav_store_${store.id}")
                ) {
                    StoreLogoThumbnail(
                        store = store,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFE8E8E8), CircleShape),
                        circular = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF242424)
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
private fun HomeAffordableProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (DiscoverProduct) -> Unit,
    onViewMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preço acessível",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1F1F1F)
                )
            )
            Text(
                text = "Ver mais",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewMore)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("affordable_products_view_more")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(products.take(8), key = { _, product -> "affordable_product_${product.id}" }) { _, product ->
                Card(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onProductClick(product) }
                        .testTag("affordable_product_${product.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFF7F7F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_ita_bag),
                                    contentDescription = null,
                                    tint = Color(0xFF8A8A8A),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF242424)
                            ),
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFF737373)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format("R$ %.2f", product.price).replace('.', ','),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRepeatProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (DiscoverProduct) -> Unit,
    onViewMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Peça de novo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F1F1F)
                    )
                )
                Text(
                    text = "produtos que você já pediu",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Color(0xFF6D6D6D)
                    )
                )
            }
            Text(
                text = "Ver mais",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewMore)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("repeat_products_view_more")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(products.take(8), key = { _, product -> "repeat_product_${product.id}" }) { _, product ->
                Card(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onProductClick(product) }
                        .testTag("repeat_product_${product.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFF7F7F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_ita_bag),
                                    contentDescription = null,
                                    tint = Color(0xFF8A8A8A),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF242424)
                            ),
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFF737373)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format("R$ %.2f", product.price).replace('.', ','),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStoreListSection(
    stores: List<Store>,
    regionalStoreCount: Int,
    hasActiveFilters: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    requiresAddress: Boolean,
    activeCity: String,
    selectedCategory: String,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    storeSort: HomeStoreSort,
    onCategorySelect: (String) -> Unit,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit,
    onStoreSortSelect: (HomeStoreSort) -> Unit,
    onRetry: () -> Unit,
    onInformAddress: () -> Unit,
    onStoreClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Text(
                text = if (activeCity.isNotBlank()) "Lojas em $activeCity" else "Lojas",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = ItaSuperTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (activeCity.isNotBlank()) {
                Text(
                    text = "$regionalStoreCount disponíveis",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily,
                        color = ItaSuperTextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            HomeStoreSortControl(
                selectedSort = storeSort,
                onSelect = onStoreSortSelect
            )
        }

        HomeStoreFilterBar(
            isAllSelected = selectedCategory.equals("todas", ignoreCase = true),
            isFreeFeeActive = isFreeFeeActive,
            isDirectDeliveryActive = isDirectDeliveryActive,
            onSelectAll = { onCategorySelect("todas") },
            onToggleFreeFee = onToggleFreeFee,
            onToggleDirectDelivery = onToggleDirectDelivery
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ItaSuperPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Carregando lojas...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else if (requiresAddress) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSuperHighlightBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Informe sua localização ou endereço",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Precisamos da sua cidade para mostrar lojas que atendem sua região.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onInformAddress,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Informar localização/endereço", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (errorMessage != null && stores.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tentar novamente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (stores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        regionalStoreCount > 0 && hasActiveFilters -> "Nenhuma loja encontrada com os filtros selecionados"
                        activeCity.isNotBlank() -> "Nenhuma loja disponível em $activeCity"
                        else -> "Nenhuma loja encontrada para esse filtro"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                stores.forEachIndexed { index, store ->
                    StoreCardItem(
                        store = store,
                        showDivider = index < stores.lastIndex,
                        onClick = { onStoreClick(store.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStoreFilterBar(
    isAllSelected: Boolean,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    onSelectAll: () -> Unit,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "filter_all") {
            HomeQuickFilterPill(label = "Todos", selected = isAllSelected, onClick = onSelectAll)
        }
        item(key = "filter_free_fee") {
            HomeQuickFilterPill(label = "Taxa grátis", selected = isFreeFeeActive, onClick = onToggleFreeFee)
        }
        item(key = "filter_direct_delivery") {
            HomeQuickFilterPill(label = "Entrega direta", selected = isDirectDeliveryActive, onClick = onToggleDirectDelivery)
        }
    }
}

@Composable
private fun HomeStoreSortControl(
    selectedSort: HomeStoreSort,
    onSelect: (HomeStoreSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            modifier = Modifier
                .width(100.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .testTag("home_sort_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_sort),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Ordenar",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HomeStoreSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.label,
                            fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (sort == selectedSort) ItaSuperPrimary else ItaSuperTextPrimary
                        )
                    },
                    trailingIcon = if (sort == selectedSort) {
                        { Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = ItaSuperPrimary) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    }
                )
            }
        }
    }
}

private fun nextStoreOpeningLabel(store: Store): String {
    if (store.isOpen || store.openingHours.isEmpty()) return ""
    val now = java.util.Calendar.getInstance()
    val today = now.get(java.util.Calendar.DAY_OF_WEEK)
    val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

    fun startMinutes(value: String): Int? {
        val parts = value.trim().split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    for (offset in 0..7) {
        val day = ((today - 1 + offset) % 7) + 1
        val nextHour = store.openingHours
            .filter { it.dayOfWeek == day && !it.isClosedAllDay }
            .sortedBy { startMinutes(it.openTime) ?: Int.MAX_VALUE }
            .firstOrNull { offset > 0 || (startMinutes(it.openTime) ?: Int.MAX_VALUE) > currentMinutes }
            ?: continue
        val prefix = when (offset) {
            0 -> "Abre hoje às"
            1 -> "Abre amanhã às"
            else -> "Abre"
        }
        return "$prefix ${nextHour.openTime}"
    }
    return ""
}

@Composable
private fun StoreCardItem(
    store: Store,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val categoryLabel = store.category
        .replace("_", " ")
        .replaceFirstChar { character -> character.uppercase() }
        .ifBlank { "Loja" }
    val deliveryTime = store.deliveryTime.takeUnless { it.isBlank() || it.equals("null", true) }
    val rawDeliveryFee = store.deliveryFee.takeUnless { it.isBlank() || it.equals("null", true) }
    val deliveryFee = rawDeliveryFee?.let { fee ->
        when {
            fee.equals("Grátis", true) || fee.equals("Retirada", true) || fee.startsWith("A partir", true) -> fee
            else -> "Taxa $fee"
        }
    }
    val deliveryDetails = listOfNotNull(deliveryTime, deliveryFee).joinToString("  •  ")
    val openingMessage = nextStoreOpeningLabel(store)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("store_card_${store.id}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreLogoThumbnail(
                store = store,
                modifier = Modifier.size(56.dp),
                circular = false
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF1F1F1F)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StoreAvailabilityBadge(isOpen = store.isOpen)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF686868)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (deliveryDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = deliveryDetails,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ManropeFontFamily,
                            fontSize = 13.sp,
                            color = if (store.isFreeDelivery) ItaSuperSuccess else Color(0xFF686868),
                            fontWeight = if (store.isFreeDelivery) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!store.isOpen && openingMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = openingMessage,
                        style = MaterialTheme.typography.labelMedium.copy(color = ItaSuperTextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_ita_chevron_right),
                contentDescription = "Abrir loja",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun StoreAvailabilityBadge(isOpen: Boolean) {
    val background = if (isOpen) Color(0xFFF0FAF3) else Color(0xFFFFF0F2)
    val foreground = if (isOpen) ItaSuperSuccess else Color(0xFFC13550)
    val label = if (isOpen) "Aberta" else "Fechada"

    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(foreground)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    fontSize = 11.sp
                )
            )
        }
    }
}
