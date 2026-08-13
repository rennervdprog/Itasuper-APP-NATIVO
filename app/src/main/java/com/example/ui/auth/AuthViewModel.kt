package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SupabaseClient
import com.example.data.repository.UserSessionRepository
import com.example.ui.utils.Masks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthMode {
    object Login : AuthMode()
    object Register : AuthMode()
}

data class AuthUiState(
    val authMode: AuthMode = AuthMode.Login,
    // Login fields
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,
    
    // Register fields
    val regName: String = "",
    val regCpfCnpj: String = "",
    val regWhatsapp: String = "",
    val regPassword: String = "",
    val isRegPasswordVisible: Boolean = false,
    val regPin: String = "",
    val regPinConfirm: String = "",
    val isRegPinVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,

    // Error messages per field or general
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,

    // Dialog & Sheet States
    val showForgotPasswordDialog: Boolean = false,
    val recoveryEmail: String = "",
    val showTermsSheet: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setAuthMode(mode: AuthMode) {
        _uiState.value = _uiState.value.copy(
            authMode = mode,
            errorMessage = null,
            successMessage = null
        )
    }

    // Login updates
    fun onLoginEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(loginEmail = value, errorMessage = null)
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(loginPassword = value, errorMessage = null)
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isLoginPasswordVisible = !_uiState.value.isLoginPasswordVisible
        )
    }

    // Register updates
    fun onRegNameChange(value: String) {
        _uiState.value = _uiState.value.copy(regName = value, errorMessage = null)
    }

    fun onRegCpfCnpjChange(value: String) {
        val formatted = Masks.formatCpfCnpj(value)
        _uiState.value = _uiState.value.copy(regCpfCnpj = formatted, errorMessage = null)
    }

    fun onRegWhatsappChange(value: String) {
        val formatted = Masks.formatPhone(value)
        _uiState.value = _uiState.value.copy(regWhatsapp = formatted, errorMessage = null)
    }

    fun onRegPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(regPassword = value, errorMessage = null)
    }

    fun toggleRegPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isRegPasswordVisible = !_uiState.value.isRegPasswordVisible
        )
    }

    fun onRegPinChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(4)
        _uiState.value = _uiState.value.copy(regPin = digits, errorMessage = null)
    }

    fun onRegPinConfirmChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(4)
        _uiState.value = _uiState.value.copy(regPinConfirm = digits, errorMessage = null)
    }

    fun toggleRegPinVisibility() {
        _uiState.value = _uiState.value.copy(
            isRegPinVisible = !_uiState.value.isRegPinVisible
        )
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(isTermsAccepted = accepted, errorMessage = null)
    }

    // Forgot password dialog
    fun openForgotPasswordDialog() {
        _uiState.value = _uiState.value.copy(
            showForgotPasswordDialog = true,
            recoveryEmail = _uiState.value.loginEmail
        )
    }

    fun closeForgotPasswordDialog() {
        _uiState.value = _uiState.value.copy(showForgotPasswordDialog = false)
    }

    fun onRecoveryEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(recoveryEmail = value)
    }

    fun openTermsSheet() {
        _uiState.value = _uiState.value.copy(showTermsSheet = true)
    }

    fun closeTermsSheet() {
        _uiState.value = _uiState.value.copy(showTermsSheet = false)
    }

    fun sendRecoveryEmail() {
        val email = _uiState.value.recoveryEmail.trim()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Informe um e-mail válido para recuperação")
            return
        }
        _uiState.value = _uiState.value.copy(
            showForgotPasswordDialog = false,
            successMessage = "E-mail de recuperação enviado!",
            errorMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    // Actions
    fun handleLogin(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.loginEmail.trim()
        val password = state.loginPassword

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = state.copy(errorMessage = "Informe um e-mail válido")
            return
        }
        if (password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Senha: mínimo 6 caracteres")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = SupabaseClient.signIn(email, password)
            if (result.isSuccess) {
                UserSessionRepository.login(
                    email = email,
                    userId = result.userId,
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresAtSeconds = result.expiresAt
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Login realizado!",
                    errorMessage = null
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "Falha ao realizar login"
                )
            }
        }
    }

    fun handleRegister(onSuccess: () -> Unit) {
        val state = _uiState.value

        // Validate Nome completo
        val name = state.regName.trim()
        if (name.isBlank() || !name.contains(" ")) {
            _uiState.value = state.copy(errorMessage = "Informe seu nome completo")
            return
        }

        // Validate CPF/CNPJ
        if (!Masks.isValidCpfOrCnpj(state.regCpfCnpj)) {
            _uiState.value = state.copy(errorMessage = "CPF ou CNPJ inválido")
            return
        }

        // Validate WhatsApp com DDD
        if (!Masks.isValidPhone(state.regWhatsapp)) {
            _uiState.value = state.copy(errorMessage = "Informe um WhatsApp válido com DDD")
            return
        }

        // Validate Password
        if (state.regPassword.length < 6) {
            _uiState.value = state.copy(errorMessage = "Senha: mínimo 6 caracteres")
            return
        }

        // Validate PIN
        if (state.regPin.length != 4) {
            _uiState.value = state.copy(errorMessage = "Defina um PIN de entrega com 4 dígitos numéricos")
            return
        }

        // Validate PIN Confirmation
        if (state.regPin != state.regPinConfirm) {
            _uiState.value = state.copy(errorMessage = "Os PINs informados não coincidem")
            return
        }

        // Validate Terms of Use
        if (!state.isTermsAccepted) {
            _uiState.value = state.copy(errorMessage = "Você precisa aceitar os Termos de Uso para continuar")
            return
        }

        val email = if (state.loginEmail.isNotBlank()) state.loginEmail.trim() else "cliente_${System.currentTimeMillis()}@itasuper.com.br"

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val authResult = SupabaseClient.signUp(email, state.regPassword)
            if (authResult.isSuccess && authResult.userId != null) {
                // Insert profile record into Supabase 'profiles' table
                SupabaseClient.insertProfile(
                    userId = authResult.userId,
                    fullName = name,
                    document = state.regCpfCnpj,
                    whatsappNumber = state.regWhatsapp,
                    email = email,
                    deliveryPin = state.regPin,
                    accessToken = authResult.accessToken
                )

                // Save local user session
                UserSessionRepository.register(
                    name = name,
                    email = email,
                    cpfCnpj = state.regCpfCnpj,
                    whatsapp = state.regWhatsapp,
                    pin = state.regPin,
                    userId = authResult.userId,
                    accessToken = authResult.accessToken,
                    refreshToken = authResult.refreshToken,
                    expiresAtSeconds = authResult.expiresAt
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Conta criada!",
                    errorMessage = null
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = authResult.errorMessage ?: "Erro ao criar conta no servidor"
                )
            }
        }
    }
}
