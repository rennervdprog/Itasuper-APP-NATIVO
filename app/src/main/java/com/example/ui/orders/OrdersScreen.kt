package com.example.ui.orders

import androidx.compose.runtime.Composable

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    onNavigateToRoute: (String) -> Unit,
    onExploreClick: () -> Unit,
    onNavigateToCart: () -> Unit = { onNavigateToRoute("carrinho") }
) {
    OrdersHistoryScreen(
        viewModel = viewModel,
        onNavigateToRoute = onNavigateToRoute,
        onNavigateToCart = onNavigateToCart,
        onExploreClick = onExploreClick
    )
}
