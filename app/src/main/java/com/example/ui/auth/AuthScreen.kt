package com.example.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ItaSuperError
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperHighlightText
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess
import com.example.ui.theme.ItaSuperWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.authMode) {
        if (uiState.authMode is AuthMode.Register) {
            viewModel.loadRegistrationLegalDocuments()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFFFBF8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFAF7))
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            AuthHeader()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = 28.dp)
                    .border(1.dp, Color(0xFFF3E5DD), RoundedCornerShape(30.dp)),
                shape = RoundedCornerShape(30.dp),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthTabRow(
                        selectedMode = uiState.authMode,
                        onModeSelected = { viewModel.setAuthMode(it) }
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                    AuthModeIntro(mode = uiState.authMode)
                    Spacer(modifier = Modifier.height(20.dp))

                    uiState.errorMessage?.let { error ->
                        ErrorBanner(message = error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.authMode is AuthMode.Login) {
                        LoginForm(
                            uiState = uiState,
                            viewModel = viewModel,
                            onLoginClick = {
                                focusManager.clearFocus()
                                viewModel.handleLogin(onSuccess = onAuthSuccess)
                            }
                        )
                    } else {
                        RegisterForm(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenLegalDocument = { url ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            },
                            onRegisterClick = {
                                focusManager.clearFocus()
                                viewModel.handleRegister(onSuccess = onAuthSuccess)
                            }
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
            ForgotPasswordDialog(
                email = uiState.recoveryEmail,
                isSending = uiState.isSendingRecovery,
                onEmailChange = viewModel::onRecoveryEmailChange,
                onDismiss = viewModel::closeForgotPasswordDialog,
                onSend = viewModel::sendRecoveryEmail
            )
    }

}

@Composable
private fun AuthHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(222.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF0E7), Color(0xFFFFFAF7))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.itasuper_brand_icon),
                contentDescription = "Ícone oficial ItaSuper",
                modifier = Modifier.size(66.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Image(
                painter = painterResource(id = R.drawable.itasuper_brand_wordmark_cropped),
                contentDescription = "ItaSuper",
                modifier = Modifier
                    .width(196.dp)
                    .height(44.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mercado e delivery, do seu jeito.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF766159),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun AuthModeIntro(mode: AuthMode) {
    val isLogin = mode is AuthMode.Login
    Text(
        text = if (isLogin) "Que bom ter você de volta" else "Sua próxima compra começa aqui",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            color = Color(0xFF251A15)
        ),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = if (isLogin) "Entre para encontrar suas lojas favoritas e acompanhar seus pedidos." else "Crie sua conta para pedir com rapidez, segurança e praticidade.",
        style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF796860),
            lineHeight = 22.sp
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AuthTabRow(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFFFF0E8))
            .padding(4.dp)
    ) {
        val isLogin = selectedMode is AuthMode.Login

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isLogin) ItaSuperPrimary else Color.Transparent)
                .clickable { onModeSelected(AuthMode.Login) }
                .testTag("tab_login"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Entrar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isLogin) Color.White else Color(0xFF6C5B53)
                )
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(if (!isLogin) ItaSuperPrimary else Color.Transparent)
                .clickable { onModeSelected(AuthMode.Register) }
                .testTag("tab_register"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Criar conta",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (!isLogin) Color.White else Color(0xFF6C5B53)
                )
            )
        }
    }
}

@Composable
private fun LoginForm(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // E-mail field
        OutlinedTextField(
            value = uiState.loginEmail,
            onValueChange = viewModel::onLoginEmailChange,
            label = { Text("E-mail") },
            placeholder = { Text("seu.email@exemplo.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = ItaSuperPrimary)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .testTag("login_email_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // Password field
        OutlinedTextField(
            value = uiState.loginPassword,
            onValueChange = viewModel::onLoginPasswordChange,
            label = { Text("Senha") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ItaSuperPrimary)
            },
            trailingIcon = {
                IconButton(onClick = viewModel::toggleLoginPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Alternar visibilidade da senha"
                    )
                }
            },
            visualTransformation = if (uiState.isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .testTag("login_password_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // Forgot password link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = viewModel::openForgotPasswordDialog,
                modifier = Modifier.testTag("forgot_password_button")
            ) {
                Text(
                    text = "Esqueci minha senha",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ItaSuperPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Login Submit Button
        Button(
            onClick = onLoginClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ItaSuperPrimary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Entrar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Acesso seguro e dados protegidos",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF806D64),
                    fontWeight = FontWeight.Medium
                )
            )
        }

    }
}

@Composable
private fun RegisterForm(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    onOpenLegalDocument: (String) -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Nome Completo
        OutlinedTextField(
            value = uiState.regName,
            onValueChange = viewModel::onRegNameChange,
            label = { Text("Nome completo *") },
            placeholder = { Text("Ex: Maria Silva") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_name_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // CPF / CNPJ com máscara e validação
        OutlinedTextField(
            value = uiState.regCpfCnpj,
            onValueChange = viewModel::onRegCpfCnpjChange,
            label = { Text("CPF ou CNPJ *") },
            placeholder = { Text("000.000.000-00") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_cpf_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // WhatsApp com DDD
        OutlinedTextField(
            value = uiState.regWhatsapp,
            onValueChange = viewModel::onRegWhatsappChange,
            label = { Text("WhatsApp com DDD *") },
            placeholder = { Text("(21) 99999-9999") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_whatsapp_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // Senha
        OutlinedTextField(
            value = uiState.regPassword,
            onValueChange = viewModel::onRegPasswordChange,
            label = { Text("Senha *") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ItaSuperPrimary) },
            trailingIcon = {
                IconButton(onClick = viewModel::toggleRegPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Alternar visibilidade"
                    )
                }
            },
            visualTransformation = if (uiState.isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_password_input"),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ItaSuperPrimary,
                unfocusedBorderColor = Color(0xFFE5DED9),
                focusedLabelColor = ItaSuperPrimary
            )
        )

        // Box de PIN de entrega (4 dígitos numéricos)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6F1))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pin, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PIN de Entrega (4 dígitos)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "O PIN garante a segurança no momento da entrega do seu pedido.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // PIN input
                OutlinedTextField(
                    value = uiState.regPin,
                    onValueChange = viewModel::onRegPinChange,
                    label = { Text("PIN de entrega (4 dígitos) *") },
                    placeholder = { Text("Ex: 1234") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleRegPinVisibility) {
                            Icon(
                                imageVector = if (uiState.isRegPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Alternar visibilidade PIN"
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("reg_pin_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ItaSuperPrimary,
                        unfocusedBorderColor = Color(0xFFE5DED9),
                        focusedLabelColor = ItaSuperPrimary
                    )
                )

                // PIN Confirmation
                OutlinedTextField(
                    value = uiState.regPinConfirm,
                    onValueChange = viewModel::onRegPinConfirmChange,
                    label = { Text("Confirme o PIN de entrega *") },
                    placeholder = { Text("Repita o PIN de 4 dígitos") },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("reg_pin_confirm_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ItaSuperPrimary,
                        unfocusedBorderColor = Color(0xFFE5DED9),
                        focusedLabelColor = ItaSuperPrimary
                    )
                )

                // Visual Pin Match Indicator
                if (uiState.regPin.length == 4 && uiState.regPinConfirm.length == 4) {
                    val isMatch = uiState.regPin == uiState.regPinConfirm
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isMatch) ItaSuperSuccess else ItaSuperError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMatch) "PINs coincidem!" else "Os PINs informados não coincidem",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isMatch) ItaSuperSuccess else ItaSuperError,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        val legalDocuments = uiState.pendingLegalChanges
        if (uiState.isLoadingLegalDocuments) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ItaSuperPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Carregando Termos e Política de Privacidade...", style = MaterialTheme.typography.bodySmall)
            }
        } else if (legalDocuments != null) {
            if (legalDocuments.needsTerms) {
                LegalRegisterCheckbox(
                    checked = uiState.isTermsAccepted,
                    onCheckedChange = viewModel::onTermsAcceptedChange,
                    prefix = "Li e aceito os ",
                    title = "Termos de Uso",
                    version = legalDocuments.currentTermsVersion,
                    url = "https://itasuper.com.br/termos-de-uso",
                    onOpen = onOpenLegalDocument,
                    testTag = "terms_checkbox"
                )
            }
            if (legalDocuments.needsPrivacy) {
                LegalRegisterCheckbox(
                    checked = uiState.isPrivacyAccepted,
                    onCheckedChange = viewModel::onPrivacyAcceptedChange,
                    prefix = "Li e aceito a ",
                    title = "Política de Privacidade",
                    version = legalDocuments.currentPrivacyVersion,
                    url = "https://itasuper.com.br/politica-de-privacidade",
                    onOpen = onOpenLegalDocument,
                    testTag = "privacy_checkbox"
                )
            }
        } else {
            Text(
                "Os Termos e a Política de Privacidade precisam ser carregados antes do cadastro.",
                style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
            onClick = onRegisterClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("register_submit_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Criar conta",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun LegalRegisterCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    prefix: String,
    title: String,
    version: String,
    url: String,
    onOpen: (String) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary),
                modifier = Modifier.testTag(testTag)
            )
            Column(modifier = Modifier.padding(top = 10.dp).weight(1f)) {
                Text(
                    text = "$prefix$title versão $version.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5D5D5D))
                )
                Text(
                    text = "Ler $title",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ItaSuperPrimary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .clickable { onOpen(url) }
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    isSending: Boolean,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Recuperar Senha",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Informe seu e-mail cadastrado no ItaSuper. Enviaremos um link de recuperação.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("E-mail para recuperação") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ItaSuperPrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_password_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                    onClick = onSend,
                    enabled = !isSending,
                    shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                modifier = Modifier.testTag("send_recovery_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsBottomSheet(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Termos de Uso - ItaSuper",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "Bem-vindo ao ItaSuper, sua plataforma de supermercados, restaurantes e farmácias.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "1. Uso da Conta: O usuário é responsável por manter o sigilo de suas credenciais e do seu PIN de entrega de 4 dígitos.\n" +
                        "2. Entregas e Pedidos: Os pedidos realizados são processados pelas lojas parceiras e entregues no endereço informado.\n" +
                        "3. PIN de Entrega: Na entrega do seu pedido, você deverá fornecer o PIN de 4 dígitos ao entregador como comprovação de recebimento.\n" +
                        "4. Pagamento e Troco: Selecione a forma de pagamento adequada no checkout. Em dinheiro, informe se necessita de troco.\n" +
                        "5. Privacidade: Seus dados pessoais e de localização são protegidos conforme a LGPD e utilizados estritamente para o processamento de suas compras.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("accept_terms_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text("Concordo com os Termos")
            }
        }
    }
}
