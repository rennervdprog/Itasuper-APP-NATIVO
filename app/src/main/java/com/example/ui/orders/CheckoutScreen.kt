package com.example.ui.orders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Order
import com.example.data.model.preorderReleaseAtMillis
import com.example.data.repository.StoreRepository
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: OrdersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOrders: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart by viewModel.cartState.collectAsState()
    val checkoutTotal = if (uiState.benefitsStoreId == cart.storeId && !uiState.isLoadingBenefits) uiState.finalTotal else cart.total
    val deliveryQuoteReady = cart.deliveryType == "RETIRADA" || (
        !uiState.isQuotingDelivery && cart.hasOfficialDeliveryQuote
        )
    val context = LocalContext.current
    val store = cart.storeId?.let { StoreRepository.getStoreById(it) }
    val preorderReleaseAtMillis = store?.preorderReleaseAtMillis()
    val isPreorder = preorderReleaseAtMillis != null
    val scheduleFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    val openSchedulePicker = {
        val earliest = System.currentTimeMillis() + 30 * 60 * 1000L
        val calendar = Calendar.getInstance().apply { timeInMillis = maxOf(earliest, uiState.scheduledForMillis ?: earliest) }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.setScheduledForMillis(selected)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = earliest
        }.show()
    }

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
        containerColor = Color(0xFFFAFAFA),
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
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Delivery Address / Pickup Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(
                                    if (cart.deliveryType == "RETIRADA") R.drawable.ic_ita_store else R.drawable.ic_ita_pin
                                ),
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (cart.deliveryType == "RETIRADA") "Local de Retirada" else "Endereço de Entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (cart.deliveryType == "DELIVERY" && uiState.savedAddresses.isNotEmpty()) {
                            Text(
                                text = "Endereços salvos",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            uiState.savedAddresses.forEach { address ->
                                val selected = address.id == uiState.selectedSavedAddressId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF0E6) else Color.White)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(16.dp))
                                        .selectable(
                                            selected = selected,
                                            onClick = { viewModel.selectSavedAddress(address) }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_ita_check),
                                            contentDescription = "Endereço selecionado",
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, Color(0xFFBDAEA5), CircleShape)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = address.label,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = listOf(address.displayLine, address.neighborhood)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (address.pinConfirmed) {
                                            Text(
                                                text = "Localização confirmada",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ItaSuperSuccess
                                            )
                                        }
                                    }
                                    if (!address.isDefault) {
                                        androidx.compose.material3.TextButton(onClick = { viewModel.makeSavedAddressDefault(address) }) {
                                            Text("Padrão")
                                        }
                                    }
                                    androidx.compose.material3.TextButton(onClick = { viewModel.deleteSavedAddress(address) }) {
                                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (cart.deliveryType == "RETIRADA") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFF0E6))
                                .border(1.dp, Color(0xFFF7CBAE), RoundedCornerShape(16.dp))
                                .padding(14.dp)
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
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = listOf(uiState.street, uiState.number)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", "),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                            val isGpsAddress = uiState.usingGpsAddress
                            if (isGpsAddress) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF3E8))
                                        .border(1.dp, Color(0xFFF7CBAE), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Localização atual selecionada",
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (uiState.number.isBlank()) {
                                            "1. Digite o número do imóvel. 2. Aguarde a confirmação da entrega. 3. Role até o final e toque em Confirmar e Enviar Pedido."
                                        } else {
                                            "Confira o número do imóvel. Depois aguarde a confirmação da entrega e toque em Confirmar e Enviar Pedido no final da tela."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = uiState.cep,
                                    onValueChange = { viewModel.updateCep(it) },
                                    label = { Text("CEP") },
                                    placeholder = { Text("00000-000") },
                                    readOnly = isGpsAddress,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_cep_input")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { viewModel.searchAddressByCep() },
                                    enabled = !isGpsAddress && !uiState.isSearchingCep && uiState.cep.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(14.dp),
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
                                            painter = painterResource(R.drawable.ic_ita_search),
                                            contentDescription = "Buscar CEP",
                                            tint = Color.White
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
                                    readOnly = isGpsAddress,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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
                                    readOnly = isGpsAddress,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_neighborhood_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = uiState.city,
                                    onValueChange = { viewModel.updateCity(it) },
                                    label = { Text("Cidade") },
                                    readOnly = isGpsAddress,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_city_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.state,
                                onValueChange = { viewModel.updateState(it) },
                                label = { Text("UF (opcional)") },
                                placeholder = { Text("Ex: RJ") },
                                readOnly = isGpsAddress,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ItaSuperPrimary,
                                    unfocusedBorderColor = Color(0xFFE5DAD3),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_state_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.complement,
                                onValueChange = { viewModel.updateComplement(it) },
                                label = { Text("Complemento / Ponto de Referência (opcional)") },
                                readOnly = isGpsAddress,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_complement_input")
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.saveCurrentAddress() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (isGpsAddress) "Salvar para próximas compras" else "Salvar como endereço")
                            }
                            if (isGpsAddress) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = viewModel::confirmGpsAddressForCheckout,
                                    enabled = !uiState.isQuotingDelivery && !uiState.isSearchingCep,
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("confirm_gps_address_button")
                                ) {
                                    Text(
                                        text = "Confirmar localização e usar neste pedido",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (cart.deliveryType == "DELIVERY") {
                            Spacer(modifier = Modifier.height(10.dp))
                            when {
                                cart.hasOfficialDeliveryQuote && uiState.deliveryQuote == null && !uiState.isQuotingDelivery -> {
                                    Text(
                                        text = "Taxa fixa confirmada pela regra da loja",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ItaSuperSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                uiState.isQuotingDelivery -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Calculando a taxa de entrega...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                uiState.deliveryQuote?.isSuccessfulDelivery == true -> {
                                    val quote = uiState.deliveryQuote!!
                                    Text(
                                        text = if (quote.pricing.freeDeliveryApplied) {
                                            "Frete grátis aplicado pela regra da loja"
                                        } else {
                                            "Entrega confirmada para ${"%.1f".format(quote.distance.km)} km"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ItaSuperSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                uiState.deliveryQuoteFailure != null -> {
                                    Text(
                                        text = uiState.deliveryQuoteFailure!!.userMessage(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {
                                    Text(
                                        text = if (uiState.usingGpsAddress && uiState.number.isBlank()) {
                                            "Digite o número do imóvel para calcular a entrega."
                                        } else if (uiState.usingGpsAddress) {
                                            "Confira o endereço e aguarde a confirmação da entrega para prosseguir."
                                        } else {
                                            "Preencha CEP, rua, número e bairro para calcular a entrega."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Payment Method Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Forma de pagamento",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
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

                        LaunchedEffect(paymentOptions.joinToString("|"), uiState.paymentMethod) {
                            if (paymentOptions.isNotEmpty() && uiState.paymentMethod !in paymentOptions) {
                                viewModel.setPaymentMethod(paymentOptions.first())
                            }
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
                                PaymentSelectionCard(
                                    method = method,
                                    selected = isSelected,
                                    onClick = { viewModel.setPaymentMethod(method) }
                                )
                            }
                        }

                        // Dinheiro exato é permitido; o valor só é solicitado quando houver troco.
                        if (uiState.paymentMethod == "Dinheiro") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (uiState.needsChange) Color(0xFFFFF0E6) else Color(0xFFF7F7F7))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.needsChange,
                                    onCheckedChange = viewModel::setNeedsChange,
                                    colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                                )
                                Text(
                                    text = "Preciso de troco",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (uiState.needsChange) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = uiState.changeForAmount,
                                    onValueChange = { viewModel.updateChangeForAmount(it) },
                                    label = { Text("Troco para quanto?") },
                                    placeholder = { Text("Ex: 50,00 ou 100,00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = Color(0xFFE5DAD3),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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
            }

            // 3. Agendamento. A data é opcional; no pré-pedido fechado, sem seleção significa liberação na abertura.
            if (store != null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Text(
                                text = if (cart.deliveryType == "RETIRADA") "Agendar retirada" else "Agendar entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isPreorder) {
                                Text(
                                    text = "A loja está fechada, mas aceita pré-pedidos. O pedido será liberado às ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(preorderReleaseAtMillis ?: 0L))}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.setScheduledForMillis(null) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.scheduledForMillis == null) ItaSuperPrimary else Color.White,
                                        contentColor = if (uiState.scheduledForMillis == null) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isPreorder) "Na abertura" else "Agora")
                                }
                                Button(
                                    onClick = openSchedulePicker,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.scheduledForMillis != null) ItaSuperPrimary else Color.White,
                                        contentColor = if (uiState.scheduledForMillis != null) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Escolher horário")
                                }
                            }
                            uiState.scheduledForMillis?.let { scheduledAt ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Agendado para ${scheduleFormatter.format(Date(scheduledAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Carteira e fidelidade
            if (uiState.isLoadingBenefits || uiState.loyaltyConfig != null || uiState.walletBalance > 0) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Text("Benefícios ItaSuper", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(10.dp))
                            if (uiState.isLoadingBenefits) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Carregando carteira e fidelidade...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                uiState.loyaltyConfig?.let { config ->
                                    val canRedeem = uiState.loyaltyMaxPointsUsable >= config.minPointsRedeem
                                    Text(
                                        text = "Fidelidade nesta loja",
                                        fontWeight = FontWeight.SemiBold,
                                        color = ItaSuperPrimary
                                    )
                                    Text(
                                        text = "Você tem ${uiState.loyaltyPointsAvailable} pontos disponíveis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (canRedeem) {
                                        val selectedPoints = uiState.loyaltyPointsToUse.takeIf { it > 0 } ?: config.minPointsRedeem
                                        val selectedDiscount = selectedPoints * config.discountPerPoint
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.TextButton(
                                                enabled = selectedPoints > config.minPointsRedeem,
                                                onClick = { viewModel.applyLoyaltyPoints(selectedPoints - 10) }
                                            ) { Text("− 10") }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$selectedPoints pontos", fontWeight = FontWeight.Bold)
                                                Text("= R$ ${String.format("%.2f", selectedDiscount).replace(".", ",")}", color = ItaSuperSuccess, style = MaterialTheme.typography.bodySmall)
                                            }
                                            androidx.compose.material3.TextButton(
                                                enabled = selectedPoints < uiState.loyaltyMaxPointsUsable,
                                                onClick = { viewModel.applyLoyaltyPoints(selectedPoints + 10) }
                                            ) { Text("+ 10") }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { viewModel.applyLoyaltyPoints(selectedPoints) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                        shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text(
                                                if (uiState.loyaltyPointsToUse > 0) "Atualizar resgate" else "Aplicar $selectedPoints pontos",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (uiState.loyaltyPointsToUse > 0) {
                                            androidx.compose.material3.TextButton(
                                                onClick = viewModel::removeLoyaltyPoints,
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("Remover pontos aplicados") }
                                        }
                                    } else {
                                        Text(
                                            text = "Você precisa de pelo menos ${config.minPointsRedeem} pontos para resgatar. Este pedido acumula novos pontos.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                if (uiState.walletBalance > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (uiState.useWallet) Color(0xFFFFF0E6) else Color.White)
                                            .border(if (uiState.useWallet) 2.dp else 1.dp, if (uiState.useWallet) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(16.dp))
                                            .selectable(
                                                selected = uiState.useWallet,
                                                onClick = { viewModel.setUseWallet(!uiState.useWallet) }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(if (uiState.useWallet) Color(0xFFFFE8DA) else Color(0xFFF5F5F5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_ita_card),
                                                contentDescription = null,
                                                tint = if (uiState.useWallet) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Usar crédito da carteira", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                if (uiState.useWallet) "Usando R$ ${String.format("%.2f", uiState.walletDiscount).replace(".", ",")}" else "Saldo disponível: R$ ${String.format("%.2f", uiState.walletBalance).replace(".", ",")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (uiState.useWallet) ItaSuperSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (uiState.useWallet) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_ita_check),
                                                contentDescription = "Carteira selecionada",
                                                tint = ItaSuperPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Resumo final do pedido",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
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
                            when {
                                cart.deliveryType == "RETIRADA" -> Text("Grátis (Retirada)", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                uiState.isQuotingDelivery -> Text("Calculando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                !deliveryQuoteReady -> Text("A confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                cart.deliveryFee == 0.0 -> Text("Grátis", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                else -> Text("R$ ${String.format("%.2f", cart.deliveryFee).replace(".", ",")}")
                            }
                        }

                        if (cart.effectiveCouponDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cupom", color = ItaSuperSuccess)
                                Text("- R$ ${String.format("%.2f", cart.effectiveCouponDiscount).replace(".", ",")}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (uiState.loyaltyDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fidelidade (${uiState.loyaltyPointsToUse} pontos)", color = ItaSuperSuccess)
                                Text("- R$ ${String.format("%.2f", uiState.loyaltyDiscount).replace(".", ",")}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (uiState.walletDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Carteira ItaSuper", color = ItaSuperSuccess)
                                Text("- R$ ${String.format("%.2f", uiState.walletDiscount).replace(".", ",")}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
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
                                Text("Total a pagar", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text("Revise os dados antes de confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                text = "R$ ${String.format("%.2f", checkoutTotal).replace(".", ",")}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
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
                    enabled = !uiState.isPlacingOrder && deliveryQuoteReady,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("confirm_place_order_button")
                ) {
                    if (uiState.isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enviando pedido para ${cart.storeName.ifBlank { "a loja" }}...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (cart.deliveryType == "DELIVERY" && uiState.isQuotingDelivery) {
                                "Calculando entrega..."
                            } else {
                                "Confirmar e Enviar Pedido • R$ ${String.format("%.2f", checkoutTotal).replace(".", ",")}"
                            },
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
private fun PaymentSelectionCard(
    method: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconRes = when {
        method.contains("PIX", ignoreCase = true) -> R.drawable.ic_ita_pix
        method.contains("Cartão", ignoreCase = true) -> R.drawable.ic_ita_card
        else -> R.drawable.ic_ita_cash
    }
    val description = when {
        method.equals("PIX Online", ignoreCase = true) -> "Pagamento confirmado no aplicativo"
        method.equals("PIX Direto", ignoreCase = true) -> "Envie o comprovante depois do pedido"
        method.equals("PIX na Maquininha", ignoreCase = true) -> "Pague na entrega"
        method.equals("Cartão", ignoreCase = true) -> "Pague na entrega"
        else -> "Informe o valor para troco"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFFFF3EC) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(if (selected) ItaSuperPrimary else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_ita_check),
                    contentDescription = "Selecionado",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEAEAEA))
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
                shape = RoundedCornerShape(14.dp),
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
                        painter = painterResource(R.drawable.ic_ita_check),
                        contentDescription = null,
                        tint = ItaSuperSuccess,
                        modifier = Modifier.size(38.dp)
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
                        text = order.customerOrderLabel(),
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Seu pedido foi enviado para ${order.storeName.ifBlank { "a loja" }}. Acompanhe o andamento em Meus Pedidos.",
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
