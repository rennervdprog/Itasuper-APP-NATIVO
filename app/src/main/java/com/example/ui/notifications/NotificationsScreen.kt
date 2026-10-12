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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClientNotification
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperBackground
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class NotificationsFilter(val label: String) {
    ALL("Todos"),
    UNREAD("Não lidos"),
    ORDER("Pedidos"),
    DELIVERY("Entrega"),
    PROMO("Promoções")
}

private fun ClientNotification.matchesFilter(filter: NotificationsFilter): Boolean = when (filter) {
    NotificationsFilter.ALL -> true
    NotificationsFilter.UNREAD -> !isRead
    NotificationsFilter.ORDER -> type.lowercase() in setOf("preparando", "pronto_para_entrega", "cancelado", "finalizado", "order_update")
    NotificationsFilter.DELIVERY -> type.lowercase() in setOf("saiu_entrega", "em_transito", "entregue")
    NotificationsFilter.PROMO -> type.lowercase() in setOf("promocao", "promotion", "promo", "cupom", "coupon", "oferta", "offer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateToRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(NotificationsFilter.ALL) }
    val filteredNotifications = uiState.notifications.filter { it.matchesFilter(selectedFilter.value) }

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
        while (true) {
            delay(15_000)
            viewModel.loadNotifications()
        }
    }

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
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                    } else {
                        IconButton(onClick = viewModel::loadNotifications, modifier = Modifier.testTag("refresh_notifications")) {
                            Icon(
                                painter = painterResource(R.drawable.ic_ita_refresh),
                                contentDescription = "Atualizar notificações",
                                tint = ItaSuperTextPrimary
                            )
                        }
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
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadNotifications() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = ItaSuperPrimary) }
                }
                uiState.notifications.isEmpty() -> EmptyNotificationsContent(
                    errorMessage = uiState.errorMessage,
                    everHadNotifications = uiState.everHadNotifications,
                    onExploreClick = { onNavigateToRoute("busca") }
                )
                else -> {
                    val grouped = remember(filteredNotifications) { groupNotificationsByDate(filteredNotifications) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("notifications_list"),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            NotificationsFilterBar(
                                selected = selectedFilter.value,
                                onSelect = { selectedFilter.value = it }
                            )
                        }
                        if (uiState.unreadCount > 0) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = viewModel::markAllAsRead,
                                        modifier = Modifier.testTag("mark_all_read")
                                    ) {
                                        Text(
                                            text = "Marcar todas como lidas",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ItaSuperPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (uiState.isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                                    Text("Sincronizando avisos", style = MaterialTheme.typography.labelMedium, color = ItaSuperTextSecondary)
                                }
                            }
                        }
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
                        if (filteredNotifications.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhum aviso nesta categoria.",
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ItaSuperTextSecondary
                                )
                            }
                        } else {
                            grouped.forEach { (group, groupItems) ->
                                item {
                                    DateGroupHeader(label = group.label)
                                }
                                items(groupItems, key = { it.id }) { notification ->
                                    NotificationRow(
                                        notification = notification,
                                        onMarkRead = { viewModel.openNotification(notification) {} },
                                        onDismiss = { viewModel.dismissNotification(notification.id) },
                                        onClick = {
                                            viewModel.openNotification(notification) {
                                                val route = notification.orderId
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { "pedidos?orderId=$it" }
                                                    ?: "pedidos"
                                                onNavigateToRoute(route)
                                            }
                                        }
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
private fun NotificationsFilterBar(selected: NotificationsFilter, onSelect: (NotificationsFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NotificationsFilter.entries, key = { it.name }) { filter ->
            val isSelected = filter == selected
            TextButton(
                onClick = { onSelect(filter) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) ItaSuperHighlightBg else Color.White,
                    contentColor = if (isSelected) ItaSuperPrimary else ItaSuperTextSecondary
                ),
                modifier = Modifier.testTag("notifications_filter_${filter.name.lowercase()}")
            ) {
                Text(filter.label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyNotificationsContent(
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    everHadNotifications: Boolean = false,
    onExploreClick: () -> Unit = {}
) {
    val title: String
    val body: String
    val showExploreCta: Boolean
    when {
        !errorMessage.isNullOrBlank() -> {
            title = "Não foi possível carregar os avisos"
            body = errorMessage
            showExploreCta = false
        }
        everHadNotifications -> {
            title = "Você está em dia! 🎉"
            body = "Nenhum aviso novo por aqui."
            showExploreCta = false
        }
        else -> {
            title = "Nenhuma notificação ainda"
            body = "As atualizações dos seus pedidos aparecerão aqui."
            showExploreCta = true
        }
    }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(ItaSuperHighlightBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ita_bell),
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ItaSuperTextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (showExploreCta) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaSuperPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Explorar ofertas", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationRow(
    notification: ClientNotification,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkRead()
                    false // executa a ação e volta (não remove a linha)
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDismiss()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !notification.isRead,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val isMarkRead = direction == SwipeToDismissBoxValue.StartToEnd
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isMarkRead) Color(0xFF43A047) else Color(0xFFE53935))
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (isMarkRead) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isMarkRead) Icons.Filled.Check else Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isMarkRead) "Marcar como lida" else "Excluir",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        },
        content = { NotificationRowContent(notification = notification, onClick = onClick) }
    )
    Divider(color = Color(0xFFECECEC), thickness = 1.dp)
}

@Composable
private fun NotificationRowContent(notification: ClientNotification, onClick: () -> Unit) {
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
            val iconRes = when (notification.type.lowercase()) {
                "saiu_entrega", "em_transito", "entregue" -> R.drawable.ic_ita_delivery
                "pronto_para_entrega", "finalizado" -> R.drawable.ic_ita_check
                else -> R.drawable.ic_ita_bell
            }
            Icon(
                painter = painterResource(iconRes),
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
}

@Composable
private fun DateGroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = ItaSuperTextSecondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(ItaSuperBackground)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

private enum class NotificationDateGroup(val label: String) {
    TODAY("Hoje"),
    YESTERDAY("Ontem"),
    THIS_WEEK("Esta semana"),
    OLDER("Anteriores")
}

/** createdAt vem do banco em UTC (ex: "2026-10-12T17:06:00Z"). Retorna null se inválido. */
private fun parseNotificationDateUtc(value: String): Date? {
    return try {
        val normalized = value
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .take(19)
        if (normalized.length < 16) return null
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(normalized)
    } catch (_: Throwable) {
        null
    }
}

private fun startOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun dateGroupOf(notification: ClientNotification, nowMillis: Long): NotificationDateGroup {
    val date = parseNotificationDateUtc(notification.createdAt) ?: return NotificationDateGroup.OLDER
    val dayMs = 24L * 60 * 60 * 1000
    val diffDays = (startOfDayMillis(nowMillis) - startOfDayMillis(date.time)) / dayMs
    return when {
        diffDays <= 0 -> NotificationDateGroup.TODAY
        diffDays == 1L -> NotificationDateGroup.YESTERDAY
        diffDays < 7 -> NotificationDateGroup.THIS_WEEK
        else -> NotificationDateGroup.OLDER
    }
}

private fun groupNotificationsByDate(
    notifications: List<ClientNotification>
): List<Pair<NotificationDateGroup, List<ClientNotification>>> {
    val now = System.currentTimeMillis()
    return notifications
        .groupBy { dateGroupOf(it, now) }
        .toSortedMap(compareBy { it.ordinal })
        .map { (group, items) -> group to items }
}

private fun formatNotificationDate(value: String): String {
    if (value.isBlank()) return "Agora"
    val fallback = value.take(16).replace("T", " às ")
    val date = parseNotificationDateUtc(value) ?: return fallback
    return try {
        // Converte UTC -> fuso local do aparelho (mesmo padrão do fix em OrdersHistoryScreen).
        SimpleDateFormat("yyyy-MM-dd 'às' HH:mm", Locale("pt", "BR")).apply {
            timeZone = TimeZone.getDefault()
        }.format(date)
    } catch (_: Throwable) {
        fallback
    }
}
