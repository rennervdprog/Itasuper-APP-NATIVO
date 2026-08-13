package com.example.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSession
import com.example.data.remote.SupabaseClient
import com.example.data.repository.UserSessionRepository
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.permissions.LocationOnboardingPreferences
import com.example.ui.theme.ItaSuperBorder
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess
import com.example.ui.theme.ItaSuperTextSecondary
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val SECTION_PERSONAL = "personal"
private const val SECTION_ADDRESS = "address"

@Composable
fun ProfileScreen(
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit
) {
    val session by UserSessionRepository.userSession.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var personalDirty by remember { mutableStateOf(false) }
    var addressDirty by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf(session.name) }
    var document by remember { mutableStateOf(session.cpfCnpj) }
    var cep by remember { mutableStateOf(session.addressCep) }
    var street by remember { mutableStateOf(session.addressStreet) }
    var number by remember { mutableStateOf(session.addressNumber) }
    var complement by remember { mutableStateOf(session.addressComplement) }
    var neighborhood by remember { mutableStateOf(session.addressNeighborhood) }
    var city by remember { mutableStateOf(session.addressCity) }
    var state by remember { mutableStateOf(session.addressState) }
    var referencePoint by remember { mutableStateOf(session.addressReferencePoint) }
    var whatsapp by remember { mutableStateOf(session.whatsapp) }
    var pin by remember { mutableStateOf("") }
    var pinConfirmation by remember { mutableStateOf("") }
    var deleteReason by remember { mutableStateOf("") }
    var isSavingPersonal by remember { mutableStateOf(false) }
    var isSavingAddress by remember { mutableStateOf(false) }
    var isLookingUpCep by remember { mutableStateOf(false) }
    var isSavingPin by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.name, session.cpfCnpj) {
        if (!personalDirty) {
            fullName = session.name
            document = session.cpfCnpj
        }
    }
    LaunchedEffect(
        session.addressCep,
        session.addressStreet,
        session.addressNumber,
        session.addressComplement,
        session.addressNeighborhood,
        session.addressCity,
        session.addressState,
        session.addressReferencePoint,
        session.whatsapp
    ) {
        if (!addressDirty) {
            cep = session.addressCep
            street = session.addressStreet
            number = session.addressNumber
            complement = session.addressComplement
            neighborhood = session.addressNeighborhood
            city = session.addressCity
            state = session.addressState
            referencePoint = session.addressReferencePoint
            whatsapp = session.whatsapp
        }
    }

    val displayName = session.name.ifBlank { session.email.substringBefore("@").ifBlank { "Cliente ItaSuper" } }
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "CI" }
    val hasPersonalData = session.name.isNotBlank() && session.cpfCnpj.isNotBlank()
    val hasAddress = session.addressStreet.isNotBlank() && session.addressNumber.isNotBlank() &&
        session.addressNeighborhood.isNotBlank() && session.addressCity.isNotBlank()
    val hasWhatsapp = session.whatsapp.filter { it.isDigit() }.length >= 10
    val hasPin = session.deliveryPin.matches(Regex("^\\d{4}$"))
    val completed = listOf(hasPersonalData, hasAddress, hasWhatsapp, hasPin).count { it }
    val progress = completed / 4f

    fun toggleSection(section: String) {
        expandedSection = if (expandedSection == section) null else section
    }

    fun openWhatsApp(message: String) {
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/5522992796291?text=$encoded"))
        runCatching { context.startActivity(intent) }
            .onFailure { feedback = "Não foi possível abrir o WhatsApp neste aparelho." }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingPin) showPinDialog = false },
            title = { Text(if (hasPin) "Alterar PIN de entrega" else "Definir PIN de entrega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("O entregador solicitará este código de 4 dígitos ao concluir a entrega.")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Novo PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinConfirmation,
                        onValueChange = { pinConfirmation = it.filter(Char::isDigit).take(4) },
                        label = { Text("Confirmar PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSavingPin,
                    onClick = {
                        when {
                            !pin.matches(Regex("^\\d{4}$")) -> feedback = "Informe um PIN de 4 dígitos."
                            pin != pinConfirmation -> feedback = "Os PINs não coincidem."
                            else -> scope.launch {
                                isSavingPin = true
                                val saved = SupabaseClient.updateCustomerDeliveryPin(
                                    session.userId,
                                    session.accessToken,
                                    pin
                                )
                                isSavingPin = false
                                if (saved) {
                                    UserSessionRepository.updatePin(pin)
                                    pin = ""
                                    pinConfirmation = ""
                                    showPinDialog = false
                                    feedback = "PIN de entrega atualizado."
                                } else {
                                    feedback = "Não foi possível salvar o PIN. Tente novamente."
                                }
                            }
                        }
                    }
                ) {
                    if (isSavingPin) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Salvar PIN")
                }
            },
            dismissButton = {
                TextButton(enabled = !isSavingPin, onClick = { showPinDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você poderá entrar novamente a qualquer momento usando seu e-mail e senha.") },
            confirmButton = {
                Button(onClick = {
                    showLogoutConfirmation = false
                    onLogout()
                }) { Text("Sair") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirmation = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirmation = false },
            title = { Text("Excluir minha conta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Esta ação é definitiva. Seus dados serão tratados conforme as regras da plataforma.")
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCE3730)),
                    onClick = {
                        scope.launch {
                            isDeletingAccount = true
                            val error = SupabaseClient.deleteCustomerAccount(session.accessToken, deleteReason)
                            isDeletingAccount = false
                            if (error == null) {
                                showDeleteConfirmation = false
                                UserSessionRepository.logout()
                                onLogout()
                            } else {
                                feedback = error
                            }
                        }
                    }
                ) {
                    if (isDeletingAccount) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Excluir conta")
                }
            },
            dismissButton = { TextButton(enabled = !isDeletingAccount, onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(currentRoute = "perfil", onNavigateToRoute = onNavigateToRoute)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileHero(displayName, session.email, initials) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-18).dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (completed < 4) {
                        CompletionCard(
                            completed = completed,
                            progress = progress,
                            nextLabel = when {
                                !hasPersonalData -> "Dados pessoais"
                                !hasAddress -> "Endereço"
                                !hasWhatsapp -> "WhatsApp"
                                else -> "PIN de entrega"
                            },
                            onClick = {
                                when {
                                    !hasPersonalData -> expandedSection = SECTION_PERSONAL
                                    !hasAddress || !hasWhatsapp -> expandedSection = SECTION_ADDRESS
                                    else -> showPinDialog = true
                                }
                            }
                        )
                    }

                    feedback?.let { message ->
                        FeedbackCard(message = message, onDismiss = { feedback = null })
                    }

                    SectionCaption("Acesso rápido")
                    QuickActionGrid(
                        onOrders = { onNavigateToRoute("pedidos") },
                        onSupport = {
                            openWhatsApp("Olá! Sou $displayName (${session.email}) e preciso de ajuda no ItaSuper.")
                        }
                    )

                    SectionCaption("Conta")
                    AccountSectionCard(
                        icon = Icons.Default.Person,
                        title = "Dados pessoais",
                        subtitle = session.name.ifBlank { "Toque para cadastrar seu nome" },
                        complete = hasPersonalData,
                        expanded = expandedSection == SECTION_PERSONAL,
                        onToggle = { toggleSection(SECTION_PERSONAL) }
                    ) {
                        ProfileTextField(
                            value = fullName,
                            onValueChange = { personalDirty = true; fullName = it },
                            label = "Nome completo"
                        )
                        ProfileTextField(
                            value = document,
                            onValueChange = { personalDirty = true; document = it.filter(Char::isDigit).take(14) },
                            label = "CPF ou CNPJ",
                            keyboardType = KeyboardType.Number
                        )
                        ProfileTextField(
                            value = session.email,
                            onValueChange = {},
                            label = "E-mail",
                            enabled = false
                        )
                        Button(
                            enabled = !isSavingPersonal,
                            onClick = {
                                when {
                                    fullName.trim().isBlank() -> feedback = "Informe seu nome completo."
                                    document.filter(Char::isDigit).length !in 11..14 -> feedback = "Informe um CPF ou CNPJ válido."
                                    else -> scope.launch {
                                        isSavingPersonal = true
                                        val saved = SupabaseClient.updateCustomerPersonalProfile(
                                            session.userId,
                                            session.accessToken,
                                            fullName.trim(),
                                            document.filter(Char::isDigit)
                                        )
                                        isSavingPersonal = false
                                        if (saved) {
                                            UserSessionRepository.updatePersonal(
                                                name = fullName.trim(),
                                                document = document.filter(Char::isDigit)
                                            )
                                            personalDirty = false
                                            feedback = "Dados pessoais atualizados."
                                        } else {
                                            feedback = "Não foi possível salvar seus dados. Tente novamente."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                        ) {
                            if (isSavingPersonal) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Salvar dados pessoais")
                        }
                    }

                    AccountSectionCard(
                        icon = Icons.Default.LocationOn,
                        title = "Endereço de entrega",
                        subtitle = listOf(session.addressStreet, session.addressNumber, session.addressNeighborhood)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { "Toque para cadastrar seu endereço" },
                        complete = hasAddress,
                        expanded = expandedSection == SECTION_ADDRESS,
                        onToggle = { toggleSection(SECTION_ADDRESS) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileTextField(
                                value = cep,
                                onValueChange = { addressDirty = true; cep = it.filter(Char::isDigit).take(8) },
                                label = "CEP",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = !isLookingUpCep && cep.length == 8,
                                onClick = {
                                    scope.launch {
                                        isLookingUpCep = true
                                        val result = SupabaseClient.fetchAddressByCep(cep)
                                        isLookingUpCep = false
                                        if (result == null) {
                                            feedback = "CEP não encontrado. Confira ou preencha manualmente."
                                        } else {
                                            addressDirty = true
                                            street = result.street.ifBlank { street }
                                            neighborhood = result.neighborhood.ifBlank { neighborhood }
                                            city = result.city.ifBlank { city }
                                            state = result.state.ifBlank { state }
                                        }
                                    }
                                },
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                            ) {
                                if (isLookingUpCep) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else Icon(Icons.Default.Search, contentDescription = "Buscar CEP")
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ProfileTextField(
                                value = street,
                                onValueChange = { addressDirty = true; street = it },
                                label = "Rua",
                                modifier = Modifier.weight(2f)
                            )
                            Spacer(Modifier.width(8.dp))
                            ProfileTextField(
                                value = number,
                                onValueChange = { addressDirty = true; number = it },
                                label = "Nº",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ProfileTextField(
                            value = complement,
                            onValueChange = { addressDirty = true; complement = it },
                            label = "Complemento (opcional)"
                        )
                        ProfileTextField(
                            value = neighborhood,
                            onValueChange = { addressDirty = true; neighborhood = it },
                            label = "Bairro"
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ProfileTextField(
                                value = city,
                                onValueChange = { addressDirty = true; city = it },
                                label = "Cidade",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            ProfileTextField(
                                value = state,
                                onValueChange = { addressDirty = true; state = it.uppercase().take(2) },
                                label = "UF",
                                modifier = Modifier.weight(0.45f)
                            )
                        }
                        ProfileTextField(
                            value = referencePoint,
                            onValueChange = { addressDirty = true; referencePoint = it },
                            label = "Ponto de referência (opcional)"
                        )
                        ProfileTextField(
                            value = whatsapp,
                            onValueChange = { addressDirty = true; whatsapp = it.filter { char -> char.isDigit() }.take(13) },
                            label = "WhatsApp com DDD",
                            keyboardType = KeyboardType.Phone
                        )
                        Button(
                            enabled = !isSavingAddress,
                            onClick = {
                                when {
                                    cep.length != 8 -> feedback = "Informe um CEP válido com 8 dígitos."
                                    street.isBlank() || number.isBlank() || neighborhood.isBlank() -> feedback = "Preencha rua, número e bairro."
                                    city.isBlank() || state.length != 2 -> feedback = "Informe cidade e UF."
                                    whatsapp.filter(Char::isDigit).length < 10 -> feedback = "Informe um WhatsApp válido com DDD."
                                    else -> scope.launch {
                                        isSavingAddress = true
                                        val saved = SupabaseClient.updateUserProfileAddress(
                                            userId = session.userId,
                                            accessToken = session.accessToken,
                                            cep = cep,
                                            street = street.trim(),
                                            number = number.trim(),
                                            complement = complement.trim(),
                                            neighborhood = neighborhood.trim(),
                                            city = city.trim(),
                                            state = state.trim(),
                                            referencePoint = referencePoint.trim(),
                                            whatsapp = whatsapp.filter(Char::isDigit)
                                        )
                                        isSavingAddress = false
                                        if (saved) {
                                            UserSessionRepository.updateProfile(
                                                name = session.name,
                                                whatsapp = whatsapp.filter(Char::isDigit),
                                                street = street.trim(),
                                                number = number.trim(),
                                                neighborhood = neighborhood.trim(),
                                                cep = cep,
                                                pixKeyType = session.pixKeyType,
                                                pixKey = session.pixKey,
                                                city = city.trim(),
                                                state = state.trim(),
                                                complement = complement.trim(),
                                                referencePoint = referencePoint.trim()
                                            )
                                            addressDirty = false
                                            feedback = "Endereço atualizado e disponível no checkout."
                                        } else {
                                            feedback = "Não foi possível salvar o endereço. Tente novamente."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                        ) {
                            if (isSavingAddress) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Salvar endereço")
                        }
                    }

                    PinCard(hasPin = hasPin, onClick = { showPinDialog = true })

                    SectionCaption("Ajuda e suporte")
                    MenuCard {
                        ProfileMenuRow(
                            icon = Icons.Default.Message,
                            title = "Falar com o suporte",
                            subtitle = "WhatsApp — resposta em minutos",
                            onClick = { openWhatsApp("Olá! Sou $displayName (${session.email}) e preciso de ajuda no ItaSuper.") }
                        )
                        ProfileDivider()
                        ProfileMenuRow(
                            icon = Icons.Default.ReportProblem,
                            title = "Reportar um problema",
                            subtitle = "Envie um relato ao suporte",
                            onClick = {
                                openWhatsApp("[BUG] ItaSuper\nUsuário: $displayName\nE-mail: ${session.email}\n\nDescreva o problema:")
                            }
                        )
                    }

                    SectionCaption("Sobre o ItaSuper")
                    MenuCard {
                        ProfileMenuRow(
                            icon = Icons.Default.School,
                            title = "Ver tutorial novamente",
                            subtitle = "Reveja o guia de localização ao voltar para a Home",
                            onClick = {
                                LocationOnboardingPreferences.reset(context, session.userId)
                                feedback = "Tutorial preparado. Volte para a Home para visualizá-lo."
                            }
                        )
                        ProfileDivider()
                        ProfileMenuRow(
                            icon = Icons.Default.Share,
                            title = "Compartilhar o app",
                            subtitle = "Convide amigos para usar o ItaSuper",
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Peça no ItaSuper: https://itasuper.com.br")
                                }
                                runCatching { context.startActivity(Intent.createChooser(share, "Compartilhar ItaSuper")) }
                                    .onFailure { feedback = "Não foi possível abrir o compartilhamento." }
                            }
                        )
                    }

                    SectionCaption("Conta")
                    MenuCard {
                        ProfileMenuRow(
                            icon = Icons.Default.Logout,
                            title = "Sair da conta",
                            subtitle = "Encerrar esta sessão neste aparelho",
                            onClick = { showLogoutConfirmation = true },
                            destructive = true
                        )
                        ProfileDivider()
                        ProfileMenuRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Excluir minha conta",
                            subtitle = "Remover sua conta do ItaSuper",
                            onClick = { showDeleteConfirmation = true },
                            destructive = true
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(name: String, email: String, initials: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = listOf(ItaSuperPrimary, Color(0xFFE95D00))))
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 42.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(email.ifBlank { "Conta ItaSuper" }, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(modifier = Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CLIENTE",
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 9.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionCard(completed: Int, progress: Float, nextLabel: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Complete seu cadastro", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text("$completed/4", color = ItaSuperPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = ItaSuperPrimary, trackColor = ItaSuperHighlightBg)
            Text("Próximo: $nextLabel", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = ItaSuperTextSecondary)
        }
    }
}

@Composable
private fun FeedbackCard(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ItaSuperHighlightBg).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f), color = Color(0xFF744100), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onDismiss) { Text("OK") }
    }
}

@Composable
private fun SectionCaption(title: String) {
    Text(title.uppercase(), modifier = Modifier.padding(start = 2.dp, top = 2.dp), style = MaterialTheme.typography.labelSmall, color = ItaSuperTextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
}

@Composable
private fun QuickActionGrid(onOrders: () -> Unit, onSupport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction(Modifier.weight(1f), Icons.Default.ShoppingBag, "Meus pedidos", "Acompanhe", onOrders)
        QuickAction(Modifier.weight(1f), Icons.Default.Message, "Suporte", "Precisa de ajuda?", onSupport)
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(102.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(ItaSuperHighlightBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(19.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AccountSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    complete: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ItaSuperHighlightBg), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(7.dp))
                        StatusBadge(complete)
                    }
                    Text(subtitle, color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ItaSuperTextSecondary.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                HorizontalDivider(color = ItaSuperBorder.copy(alpha = 0.65f))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            }
        }
    }
}

@Composable
private fun StatusBadge(complete: Boolean) {
    Text(
        if (complete) "OK" else "PENDENTE",
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (complete) ItaSuperSuccess.copy(alpha = 0.12f) else Color(0xFFFFF0D5)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = if (complete) ItaSuperSuccess else Color(0xFFB96A00),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PinCard(hasPin: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ItaSuperHighlightBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Key, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PIN de entrega", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(7.dp))
                    StatusBadge(hasPin)
                }
                Text(if (hasPin) "•• •• (código exigido pelo entregador)" else "Defina um PIN de 4 dígitos", color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Text(if (hasPin) "Alterar" else "Definir", color = ItaSuperPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder)
    ) { Column { content() } }
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val tint = if (destructive) Color(0xFFCE3730) else ItaSuperPrimary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (destructive) Color(0xFFFFECEB) else ItaSuperHighlightBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (destructive) tint else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tint.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = ItaSuperBorder.copy(alpha = 0.65f))
}
