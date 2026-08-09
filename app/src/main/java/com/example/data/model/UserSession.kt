package com.example.data.model

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val cpfCnpj: String = "",
    val whatsapp: String = "",
    val deliveryPin: String = "",
    val addressStreet: String = "Rua Central",
    val addressNumber: String = "100",
    val addressNeighborhood: String = "Centro",
    val addressCity: String = "Itaguaí",
    val addressCep: String = "23810-000",
    val pixKeyType: String = "CPF",
    val pixKey: String = ""
)
