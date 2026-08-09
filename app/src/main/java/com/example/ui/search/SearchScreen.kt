package com.example.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.Store
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSecondary
import com.example.ui.theme.ItaSuperSuccess
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary
import com.example.ui.theme.ItaSuperWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToStore: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stores by viewModel.filteredStores.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "busca",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Header & Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Location Indicator Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "BUSCAR EM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextSecondary
                                    )
                                )
                                Text(
                                    text = uiState.address,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextPrimary
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Refresh Location Button
                        IconButton(
                            onClick = viewModel::refreshLocation,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("refresh_location_button")
                        ) {
                            if (uiState.isRefreshingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ItaSuperPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Atualizar localização",
                                    tint = ItaSuperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Outlined Input with Clear Button
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = {
                            Text(
                                "Buscar por loja, prato ou mercado...",
                                color = ItaSuperTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ícone de busca",
                                tint = ItaSuperPrimary
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_page_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ItaSuperPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = ItaSuperSecondary,
                            unfocusedContainerColor = ItaSuperSecondary
                        )
                    )
                }
            }

            // Categories Horizontal Filter Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Categorias",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperTextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) {
                        items(viewModel.categories) { cat ->
                            val isSelected = uiState.selectedCategoryId == cat.id
                            SearchCategoryChip(
                                category = cat,
                                isSelected = isSelected,
                                onClick = { viewModel.onCategorySelect(cat.id) }
                            )
                        }
                    }
                }
            }

            // Recent Searches (shows when query is empty and list exists)
            if (uiState.query.isEmpty() && uiState.recentSearches.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = ItaSuperTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buscas recentes",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperTextPrimary
                                    )
                                )
                            }
                            TextButton(
                                onClick = viewModel::clearRecentSearches,
                                modifier = Modifier.testTag("clear_recent_searches")
                            ) {
                                Text(
                                    text = "Limpar",
                                    style = MaterialTheme.typography.labelMedium.copy(color = ItaSuperPrimary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.recentSearches.forEachIndexed { index, term ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { viewModel.onRecentSearchSelect(term) }
                                        .testTag("recent_search_chip_$index"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = ItaSuperSecondary,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = term,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = ItaSuperTextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.query.isBlank()) "Lojas recomendadas" else "Resultados de busca",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperTextPrimary
                        )
                    )
                    Text(
                        text = "${stores.size} ${if (stores.size == 1) "loja" else "lojas"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Empty State if no stores found
            if (stores.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
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
                            text = if (uiState.query.isNotBlank()) {
                                "Não encontramos resultados para \"${uiState.query}\". Tente buscar por outros termos ou selecione outra categoria."
                            } else {
                                "Nenhuma loja disponível para o filtro selecionado."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ItaSuperTextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.clearQuery()
                                viewModel.onCategorySelect("todas")
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                            modifier = Modifier.testTag("reset_search_button")
                        ) {
                            Text("Ver todas as lojas")
                        }
                    }
                }
            } else {
                // Store Cards List
                items(stores, key = { it.id }) { store ->
                    SearchStoreCard(
                        store = store,
                        onClick = { onNavigateToStore(store.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchCategoryChip(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (category.iconName) {
        "fastfood" -> Icons.Default.Fastfood
        "local_pizza" -> Icons.Default.LocalPizza
        "shopping_cart" -> Icons.Default.ShoppingCart
        "medical_services" -> Icons.Default.MedicalServices
        "local_bar" -> Icons.Default.LocalBar
        else -> Icons.Default.Apps
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("search_chip_category_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ItaSuperPrimary else ItaSuperSecondary,
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else ItaSuperPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else ItaSuperTextPrimary
                )
            )
        }
    }
}

@Composable
fun SearchStoreCard(
    store: Store,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("search_store_card_${store.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo Placeholder
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ItaSuperHighlightBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ItaSuperWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${store.rating}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperWarning
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ItaSuperSecondary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = store.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ItaSuperTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•  ${store.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = store.deliveryTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = ItaSuperTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•   ${store.deliveryFee}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (store.isFreeDelivery) FontWeight.Bold else FontWeight.Normal,
                            color = if (store.isFreeDelivery) ItaSuperSuccess else ItaSuperTextSecondary
                        )
                    )
                }
            }
        }
    }
}
