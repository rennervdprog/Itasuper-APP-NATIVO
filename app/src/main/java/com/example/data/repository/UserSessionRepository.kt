package com.example.data.repository

import android.content.Context
import com.example.data.model.UserSession
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UserSessionRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var storage: SessionStorage? = null
    private val _userSession = MutableStateFlow(UserSession(isSessionRestored = false))
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    /** Inicializa uma vez no início do app e restaura a conta sem solicitar senha novamente. */
    fun initialize(context: Context) {
        if (storage != null) return
        storage = SessionStorage(context)
        val restored = storage?.read()
        if (restored == null) {
            _userSession.value = UserSession(isSessionRestored = true)
            return
        }

        _userSession.value = restored.copy(isSessionRestored = true)
        refreshSession()
    }

    fun login(
        email: String,
        userId: String?,
        accessToken: String?,
        refreshToken: String?,
        expiresAtSeconds: Long?
    ) {
        _userSession.value = _userSession.value.copy(
            isLoggedIn = true,
            isSessionRestored = true,
            userId = userId ?: _userSession.value.userId,
            accessToken = accessToken ?: _userSession.value.accessToken,
            refreshToken = refreshToken ?: _userSession.value.refreshToken,
            accessTokenExpiresAt = expiresAtSeconds?.times(1000L) ?: _userSession.value.accessTokenExpiresAt,
            email = email,
            name = if (_userSession.value.name.isBlank()) "Cliente ItaSuper" else _userSession.value.name
        )
        persistCurrentSession()
    }

    fun setUserId(userId: String) {
        _userSession.value = _userSession.value.copy(userId = userId)
        persistCurrentSession()
    }

    fun setAccessToken(accessToken: String?) {
        if (!accessToken.isNullOrBlank()) {
            _userSession.value = _userSession.value.copy(accessToken = accessToken)
            persistCurrentSession()
        }
    }

    fun register(
        name: String,
        email: String,
        cpfCnpj: String,
        whatsapp: String,
        pin: String,
        userId: String,
        accessToken: String?,
        refreshToken: String?,
        expiresAtSeconds: Long?
    ) {
        _userSession.value = UserSession(
            isLoggedIn = true,
            isSessionRestored = true,
            userId = userId,
            accessToken = accessToken.orEmpty(),
            refreshToken = refreshToken.orEmpty(),
            accessTokenExpiresAt = expiresAtSeconds?.times(1000L) ?: 0L,
            name = name,
            email = email,
            cpfCnpj = cpfCnpj,
            whatsapp = whatsapp,
            deliveryPin = pin
        )
        persistCurrentSession()
    }

    /**
     * Renova a sessão sem interromper o cliente quando ele está temporariamente sem rede.
     * A sessão só é apagada pela ação explícita de sair.
     */
    fun refreshSession() {
        val current = _userSession.value
        if (!current.isLoggedIn || current.refreshToken.isBlank()) return
        repositoryScope.launch {
            val result = SupabaseClient.refreshSession(current.refreshToken)
            if (result.isSuccess && !result.accessToken.isNullOrBlank()) {
                val updated = _userSession.value.copy(
                    isLoggedIn = true,
                    isSessionRestored = true,
                    userId = result.userId?.takeIf { it.isNotBlank() } ?: _userSession.value.userId,
                    email = result.email?.takeIf { it.isNotBlank() } ?: _userSession.value.email,
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken?.takeIf { it.isNotBlank() } ?: _userSession.value.refreshToken,
                    accessTokenExpiresAt = result.expiresAt?.times(1000L) ?: _userSession.value.accessTokenExpiresAt
                )
                _userSession.value = updated
                persistCurrentSession()
            }
        }
    }

    fun logout() {
        _userSession.value = UserSession(isSessionRestored = true)
        storage?.clear()
    }

    fun updateProfile(
        name: String,
        whatsapp: String,
        street: String,
        number: String,
        neighborhood: String,
        cep: String,
        pixKeyType: String,
        pixKey: String,
        city: String = _userSession.value.addressCity,
        state: String = _userSession.value.addressState,
        complement: String = _userSession.value.addressComplement,
        referencePoint: String = _userSession.value.addressReferencePoint
    ) {
        _userSession.value = _userSession.value.copy(
            name = name,
            whatsapp = whatsapp,
            addressStreet = street,
            addressNumber = number,
            addressNeighborhood = neighborhood,
            addressCity = city,
            addressState = state,
            addressCep = cep,
            addressComplement = complement,
            addressReferencePoint = referencePoint,
            pixKeyType = pixKeyType,
            pixKey = pixKey
        )
        persistCurrentSession()
    }

    fun updatePin(newPin: String) {
        _userSession.value = _userSession.value.copy(deliveryPin = newPin)
        persistCurrentSession()
    }

    private fun persistCurrentSession() {
        val current = _userSession.value
        if (current.isLoggedIn && current.refreshToken.isNotBlank()) {
            storage?.save(current)
        }
    }
}
