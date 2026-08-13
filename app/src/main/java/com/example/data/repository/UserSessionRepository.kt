package com.example.data.repository

import com.example.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSessionRepository {

    private val _userSession = MutableStateFlow(UserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    fun login(email: String, userId: String?, accessToken: String?) {
        _userSession.value = _userSession.value.copy(
            isLoggedIn = true,
            userId = userId ?: _userSession.value.userId,
            accessToken = accessToken ?: _userSession.value.accessToken,
            email = email,
            name = if (_userSession.value.name.isBlank()) "Cliente ItaSuper" else _userSession.value.name
        )
    }

    fun setUserId(userId: String) {
        _userSession.value = _userSession.value.copy(userId = userId)
    }

    fun setAccessToken(accessToken: String?) {
        if (!accessToken.isNullOrBlank()) {
            _userSession.value = _userSession.value.copy(accessToken = accessToken)
        }
    }

    fun register(
        name: String,
        email: String,
        cpfCnpj: String,
        whatsapp: String,
        pin: String,
        userId: String,
        accessToken: String?
    ) {
        _userSession.value = UserSession(
            isLoggedIn = true,
            userId = userId,
            accessToken = accessToken.orEmpty(),
            name = name,
            email = email,
            cpfCnpj = cpfCnpj,
            whatsapp = whatsapp,
            deliveryPin = pin
        )
    }

    fun logout() {
        _userSession.value = UserSession()
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
    }

    fun updatePin(newPin: String) {
        _userSession.value = _userSession.value.copy(deliveryPin = newPin)
    }
}
