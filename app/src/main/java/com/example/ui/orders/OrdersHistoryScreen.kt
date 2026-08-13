package com.example.ui.orders

import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess

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

    uiState.pixPayment?.let { pixPayment ->
        PixPaymentDialog(
            state = pixPayment,
            onDismiss = viewModel::dismissPixPayment
        )
    }
    uiState.pixDirectPayment?.let { pixDirect ->
        PixDirectPaymentDialog(
            state = pixDirect,
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
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Meus Pedidos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("orders_to_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cart.totalItemCount > 0) {
                                    Badge(containerColor = ItaSuperPrimary) {
                                        Text("${cart.totalItemCount}", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Ver Sacola",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "pedidos",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { innerPadding ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(ItaSuperHighlightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Nenhum pedido realizado ainda",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Seus pedidos realizados na plataforma serão listados aqui para fácil acompanhamento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("empty_orders_explore_button")
                    ) {
                        Text("Explorar Lojas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                itemsIndexed(orders, key = { idx, order -> "order_${idx}_${order.id}" }) { _, order ->
                    OrderHistoryCard(
                        order = order,
                        onPayPix = { viewModel.generatePixPayment(order) },
                        onPixDirect = { viewModel.openPixDirectPayment(order) },
                        onRepeatOrder = {
                            viewModel.repeatOrder(order)
                            onNavigateToCart()
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    onPayPix: () -> Unit,
    onPixDirect: () -> Unit,
    onRepeatOrder: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Store Name, Order ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.storeName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Pedido ${order.id} • ${order.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status real retornado por orders, formatado para o cliente.
                val normalizedStatus = order.status.lowercase()
                val statusLabel = when (normalizedStatus) {
                    "aguardando_pagamento" -> "Aguardando pagamento"
                    "aguardando_comprovante" -> "Aguardando comprovante"
                    "comprovante_enviado" -> "Comprovante enviado"
                    "pendente" -> "Pedido recebido"
                    "preparando" -> "Em preparo"
                    "pronto_para_entrega" -> "Pronto para entrega"
                    "em_transito", "saiu_entrega" -> "Saiu para entrega"
                    "entregue", "finalizado" -> "Entregue"
                    "cancelado" -> "Cancelado"
                    else -> order.status.replaceFirstChar { it.uppercase() }
                }
                val isDelivered = normalizedStatus in setOf("entregue", "finalizado")
                val badgeColor = when {
                    isDelivered -> ItaSuperSuccess
                    normalizedStatus == "cancelado" -> MaterialTheme.colorScheme.error
                    normalizedStatus.startsWith("aguardando") -> Color(0xFFFF9800)
                    else -> ItaSuperPrimary
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Items Summary
            Text(
                text = "Itens do pedido:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.product.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", item.totalPrice).replace(".", ",")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address & Payment Info
            Text(
                text = "Endereço: ${order.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Pagamento: ${order.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val awaitingPix = order.status == "aguardando_pagamento" && order.paymentMethod == "pix"
            if (awaitingPix) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPayPix,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_pix_button_${order.id}")
                ) {
                    Text("Pagar com PIX", fontWeight = FontWeight.Bold)
                }
            }
            val awaitingPixDirect = order.status == "aguardando_comprovante" && order.paymentMethod == "pix_direto"
            if (awaitingPixDirect) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPixDirect,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_pix_direct_button_${order.id}")
                ) {
                    Text("Enviar comprovante PIX", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Total and Repeat Order Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Pago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", order.total).replace(".", ",")}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = ItaSuperPrimary
                    )
                }

                OutlinedButton(
                    onClick = onRepeatOrder,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("repeat_order_button_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pedir de novo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
