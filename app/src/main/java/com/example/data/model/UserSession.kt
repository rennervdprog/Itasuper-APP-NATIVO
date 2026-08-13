package com.example.data.model

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresAt: Long = 0L,
    val isSessionRestored: Boolean = false,
    val cpfCnpj: String = "",
    val whatsapp: String = "",
    val deliveryPin: String = "",
    val addressStreet: String = "",
    val addressNumber: String = "",
    val addressNeighborhood: String = "",
    val addressCity: String = "",
    val addressCep: String = "",
    val addressState: String = "",
    val addressComplement: String = "",
    val addressReferencePoint: String = "",
    val pixKeyType: String = "",
    val pixKey: String = ""
)
