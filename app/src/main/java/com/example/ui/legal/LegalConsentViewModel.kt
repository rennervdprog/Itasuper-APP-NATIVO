package com.example.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LegalAcceptanceMode
import com.example.data.model.PendingLegalChanges
import com.example.data.model.UserSession
import com.example.data.remote.SupabaseClient
import com.example.data.repository.LegalNoticeStorage
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LegalConsentUiState(
    val isChecking: Boolean = false,
    val isAccepting: Boolean = false,
    val pending: PendingLegalChanges? = null,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val errorMessage: String? = null,
    val showRefusalPanel: Boolean = false
) {
    val requiresAcceptance: Boolean
        get() = pending?.requiresAcceptance == true

    /** Aviso prévio: os Termos prometem 30 dias em que nada muda, então não pode bloquear. */
    val isNotice: Boolean
        get() = pending?.mode == LegalAcceptanceMode.NOTICE

    /** Somente o modo binding prende o usuário na tela. */
    val isBlocking: Boolean
        get() = pending?.isBlocking == true

    val canDismiss: Boolean
        get() = requiresAcceptance && !isBlocking && !isAccepting

    val canAccept: Boolean
        get() = pending != null &&
            (!pending.needsTerms || acceptedTerms) &&
            (!pending.needsPrivacy || acceptedPrivacy) &&
            !isAccepting
}

class LegalConsentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LegalConsentUiState())
    val uiState: StateFlow<LegalConsentUiState> = _uiState.asStateFlow()

    private var checkedUserId: String? = null

    fun checkForUpdates(session: UserSession) {
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            checkedUserId = null
            _uiState.value = LegalConsentUiState()
            return
        }
        if (checkedUserId == session.userId || _uiState.value.isChecking) return

        checkedUserId = session.userId
        _uiState.value = _uiState.value.copy(isChecking = true, errorMessage = null)
        viewModelScope.launch {
            val profile = SupabaseClient.fetchCustomerProfile(session.userId, session.accessToken)
            val pending = SupabaseClient.fetchPendingLegalChanges(
                termsAccepted = profile?.termsVersionAccepted,
                privacyAccepted = profile?.privacyVersionAccepted?.ifBlank { profile.termsVersionAccepted },
                accessToken = session.accessToken
            )
            _uiState.value = if (pending != null && shouldShow(pending)) {
                LegalConsentUiState(
                    pending = pending,
                    acceptedTerms = !pending.needsTerms,
                    acceptedPrivacy = !pending.needsPrivacy
                )
            } else {
                LegalConsentUiState()
            }
        }
    }

    /** Aviso já adiado nesta sessão não reaparece; aceite obrigatório sempre aparece. */
    private fun shouldShow(pending: PendingLegalChanges): Boolean {
        if (!pending.requiresAcceptance) return false
        if (pending.isBlocking) return true
        return !LegalNoticeStorage.isDismissed(pending.versionKey)
    }

    fun setTermsAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedTerms = accepted, errorMessage = null)
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedPrivacy = accepted, errorMessage = null)
    }

    fun setRefusalPanelVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showRefusalPanel = visible, errorMessage = null)
    }

    /** Adia o aviso prévio. Nunca dispensa um aceite obrigatório. */
    fun dismissNotice() {
        val state = _uiState.value
        val pending = state.pending ?: return
        if (!state.canDismiss) return
        LegalNoticeStorage.dismiss(pending.versionKey)
        _uiState.value = LegalConsentUiState()
    }

    fun accept(session: UserSession) {
        val state = _uiState.value
        val pending = state.pending ?: return
        if (!state.canAccept) return

        _uiState.value = state.copy(isAccepting = true, errorMessage = null)
        viewModelScope.launch {
            val result = SupabaseClient.recordLegalAcceptance(
                userId = session.userId,
                accessToken = session.accessToken,
                termsVersion = pending.currentTermsVersion,
                privacyVersion = pending.currentPrivacyVersion,
                // Aceitar antes da vigência é permitido e registrado como tal.
                acceptanceMode = pending.mode
            )
            if (result.success) {
                LegalNoticeStorage.dismiss(pending.versionKey)
                _uiState.value = LegalConsentUiState()
                UserSessionRepository.synchronizeProfileFromRemote()
            } else {
                _uiState.value = state.copy(
                    isAccepting = false,
                    errorMessage = result.errorMessage ?: "Não foi possível registrar seu aceite."
                )
            }
        }
    }
}
