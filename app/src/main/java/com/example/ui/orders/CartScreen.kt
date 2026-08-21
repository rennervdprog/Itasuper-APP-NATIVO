package com.example.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.repository.StoreRepository
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: OrdersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart by viewModel.cartState.collectAsState()
    val stores by StoreRepository.stores.collectAsState()
    val store = cart.storeId?.let { storeId -> stores.firstOrNull { it.id == storeId } }
    val isStoreClosed = store?.isOpen == false
    val minimumOrder = store?.minOrder ?: 0.0
    val belowMinimum = minimumOrder > 0.0 && cart.subtotal < minimumOrder
    val minimumMissing = (minimumOrder - cart.subtotal).coerceAtLeast(0.0)
    val deliveryQuoteReady = cart.deliveryType == "RETIRADA" || cart.hasOfficialDeliveryQuote
    val isDeliveryUnavailable = store?.let {
        it.deliveryMode.equals("own", ignoreCase = true) && it.hasAvailableDriver == false
    } == true
    val deliveryBlockedForCurrentSelection = isDeliveryUnavailable && cart.deliveryType == "DELIVERY"

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Carrinho",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (cart.items.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${cart.totalItemCount} ${if (cart.totalItemCount == 1) "item" else "itens"}",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("cart_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    if (cart.items.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.testTag("cart_clear_button")
                        ) {
                            Text("Limpar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        }
    ) { innerPadding ->
        if (cart.items.isEmpty()) {
            // Empty State
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
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Seu carrinho está vazio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Explore nossas lojas parceiras e adicione itens deliciosos ao seu pedido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onNavigateToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("empty_cart_explore_button")
                    ) {
                        Text("Explorar Lojas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Store Banner Card
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pedido em ${cart.storeName.ifBlank { "Estabelecimento" }}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${cart.totalItemCount} ${if (cart.totalItemCount == 1) "item" else "itens"} adicionados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Delivery / Pickup Toggle
                item {
                    Text(
                        text = "Modo de Recebimento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFAFAFA))
                            .padding(5.dp)
                    ) {
                        val isDelivery = cart.deliveryType == "DELIVERY"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDelivery) Color(0xFFFFF3EC) else Color.White)
                                .border(if (isDelivery) 2.dp else 1.dp, if (isDelivery) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(14.dp))
                                .clickable { viewModel.setDeliveryType("DELIVERY") }
                                .padding(vertical = 12.dp)
                                .testTag("delivery_type_delivery_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (isDelivery) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Entrega",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDelivery) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isDelivery) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Entrega selecionada",
                                        tint = ItaSuperPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isDelivery) Color(0xFFFFF3EC) else Color.White)
                                .border(if (!isDelivery) 2.dp else 1.dp, if (!isDelivery) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(14.dp))
                                .clickable { viewModel.setDeliveryType("RETIRADA") }
                                .padding(vertical = 12.dp)
                                .testTag("delivery_type_pickup_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (!isDelivery) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retirar na loja",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isDelivery) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isDelivery) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Retirada selecionada",
                                        tint = ItaSuperPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (isDeliveryUnavailable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ItaSuperHighlightBg)
                                .border(1.dp, ItaSuperPrimary.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = store?.deliveryAvailabilityMessage?.ifBlank {
                                    "Entrega indisponível no momento. Selecione retirada para continuar."
                                } ?: "Entrega indisponível no momento. Selecione retirada para continuar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Cart Items
                itemsIndexed(cart.items, key = { idx, item -> "cart_item_${idx}_${item.product.id}" }) { _, cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onUpdateQuantity = { newQty ->
                            viewModel.updateQuantity(cartItem.product.id, newQty)
                        }
                    )
                }

                // Coupon Input Section
                item {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = ItaSuperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cupom de Desconto",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (cart.appliedCoupon != null) {
                                // Applied coupon banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ItaSuperSuccess.copy(alpha = 0.10f))
                                        .border(1.dp, ItaSuperSuccess.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ItaSuperSuccess
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Cupom '${cart.appliedCoupon?.code}' aplicado!",
                                                fontWeight = FontWeight.Bold,
                                                color = ItaSuperSuccess,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Desconto de R$ ${String.format("%.2f", cart.effectiveCouponDiscount).replace(".", ",")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeCoupon() },
                                        modifier = Modifier.testTag("remove_coupon_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover Cupom",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Coupon input field
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = uiState.couponCode,
                                        onValueChange = { viewModel.onCouponCodeChange(it) },
                                        placeholder = { Text("Código do cupom") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ItaSuperPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("coupon_input_field")
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { viewModel.applyCoupon() },
                                        enabled = !uiState.couponLoading && uiState.couponCode.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .height(56.dp)
                                            .testTag("apply_coupon_button")
                                    ) {
                                        if (uiState.couponLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Aplicar", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (uiState.couponError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = uiState.couponError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                // Summary Financial Card
                item {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                            Text(
                                text = "Resumo dos valores",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal dos itens", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("R$ ${String.format("%.2f", cart.subtotal).replace(".", ",")}", fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Taxa de entrega", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                when {
                                    cart.deliveryType == "RETIRADA" -> Text("Grátis (Retirada)", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                    !deliveryQuoteReady -> Text("A confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    cart.deliveryFee == 0.0 -> Text("Grátis", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                    else -> Text("R$ ${String.format("%.2f", cart.deliveryFee).replace(".", ",")}", fontWeight = FontWeight.Medium)
                                }
                            }

                            if (cart.effectiveCouponDiscount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Desconto aplicado", color = ItaSuperSuccess)
                                    Text("- R$ ${String.format("%.2f", cart.effectiveCouponDiscount).replace(".", ",")}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE8DCD4), modifier = Modifier.padding(vertical = 14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFEEDF))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(if (deliveryQuoteReady) "Total" else "Total parcial", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (deliveryQuoteReady) "Valor final do pedido" else "A taxa será confirmada no checkout",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = "R$ ${String.format("%.2f", cart.total).replace(".", ",")}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = ItaSuperPrimary
                                )
                            }
                        }
                    }

                if (isStoreClosed || belowMinimum || deliveryBlockedForCurrentSelection) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isStoreClosed) MaterialTheme.colorScheme.errorContainer else ItaSuperHighlightBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    isStoreClosed -> "Esta loja está fechada no momento. O pedido não pode ser finalizado agora."
                                    deliveryBlockedForCurrentSelection -> store?.deliveryAvailabilityMessage?.ifBlank {
                                        "Entrega indisponível no momento. Selecione retirada para continuar."
                                    } ?: "Entrega indisponível no momento. Selecione retirada para continuar."
                                    else -> "Pedido mínimo: R$ ${String.format("%.2f", minimumOrder).replace(".", ",")}. Faltam R$ ${String.format("%.2f", minimumMissing).replace(".", ",")}."
                                },
                                color = if (isStoreClosed) MaterialTheme.colorScheme.onErrorContainer else ItaSuperPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                // Proceed Button
                item {
                    Button(
                        onClick = onNavigateToCheckout,
                        enabled = !isStoreClosed && !belowMinimum && !deliveryBlockedForCurrentSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("proceed_to_checkout_button")
                    ) {
                        Text(
                            text = when {
                                isStoreClosed -> "Loja fechada"
                                belowMinimum -> "Faltam R$ ${String.format("%.2f", minimumMissing).replace(".", ",")}"
                                deliveryBlockedForCurrentSelection -> "Selecione retirada para continuar"
                                else -> if (deliveryQuoteReady) {
                                    "Avançar para Checkout • R$ ${String.format("%.2f", cart.total).replace(".", ",")}"
                                } else {
                                    "Avançar para Checkout • Taxa a confirmar"
                                }

                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onUpdateQuantity: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cartItem.product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = cartItem.product.imageUrl,
                    contentDescription = cartItem.product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ItaSuperHighlightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (cartItem.selectedAddons.isNotEmpty()) {
                    val addonsText = cartItem.selectedAddons.joinToString(", ") { it.itemName }
                    Text(
                        text = "Adicionais: $addonsText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (cartItem.notes.isNotBlank()) {
                    Text(
                        text = "Obs: ${cartItem.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "R$ ${String.format("%.2f", cartItem.totalPrice).replace(".", ",")}",
                    fontWeight = FontWeight.ExtraBold,
                    color = ItaSuperPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF6F6F6))
                    .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(18.dp))
                    .padding(horizontal = 3.dp, vertical = 3.dp)
            ) {
                IconButton(
                    onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (cartItem.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "Diminuir",
                        tint = if (cartItem.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${cartItem.quantity}",
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                IconButton(
                    onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar",
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
