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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Banner
import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.repository.UserSessionRepository
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperHighlightText
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Location & Top Actions Bar
            HomeHeaderSection(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToOrders = onNavigateToOrders,
                onRequestPermissions = viewModel::openLocationOrAddressDialog
            )

            // 1. Busca
            HomeSearchSection(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onFilterClick = viewModel::onFiltersClick
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Atalhos de lojas realmente compradas pelo cliente.
            if (uiState.recentStores.isNotEmpty()) {
                HomeFavoriteStoresSection(
                    favoriteStores = uiState.recentStores,
                    onStoreClick = onNavigateToStore
                )
                Spacer(modifier = Modifier.height(26.dp))
            }

            // Lista regional principal.
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
                onRetry = viewModel::loadStores,
                onInformAddress = viewModel::openLocationOrAddressDialog,
                onStoreClick = onNavigateToStore
            )

            // Descubra permanece na Home, mas somente com mídia real.
            if (uiState.discoverProducts.any { product ->
                    product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)
                }
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                HomeDiscoverProductsSection(
                    products = uiState.discoverProducts,
                    onProductClick = { storeId -> onNavigateToStore(storeId) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Informe sua cidade",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
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
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeCity,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Alterar cidade",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Support Icon
                IconButton(
                    onClick = viewModel::openSupportSheet,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Suporte",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Orders Icon
                IconButton(
                    onClick = onNavigateToOrders,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .testTag("header_orders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notificações / Pedidos",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
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
private fun HomeSearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar açaí...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = ItaSuperPrimary)
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("home_search_input"),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = ItaSuperSecondary,
                unfocusedContainerColor = ItaSuperSecondary
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Filter Primary Button
        Surface(
            modifier = Modifier
                .size(54.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onFilterClick() }
                .testTag("filter_button"),
            color = ItaSuperPrimary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtros",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
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
    lastOrder: LastOrder,
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
                            text = lastOrder.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = lastOrder.dateText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = "R$ %.2f".format(lastOrder.totalPrice).replace('.', ','),
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
                            text = "Pedir de novo",
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
    val shape = if (circular) CircleShape else RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(ItaSuperHighlightBg),
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
            Text(
                text = store.name.trim().take(1).uppercase().ifBlank { "I" },
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = ItaSuperPrimary
            )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Store,
                contentDescription = null,
                tint = ItaSuperTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SUAS LOJAS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextSecondary,
                    letterSpacing = 0.5.sp
                )
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(favoriteStores, key = { idx, store -> "fav_${idx}_${store.id}" }) { _, store ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(82.dp)
                        .clickable { onStoreClick(store.id) }
                        .testTag("fav_store_${store.id}")
                ) {
                    StoreLogoThumbnail(
                        store = store,
                        modifier = Modifier
                            .size(68.dp)
                            .border(2.dp, ItaSuperPrimary.copy(alpha = 0.22f), CircleShape),
                        circular = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
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
private fun HomeStoreListSection(
    stores: List<Store>,
    regionalStoreCount: Int,
    hasActiveFilters: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    requiresAddress: Boolean,
    activeCity: String,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeCity.isNotBlank()) "Todas as lojas em $activeCity" else "Todas as lojas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (activeCity.isNotBlank()) {
                Text(
                    text = "$regionalStoreCount ${if (regionalStoreCount == 1) "loja" else "lojas"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

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
    val categoryLabel = store.category.replace("_", " ").ifBlank { "Loja" }
    val ratingVisible = store.rating > 0.0
    val secondaryInfo = buildList {
        add(categoryLabel)
        store.distanceKm?.takeIf { it >= 0.0 }?.let { distance ->
            add(String.format("%.1f km", distance).replace('.', ','))
        }
        if (!ratingVisible) add("Novo")
    }.joinToString(" • ")
    val deliveryTime = store.deliveryTime.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"
    val rawDeliveryFee = store.deliveryFee.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"
    val deliveryFee = when {
        rawDeliveryFee == "—" || rawDeliveryFee.equals("Grátis", true) || rawDeliveryFee.equals("Retirada", true) -> rawDeliveryFee
        rawDeliveryFee.startsWith("A partir", true) -> rawDeliveryFee
        else -> "A partir de $rawDeliveryFee"
    }
    val openingMessage = nextStoreOpeningLabel(store)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("store_card_${store.id}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreLogoThumbnail(
                store = store,
                modifier = Modifier.size(68.dp),
                circular = false
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (ratingVisible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Avaliação",
                            tint = ItaSuperWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", store.rating),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperWarning
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = secondaryInfo,
                    style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = deliveryTime, style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperTextSecondary))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(text = "•", style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperTextSecondary.copy(alpha = 0.55f)))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = deliveryFee,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (store.isFreeDelivery) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (store.isFreeDelivery) ItaSuperSuccess else ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!store.isOpen) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFFFE7EB), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "FECHADA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFBA3048),
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        if (openingMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = openingMessage,
                                style = MaterialTheme.typography.labelSmall.copy(color = ItaSuperTextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Abrir loja",
                tint = if (store.isOpen) ItaSuperTextSecondary else ItaSuperTextSecondary.copy(alpha = 0.45f),
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
