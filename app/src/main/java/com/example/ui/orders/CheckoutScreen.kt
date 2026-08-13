package com.example.ui.orders

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.repository.StoreRepository
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: OrdersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOrders: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart by viewModel.cartState.collectAsState()

    if (uiState.showGpsAddressConfirmation && cart.deliveryType == "DELIVERY") {
        AlertDialog(
            onDismissRequest = { viewModel.useSavedAddressForCheckout() },
            title = { Text("Confirmar local de entrega") },
            text = {
                Text(
                    "Sua localização atual parece diferente do endereço cadastrado. Deseja manter o endereço salvo ou confirmar os dados da localização atual?"
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.useGpsAddressForCheckout() }) {
                    Text("Usar localização atual")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.useSavedAddressForCheckout() }) {
                    Text("Usar endereço salvo")
                }
            }
        )
    }

    // Confirmation Modal on successful order placement
    if (uiState.placedOrderSuccess != null) {
        OrderSuccessDialog(
            order = uiState.placedOrderSuccess!!,
            onDismiss = {
                viewModel.dismissSuccessModal()
                onNavigateToOrders()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Finalizar Pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Delivery Address / Pickup Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (cart.deliveryType == "RETIRADA") Icons.Default.Storefront else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (cart.deliveryType == "RETIRADA") "Local de Retirada" else "Endereço de Entrega",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (cart.deliveryType == "RETIRADA") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ItaSuperHighlightBg)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = cart.storeName.ifBlank { "Estabelecimento Parceiro" },
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary
                                    )
                                    Text(
                                        text = "Você deverá retirar o pedido diretamente na loja após confirmação de preparo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (!uiState.showAddressEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ItaSuperHighlightBg)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = listOf(uiState.street, uiState.number)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", "),
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = listOf(uiState.neighborhood, uiState.city)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" - "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (uiState.complement.isNotBlank()) {
                                    Text(
                                        text = uiState.complement,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.material3.TextButton(
                                    onClick = viewModel::openAddressEditor,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Alterar endereço")
                                }
                            }
                        } else {
                            // Cadastro, edição ou confirmação da localização atual.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = uiState.cep,
                                    onValueChange = { viewModel.updateCep(it) },
                                    label = { Text("CEP") },
                                    placeholder = { Text("00000-000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_cep_input")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { viewModel.searchAddressByCep() },
                                    enabled = !uiState.isSearchingCep && uiState.cep.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("checkout_search_cep_button")
                                ) {
                                    if (uiState.isSearchingCep) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Buscar CEP"
                                        )
                                    }
                                }
                            }

                            if (uiState.cepError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.cepError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Street & Number
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.street,
                                    onValueChange = { viewModel.updateStreet(it) },
                                    label = { Text("Rua / Logradouro") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(2f)
                                        .testTag("checkout_street_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = uiState.number,
                                    onValueChange = { viewModel.updateNumber(it) },
                                    label = { Text("Nº") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_number_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Neighborhood & City
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.neighborhood,
                                    onValueChange = { viewModel.updateNeighborhood(it) },
                                    label = { Text("Bairro") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_neighborhood_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = uiState.city,
                                    onValueChange = { viewModel.updateCity(it) },
                                    label = { Text("Cidade") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_city_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.complement,
                                onValueChange = { viewModel.updateComplement(it) },
                                label = { Text("Complemento / Ponto de Referência (opcional)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_complement_input")
                            )
                        }
                    }
                }
            }

            // 2. Payment Method Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Forma de Pagamento",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val store = cart.storeId?.let { StoreRepository.getStoreById(it) }
                        val paymentOptions = buildList {
                            val settings = store?.settings
                            if (settings?.acceptPixOnline == true) add("PIX Online")
                            if (settings?.acceptPixMachine == true) add("PIX na Maquininha")
                            if (store?.pixDirectEnabled == true && store.pixDirectKey.isNotBlank()) add("PIX Direto")
                            if (settings?.acceptCard == true) add("Cartão")
                            if (settings?.acceptCash == true) add("Dinheiro")
                        }

                        if (paymentOptions.isEmpty()) {
                            Text(
                                text = "A loja não possui uma forma de pagamento disponível no momento.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            paymentOptions.forEach { method ->
                                val isSelected = uiState.paymentMethod == method
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { viewModel.setPaymentMethod(method) }
                                        )
                                        .background(
                                            if (isSelected) ItaSuperHighlightBg.copy(alpha = 0.5f) else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setPaymentMethod(method) },
                                        colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = when {
                                            method.contains("PIX", ignoreCase = true) -> Icons.Default.Pix
                                            method.contains("Cartão", ignoreCase = true) -> Icons.Default.CreditCard
                                            else -> Icons.Default.AttachMoney
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = method,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        // Cash Change mandatory input
                        if (uiState.paymentMethod == "Dinheiro") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.changeForAmount,
                                onValueChange = { viewModel.updateChangeForAmount(it) },
                                label = { Text("Troco para quanto?") },
                                placeholder = { Text("Ex: 50,00 ou 100,00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_cash_change_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Informe o valor em cédulas para que o entregador leve o troco correto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error message banner
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 3. Order Summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resumo Final do Pedido",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (${cart.totalItemCount} itens)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("R$ ${String.format("%.2f", cart.subtotal).replace(".", ",")}")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxa de entrega", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (cart.deliveryType == "RETIRADA") {
                                Text("Grátis (Retirada)", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            } else if (cart.deliveryFee == 0.0) {
                                Text("Grátis", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            } else {
                                Text("R$ ${String.format("%.2f", cart.deliveryFee).replace(".", ",")}")
                            }
                        }

                        if (cart.discountAmount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Desconto", color = ItaSuperSuccess)
                                Text("- R$ ${String.format("%.2f", cart.discountAmount).replace(".", ",")}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total a pagar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "R$ ${String.format("%.2f", cart.total).replace(".", ",")}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = ItaSuperPrimary
                            )
                        }
                    }
                }
            }

            // 4. Submit Order Button
            item {
                Button(
                    onClick = {
                        viewModel.checkoutOrder {
                            // Triggered on success
                        }
                    },
                    enabled = !uiState.isPlacingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_place_order_button")
                ) {
                    if (uiState.isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Enviando pedido ao Supabase...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = "Confirmar e Enviar Pedido • R$ ${String.format("%.2f", cart.total).replace(".", ",")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun OrderSuccessDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("success_modal_track_button")
            ) {
                Text("Acompanhar em Meus Pedidos", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ItaSuperSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ItaSuperSuccess,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pedido Realizado com Sucesso!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ItaSuperHighlightBg)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Número do Pedido: ${order.id}",
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Seu pedido foi enviado para ${order.storeName} e o status oficial foi registrado no banco de dados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text("R$ ${String.format("%.2f", order.total).replace(".", ",")}", fontWeight = FontWeight.ExtraBold, color = ItaSuperPrimary)
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
