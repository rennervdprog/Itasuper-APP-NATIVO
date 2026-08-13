package com.example.data.repository

import com.example.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSessionRepository {

    private val _userSession = MutableStateFlow(UserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    fun setUserId(userId: String) {
        _userSession.value = _userSession.value.copy(userId = userId)
    }

    fun login(email: String) {
        _userSession.value = _userSession.value.copy(
            isLoggedIn = true,
            email = email,
            name = if (_userSession.value.name.isBlank()) "Cliente ItaSuper" else _userSession.value.name
        )
    }

    fun register(
        name: String,
        email: String,
        cpfCnpj: String,
        whatsapp: String,
        pin: String
    ) {
        _userSession.value = UserSession(
            isLoggedIn = true,
            userId = "user_" + System.currentTimeMillis(),
            name = name,
            email = email,
            cpfCnpj = cpfCnpj,
            whatsapp = whatsapp,
            deliveryPin = pin
        )
    }

    fun logout() {
        _userSession.value = _userSession.value.copy(isLoggedIn = false)
    }

    fun updateProfile(
        name: String,
        whatsapp: String,
        street: String,
        number: String,
        neighborhood: String,
        cep: String,
        pixKeyType: String,
        pixKey: String
    ) {
        _userSession.value = _userSession.value.copy(
            name = name,
            whatsapp = whatsapp,
            addressStreet = street,
            addressNumber = number,
            addressNeighborhood = neighborhood,
            addressCep = cep,
            pixKeyType = pixKeyType,
            pixKey = pixKey
        )
    }

    fun updatePin(newPin: String) {
        _userSession.value = _userSession.value.copy(deliveryPin = newPin)
    }
}
