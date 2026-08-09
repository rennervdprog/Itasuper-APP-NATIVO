package com.example.data.repository

import com.example.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSessionRepository {

    private val _userSession = MutableStateFlow(
        UserSession(
            isLoggedIn = true,
            userId = "user_demo_1",
            name = "Cliente ItaSuper",
            email = "cliente@itasuper.com.br",
            cpfCnpj = "123.456.789-00",
            whatsapp = "(21) 99999-8888",
            deliveryPin = "1234"
        )
    )
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

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
