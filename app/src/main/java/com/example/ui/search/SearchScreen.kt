package com.example.ui.search

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Store
import com.example.data.repository.SearchCategory
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val trendingStores by viewModel.trendingStores.collectAsStateWithLifecycle()
    val newStores by viewModel.newStores.collectAsStateWithLifecycle()
    val filteredStores by viewModel.filteredStores.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.requestGpsLocation(context)
        }
    }

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "busca",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Header: Title and Search Box
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Busca compacta, no mesmo padrão do cabeçalho da referência Capacitor
                    OutlinedTextField(
                        value = uiState.rawQuery,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = {
                            Text(
                                "Buscar por loja, prato ou mercado...",
                                color = ItaSuperTextSecondary,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ItaSuperPrimary
                            )
                        },
                        trailingIcon = {
                            if (uiState.rawQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = viewModel::clearQuery,
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpar busca",
                                        tint = ItaSuperTextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ItaSuperPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.submitSearch(uiState.rawQuery)
                                focusManager.clearFocus()
                            }
                        )
                    )

                    // Banner/Botão "Ative sua localização" se não tiver coordenadas GPS
                    if (uiState.userLocation == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ItaSuperHighlightBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = ItaSuperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ative sua localização",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary
                                        )
                                    )
                                    Text(
                                        text = "Calcule a distância exata até cada loja",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ItaSuperTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                if (uiState.isFetchingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ItaSuperPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = ItaSuperPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MODO PADRÃO vs MODO RESULTADO
            if (!isSearchActive) {
                // MODO PADRÃO
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // 1. Buscas Recentes (se houver)
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Buscas recentes",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperTextPrimary
                                        )
                                    )
                                    TextButton(onClick = viewModel::clearRecentSearches) {
                                        Text(
                                            "Limpar",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = ItaSuperPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    itemsIndexed(recentSearches, key = { idx, term -> "recent_${idx}_$term" }) { _, term ->
                                        SuggestionChip(
                                            onClick = { viewModel.onRecentSearchSelect(term) },
                                            label = { Text(term) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Grid de Categorias (2 colunas, 8 categorias)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Categorias",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperTextPrimary
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val categories = viewModel.searchCategories
                            for (i in categories.indices step 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CategoryGridCard(
                                        category = categories[i],
                                        isSelected = uiState.selectedCategoryId == categories[i].id,
                                        onClick = { viewModel.onCategorySelect(categories[i].id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (i + 1 < categories.size) {
                                        CategoryGridCard(
                                            category = categories[i + 1],
                                            isSelected = uiState.selectedCategoryId == categories[i + 1].id,
                                            onClick = { viewModel.onCategorySelect(categories[i + 1].id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 3. Seção "Em Alta" (lojas abertas com foto, máx 8)
                    if (trendingStores.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Em Alta",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextPrimary
                                    ),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    itemsIndexed(trendingStores, key = { idx, store -> "trending_${idx}_${store.id}" }) { _, store ->
                                        TrendingStoreHorizontalCard(
                                            store = store,
                                            userLocation = uiState.userLocation,
                                            onClick = { onNavigateToStore(store.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Seção "Novidades" (lojas com created_at nos últimos 30 dias, máx 8)
                    if (newStores.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Novidades",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextPrimary
                                    ),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    itemsIndexed(newStores, key = { idx, store -> "new_${idx}_${store.id}" }) { _, store ->
                                        NewStoreHorizontalCard(
                                            store = store,
                                            userLocation = uiState.userLocation,
                                            onClick = { onNavigateToStore(store.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MODO RESULTADO (busca com 2+ caracteres OU categoria selecionada)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header de filtros ativos
                    if (uiState.selectedCategoryId != null) {
                        val selectedCat = viewModel.searchCategories.find { it.id == uiState.selectedCategoryId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.clearCategory() },
                                label = { Text("Categoria: ${selectedCat?.name ?: uiState.selectedCategoryId}") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar filtro",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ItaSuperHighlightBg,
                                    selectedLabelColor = ItaSuperPrimary
                                )
                            )
                        }
                    }

                    if (filteredStores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    color = ItaSuperHighlightBg
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Nenhuma loja encontrada",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Tente buscar por outros termos ou selecione outra categoria.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ItaSuperTextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        viewModel.clearQuery()
                                        viewModel.clearCategory()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                                ) {
                                    Text("Ver todas as opções")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(filteredStores, key = { idx, store -> "search_${idx}_${store.id}" }) { _, store ->
                                StoreSearchResultCard(
                                    store = store,
                                    userLocation = uiState.userLocation,
                                    onClick = { onNavigateToStore(store.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ⚠️ IMPORTANTE: NENHUMA ANOTAÇÃO (@Composable, @Stable, etc.) DEVE SER COLOCADA NESTAS PROPRIEDADES.
// Elas são dados estáticos e não precisam de anotação. Se houver alguma, remova.
private val categoryGradients: Map<String, List<Color>> = mapOf(
    "lanches" to listOf(Color(0xFFF97316), Color(0xFFF59E0B)),
    "pizzaria" to listOf(Color(0xFFF43F5E), Color(0xFFDC2626)),
    "marmita" to listOf(Color(0xFFD97706), Color(0xFFEAB308)),
    "acai" to listOf(Color(0xFF7C3AED), Color(0xFFC026D3)),
    "bebidas" to listOf(Color(0xFFEF4444), Color(0xFFF97316)),
    "mercado" to listOf(Color(0xFF059669), Color(0xFF15803D)),
    "pastel" to listOf(Color(0xFFEAB308), Color(0xFFD97706)),
    "churrasco" to listOf(Color(0xFF44403C), Color(0xFF262626))
)

private val categoryEmojis: Map<String, String> = mapOf(
    "lanches" to "\uD83C\uDF54",
    "pizzaria" to "\uD83C\uDF55",
    "marmita" to "\uD83C\uDF71",
    "acai" to "\uD83C\uDF68",
    "bebidas" to "\uD83C\uDF79",
    "mercado" to "\uD83D\uDED2",
    "pastel" to "\uD83E\uDD5F",
    "churrasco" to "\uD83E\uDD69"
)

@Composable
private fun CategoryGridCard(
    category: SearchCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = categoryGradients[category.id] ?: listOf(ItaSuperPrimary, ItaSuperPrimary)
    val emoji = categoryEmojis[category.id] ?: "\uD83C\uDF7D\uFE0F"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = if (isSelected) BorderStroke(2.5.dp, Color.White) else null,
        shadowElevation = 3.dp,
        modifier = modifier.height(96.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)
                )
                .padding(12.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun TrendingStoreHorizontalCard(
    store: Store,
    userLocation: Pair<Double, Double>?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.width(180.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                AsyncImage(
                    model = store.bannerUrl.ifBlank { store.logoUrl },
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", store.rating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = store.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ItaSuperTextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val distStr = getFormattedDistance(store, userLocation)
                Text(
                    text = "$distStr • ${store.deliveryTime}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ItaSuperPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun NewStoreHorizontalCard(
    store: Store,
    userLocation: Pair<Double, Double>?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.width(180.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                AsyncImage(
                    model = store.bannerUrl.ifBlank { store.logoUrl },
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = ItaSuperPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "NOVO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = store.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ItaSuperTextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StoreSearchResultCard(
    store: Store,
    userLocation: Pair<Double, Double>?,
    onClick: () -> Unit
) {
    val isClosed = !store.isOpen

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isClosed) Color(0xFFF0F0F0) else Color.White,
        shadowElevation = if (isClosed) 0.dp else 2.dp,
        border = if (isClosed) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isClosed) 0.65f else 1.0f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AsyncImage(
                    model = store.logoUrl.ifBlank { store.bannerUrl },
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isClosed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FECHADA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isClosed) ItaSuperTextSecondary else ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isClosed) {
                        Surface(
                            color = Color(0xFF757575),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "FECHADA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = store.category,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ItaSuperTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", store.rating),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperTextPrimary
                            )
                        )
                    }

                    Text("•", color = ItaSuperTextSecondary, fontSize = 12.sp)

                    Text(
                        text = store.deliveryTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ItaSuperTextSecondary
                        )
                    )

                    Text("•", color = ItaSuperTextSecondary, fontSize = 12.sp)

                    val distStr = getFormattedDistance(store, userLocation)
                    Text(
                        text = distStr,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ItaSuperPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private fun getFormattedDistance(store: Store, userLocation: Pair<Double, Double>?): String {
    if (userLocation != null && store.latitude != null && store.longitude != null) {
        val distKm = calculateHaversineDistanceKm(
            userLocation.first,
            userLocation.second,
            store.latitude,
            store.longitude
        )
        return if (distKm < 1.0) {
            "${(distKm * 1000).toInt()} m"
        } else {
            String.format(Locale.getDefault(), "%.1f km", distKm).replace(".", ",")
        }
    }
    return String.format(Locale.getDefault(), "%.1f km", store.distanceKm).replace(".", ",")
}

private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "fastfood" -> Icons.Default.Fastfood
        "local_pizza" -> Icons.Default.LocalPizza
        "restaurant" -> Icons.Default.Restaurant
        "icecream" -> Icons.Default.Icecream
        "local_bar" -> Icons.Default.LocalBar
        "shopping_cart" -> Icons.Default.ShoppingCart
        "bakery_dining" -> Icons.Default.BakeryDining
        "kebab_dining" -> Icons.Default.DinnerDining
        else -> Icons.Default.Storefront
    }
}
