package com.example.ui.notifications

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientNotification
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperBackground
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateToRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ItaSuperBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notificações",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ItaSuperTextPrimary
                            )
                        )
                        if (uiState.unreadCount > 0) {
                            Text(
                                text = "${uiState.unreadCount} nova${if (uiState.unreadCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium.copy(color = ItaSuperPrimary)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadNotifications, modifier = Modifier.testTag("refresh_notifications")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar notificações", tint = ItaSuperTextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "notificacoes",
                unreadNotificationsCount = uiState.unreadCount,
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = ItaSuperPrimary) }
            }
            uiState.notifications.isEmpty() -> EmptyNotificationsContent(
                modifier = Modifier.padding(innerPadding),
                errorMessage = uiState.errorMessage
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("notifications_list"),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        item {
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                color = ItaSuperTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = {
                                viewModel.openNotification(notification) {
                                    onNavigateToRoute("pedidos")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsContent(
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(ItaSuperHighlightBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (errorMessage.isNullOrBlank()) "Nenhuma notificação ainda" else "Não foi possível carregar os avisos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ItaSuperTextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage ?: "As atualizações dos seus pedidos aparecerão aqui.",
            style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun NotificationRow(notification: ClientNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.White else ItaSuperHighlightBg.copy(alpha = 0.52f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("notification_${notification.id}"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(if (notification.isRead) Color(0xFFF2F2F2) else ItaSuperPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = if (notification.isRead) ItaSuperTextSecondary else ItaSuperPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ItaSuperPrimary))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary, lineHeight = 19.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = formatNotificationDate(notification.createdAt),
                style = MaterialTheme.typography.labelSmall.copy(color = ItaSuperTextSecondary)
            )
        }
    }
    Divider(color = Color(0xFFECECEC), thickness = 1.dp)
}

private fun formatNotificationDate(value: String): String {
    if (value.isBlank()) return "Agora"
    return value.take(16).replace("T", " às ")
}
