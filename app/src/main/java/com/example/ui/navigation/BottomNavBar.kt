package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ItaSuperBackground
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home")
    object Search : BottomNavItem("busca", "Busca", Icons.Filled.Search, Icons.Outlined.Search, "nav_busca")
    object Orders : BottomNavItem("pedidos", "Pedidos", Icons.Filled.Receipt, Icons.Outlined.Receipt, "nav_pedidos")
    object Notifications : BottomNavItem("notificacoes", "Avisos", Icons.Filled.Notifications, Icons.Outlined.NotificationsNone, "nav_notificacoes")
    object Profile : BottomNavItem("perfil", "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_perfil")
}

@Composable
fun ItaSuperBottomNavBar(
    currentRoute: String,
    unreadNotificationsCount: Int = 0,
    onNavigateToRoute: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Orders,
        BottomNavItem.Notifications,
        BottomNavItem.Profile
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = ItaSuperBackground,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToRoute(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item == BottomNavItem.Notifications && unreadNotificationsCount > 0) {
                                Badge { Text(if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ItaSuperPrimary,
                    selectedTextColor = ItaSuperPrimary,
                    indicatorColor = ItaSuperHighlightBg,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
