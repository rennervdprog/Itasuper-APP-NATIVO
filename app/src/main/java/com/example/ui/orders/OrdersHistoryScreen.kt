package com.example.ui.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess
import kotlinx.coroutines.delay
import java.util.Locale

private data class OrderStatusPresentation(
    val label: String,
    val color: Color,
    val stage: Int
)

private fun Order.presentation(): OrderStatusPresentation = when (status.lowercase()) {
    "aguardando_pagamento" -> OrderStatusPresentation("Aguardando pagamento", Color(0xFFE58A00), 0)
    "aguardando_comprovante" -> OrderStatusPresentation("Aguardando comprovante", Color(0xFFE58A00), 0)
    "comprovante_enviado" -> OrderStatusPresentation("Comprovante enviado", Color(0xFFE58A00), 0)
    "pendente", "recebido" -> OrderStatusPresentation("Pedido recebido", ItaSuperPrimary, 0)
    "preparando", "em_preparo" -> OrderStatusPresentation("Em preparo", ItaSuperPrimary, 1)
    "pronto_para_entrega", "pronto" -> OrderStatusPresentation("Pronto", ItaSuperPrimary, 2)
    "saiu_entrega", "em_transito" -> OrderStatusPresentation("A caminho", ItaSuperPrimary, 3)
    "entregue", "finalizado" -> OrderStatusPresentation("Entregue", ItaSuperSuccess, 4)
    "cancelado" -> OrderStatusPresentation("Cancelado", Color(0xFFD32F2F), 0)
    else -> OrderStatusPresentation(status.replaceFirstChar { it.uppercase() }, ItaSuperPrimary, 0)
}

private fun Order.isFinished(): Boolean = status.lowercase() in setOf("entregue", "finalizado", "cancelado")

private fun Order.estimatedWindow(): String = when (status.lowercase()) {
    "pendente", "recebido" -> "≈ 25–45 min"
    "preparando", "em_preparo" -> "≈ 15–30 min"
    "pronto_para_entrega", "pronto" -> "≈ 10–20 min"
    "saiu_entrega", "em_transito" -> "A caminho"
    "entregue", "finalizado" -> "Concluído"
    else -> "Em atualização"
}

private fun money(value: Double): String = String.format(Locale("pt", "BR"), "R$ %,.2f", value).replace('.', ',')

private fun Order.shortCode(): String = "#${id.take(6).uppercase()}"

private fun Order.createdLabel(): String {
    if (createdAt.length >= 16) {
        val date = createdAt.take(10).split("-")
        val time = createdAt.substring(11, 16)
        if (date.size == 3) return "${date[2]}/${date[1]}, $time"
    }
    return createdAt
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    viewModel: OrdersViewModel,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onExploreClick: () -> Unit
) {
    val orders by viewModel.ordersList.collectAsState()
    val cart by viewModel.cartState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeOrders = orders.filterNot { it.isFinished() }
    val previousOrders = orders.filter { it.isFinished() }
    val orderPendingCancellation = remember { mutableStateOf<Order?>(null) }
    val orderPendingRating = remember { mutableStateOf<Order?>(null) }
    val ratingValue = remember { mutableStateOf(5) }
    val ratingComment = remember { mutableStateOf("") }
    val orderPendingRefund = remember { mutableStateOf<Order?>(null) }
    val refundDescription = remember { mutableStateOf("") }

    orderPendingCancellation.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingCancellation.value = null },
            title = { Text("Cancelar pedido?") },
            text = { Text("O cancelamento será analisado conforme o estágio atual do preparo. Deseja continuar?") },
            confirmButton = {
                Button(
                    onClick = {
                        orderPendingCancellation.value = null
                        viewModel.cancelOrder(order)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmar cancelamento") }
            },
            dismissButton = {
                TextButton(onClick = { orderPendingCancellation.value = null }) { Text("Manter pedido") }
            }
        )
    }

    orderPendingRating.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingRating.value = null },
            title = { Text("Como foi seu pedido?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            TextButton(onClick = { ratingValue.value = star }) {
                                Text(if (star <= ratingValue.value) "★" else "☆", color = ItaSuperPrimary, fontSize = 24.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = ratingComment.value,
                        onValueChange = { ratingComment.value = it },
                        label = { Text("Comentário (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.submitOrderRating(order, ratingValue.value, ratingComment.value) { success ->
                        if (success) {
                            orderPendingRating.value = null
                            ratingComment.value = ""
                        }
                    }
                }) { Text("Enviar avaliação") }
            },
            dismissButton = { TextButton(onClick = { orderPendingRating.value = null }) { Text("Agora não") } }
        )
    }

    orderPendingRefund.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingRefund.value = null },
            title = { Text("Solicitar reembolso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O crédito será analisado pela plataforma e, se aprovado, retornará à sua carteira ItaSuper.")
                    OutlinedTextField(
                        value = refundDescription.value,
                        onValueChange = { refundDescription.value = it },
                        label = { Text("Descreva o problema") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.requestRefund(order, "outro", refundDescription.value) { success ->
                        if (success) {
                            orderPendingRefund.value = null
                            refundDescription.value = ""
                        }
                    }
                }) { Text("Enviar solicitação") }
            },
            dismissButton = { TextButton(onClick = { orderPendingRefund.value = null }) { Text("Cancelar") } }
        )
    }

    uiState.pixPayment?.let { PixPaymentDialog(state = it, onDismiss = viewModel::dismissPixPayment) }
    uiState.pixDirectPayment?.let {
        PixDirectPaymentDialog(
            state = it,
            onUpload = viewModel::uploadPixDirectProof,
            onDismiss = viewModel::dismissPixDirectPayment
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            viewModel.refreshOrders()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.padding(end = 8.dp))
                        Text("Meus Pedidos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart, modifier = Modifier.testTag("orders_to_cart_button")) {
                        BadgedBox(badge = {
                            if (cart.totalItemCount > 0) Badge(containerColor = ItaSuperPrimary) {
                                Text("${cart.totalItemCount}", color = Color.White)
                            }
                        }) {
                            Icon(Icons.Default.ShoppingBag, "Ver Sacola", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { ItaSuperBottomNavBar(currentRoute = "pedidos", onNavigateToRoute = onNavigateToRoute) }
    ) { innerPadding ->
        if (orders.isEmpty()) {
            EmptyOrdersState(innerPadding, onExploreClick)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(2.dp)) }
                if (activeOrders.isNotEmpty()) {
                    item { OrderSectionTitle("PEDIDOS EM ANDAMENTO (${activeOrders.size})") }
                    items(activeOrders, key = { it.id }) { order ->
                        ActiveOrderCard(
                            order = order,
                            onPayPix = { viewModel.generatePixPayment(order) },
                            onPixDirect = { viewModel.openPixDirectPayment(order) },
                            onConfirmDelivery = { viewModel.confirmDelivery(order) },
                            onCancelOrder = { orderPendingCancellation.value = order },
                            isConfirmingDelivery = uiState.confirmingDeliveryOrderId == order.id,
                            isCancelling = uiState.cancellingOrderId == order.id
                        )
                    }
                }
                if (previousOrders.isNotEmpty()) {
                    item { OrderSectionTitle("ANTERIORES (${previousOrders.size})") }
                    items(previousOrders, key = { it.id }) { order ->
                        PreviousOrderCard(
                            order = order,
                            onRepeatOrder = {
                                viewModel.repeatOrder(order)
                                onNavigateToCart()
                            },
                            onRateOrder = { orderPendingRating.value = order },
                            onRefundOrder = { orderPendingRefund.value = order }
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OrderSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun EmptyOrdersState(innerPadding: androidx.compose.foundation.layout.PaddingValues, onExploreClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(ItaSuperHighlightBg),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.size(42.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Nenhum pedido ainda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Quando fizer um pedido, você acompanhará cada etapa por aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
            ) { Text("Explorar lojas", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ActiveOrderCard(
    order: Order,
    onPayPix: () -> Unit,
    onPixDirect: () -> Unit,
    onConfirmDelivery: () -> Unit,
    onCancelOrder: () -> Unit,
    isConfirmingDelivery: Boolean,
    isCancelling: Boolean
) {
    val presentation = order.presentation()
    val isWaitingPayment = order.status.lowercase() in setOf("aguardando_pagamento", "aguardando_comprovante", "comprovante_enviado")
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(presentation)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.estimatedWindow(), style = MaterialTheme.typography.labelMedium, color = ItaSuperPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(order.shortCode(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.storeName.ifBlank { "Loja" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(money(order.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ItaSuperPrimary)
                }
                if (!isWaitingPayment) OrderTimeline(order.presentation().stage)

                if (order.deliveryPin.isNotBlank() && order.neighborhood.uppercase() != "RETIRADA") {
                    DeliveryPinCard(order.deliveryPin)
                }

                if (order.status == "aguardando_pagamento" && order.paymentMethod == "pix") {
                    Button(onClick = onPayPix, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary), modifier = Modifier.fillMaxWidth()) {
                        Text("Pagar com PIX", fontWeight = FontWeight.Bold)
                    }
                }
                if (order.status == "aguardando_comprovante" && order.paymentMethod == "pix_direto") {
                    Button(onClick = onPixDirect, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary), modifier = Modifier.fillMaxWidth()) {
                        Text("Enviar comprovante PIX", fontWeight = FontWeight.Bold)
                    }
                }

                val canConfirmDelivery = order.status.lowercase() in setOf("saiu_entrega", "em_transito") && !order.deliveryConfirmedByClient
                if (canConfirmDelivery) {
                    Button(
                        onClick = onConfirmDelivery,
                        enabled = !isConfirmingDelivery,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isConfirmingDelivery) "Confirmando..." else "Sim, recebi meu pedido!", fontWeight = FontWeight.Bold)
                    }
                }

                OrderFinancialSummary(order)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.createdLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val canCancel = order.status.lowercase() in setOf("pendente", "recebido", "preparando", "em_preparo", "pronto_para_entrega")
                    if (canCancel) {
                        TextButton(onClick = onCancelOrder, enabled = !isCancelling) {
                            Text(if (isCancelling) "Cancelando..." else "Cancelar pedido", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(paymentLabel(order.paymentMethod), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(presentation: OrderStatusPresentation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(presentation.color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(if (presentation.stage >= 4) Icons.Default.CheckCircle else Icons.Default.LocalShipping, null, tint = presentation.color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(presentation.label, color = presentation.color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OrderTimeline(activeStage: Int) {
    val labels = listOf("Recebido", "Preparando", "Pronto", "A caminho", "Entregue")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(if (index <= activeStage) ItaSuperPrimary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (index == 3) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (index <= activeStage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (index <= activeStage) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DeliveryPinCard(pin: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ItaSuperHighlightBg).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = ItaSuperPrimary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Código de entrega", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(pin.chunked(1).joinToString(" "), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Código de entrega", pin))
            }) { Text("Copiar", color = ItaSuperPrimary, fontWeight = FontWeight.Bold) }
        }
        Text("Informe ao motoboy apenas quando receber seu pedido.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OrderFinancialSummary(order: Order) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        order.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(money(item.totalPrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        SummaryLine("Subtotal", money(order.subtotal))
        if (order.deliveryFee > 0) SummaryLine("Taxa de entrega", money(order.deliveryFee))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", fontWeight = FontWeight.ExtraBold)
            Text(money(order.total), fontWeight = FontWeight.ExtraBold, color = ItaSuperPrimary)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviousOrderCard(
    order: Order,
    onRepeatOrder: () -> Unit,
    onRateOrder: () -> Unit,
    onRefundOrder: () -> Unit
) {
    val presentation = order.presentation()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.storeName.ifBlank { "Loja" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${order.createdLabel()} · ${presentation.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(money(order.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (order.status == "cancelado") MaterialTheme.colorScheme.onSurfaceVariant else ItaSuperPrimary)
            }
            Text(
                order.items.joinToString(", ") { "${it.quantity}x ${it.product.name}" }.ifBlank { "Itens do pedido" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (order.status.lowercase() in setOf("entregue", "finalizado")) {
                OutlinedButton(onClick = onRepeatOrder, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = ItaSuperPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Pedir novamente", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onRateOrder) { Text("Avaliar pedido") }
                    if (order.paymentMethod !in setOf("dinheiro", "pix_machine", "cartao")) {
                        TextButton(onClick = onRefundOrder) { Text("Solicitar reembolso") }
                    }
                }
            }
        }
    }
}

private fun paymentLabel(method: String): String = when (method.lowercase()) {
    "pix" -> "Pagamento via PIX"
    "pix_direto" -> "PIX direto"
    "cartao", "card", "credito", "debito" -> "Cartão"
    "dinheiro" -> "Dinheiro"
    else -> method.ifBlank { "Pagamento em atualização" }.replaceFirstChar { it.uppercase() }
}

@Composable
private fun PixPaymentDialog(
    state: PixPaymentUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrImage = remember(state.qrCodeBase64) {
        state.qrCodeBase64?.let(::decodePixQr)
    }
    val pixCode = state.pixCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pagamento PIX",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = ItaSuperPrimary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Gerando QR Code PIX…")
                    }
                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        qrImage?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = "QR Code PIX",
                                modifier = Modifier.size(220.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            text = "Escaneie o QR Code ou copie o código para pagar no aplicativo do seu banco.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!pixCode.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pixCode,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!pixCode.isNullOrBlank() && !state.isLoading && state.errorMessage == null) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código PIX", pixCode))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) {
                    Text("Copiar código PIX")
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)) {
                    Text("Fechar")
                }
            }
        },
        dismissButton = {
            if (!state.isLoading && pixCode != null && state.errorMessage == null) {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun decodePixQr(raw: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val encoded = if (raw.contains(",")) raw.substringAfter(",") else raw
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun PixDirectPaymentDialog(
    state: PixDirectPaymentUiState,
    onUpload: (ByteArray, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "application/pdf" -> "pdf"
                else -> ""
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) onUpload(bytes, mimeType, extension)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pagamento via PIX Direto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Pagando para", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.storeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Valor: R$ ${String.format("%.2f", state.order?.total ?: 0.0).replace(".", ",")}",
                    fontWeight = FontWeight.ExtraBold,
                    color = ItaSuperPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Chave PIX da loja", fontWeight = FontWeight.Bold)
                if (state.beneficiary.isNotBlank()) {
                    Text("Beneficiário: ${state.beneficiary}", style = MaterialTheme.typography.bodySmall)
                }
                if (state.pixKeyType.isNotBlank()) {
                    Text(state.pixKeyType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.pixKey,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                if (state.instructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(state.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    state.isUploading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ItaSuperPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviando comprovante…")
                        }
                    }
                    state.proofSent -> Text(
                        "Comprovante enviado. Aguarde a confirmação da loja.",
                        color = ItaSuperSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    state.errorMessage != null -> Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                    else -> Text("Envie um comprovante JPG, PNG ou PDF de até 5 MB.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (!state.proofSent) {
                Button(
                    onClick = { filePicker.launch(arrayOf("image/jpeg", "image/png", "application/pdf")) },
                    enabled = !state.isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) {
                    Text(if (state.isUploading) "Enviando…" else "Enviar comprovante")
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)) {
                    Text("Fechar")
                }
            }
        },
        dismissButton = {
            if (!state.isUploading) TextButton(onClick = onDismiss) { Text("Voltar") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
