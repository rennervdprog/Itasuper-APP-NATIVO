package com.example.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PendingLegalChanges
import com.example.data.model.UserSession
import com.example.data.remote.SupabaseClient
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
    val errorMessage: String? = null
) {
    val requiresAcceptance: Boolean
        get() = pending?.requiresAcceptance == true

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
            _uiState.value = if (pending?.requiresAcceptance == true) {
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

    fun setTermsAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedTerms = accepted, errorMessage = null)
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedPrivacy = accepted, errorMessage = null)
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
                privacyVersion = pending.currentPrivacyVersion
            )
            if (result.success) {
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
