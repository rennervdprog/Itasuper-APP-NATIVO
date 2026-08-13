package com.example.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.UserSession

/**
 * Armazena somente a sessão autenticada e dados de perfil necessários ao cliente.
 * A senha nunca é persistida. Os valores ficam criptografados com uma chave do Android Keystore.
 */
class SessionStorage(context: Context) {

    private val appContext = context.applicationContext
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(session: UserSession) {
        preferences.edit()
            .putBoolean(KEY_LOGGED_IN, session.isLoggedIn)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.accessTokenExpiresAt)
            .putString(KEY_CPF_CNPJ, session.cpfCnpj)
            .putString(KEY_WHATSAPP, session.whatsapp)
            .putString(KEY_DELIVERY_PIN, session.deliveryPin)
            .putString(KEY_ADDRESS_STREET, session.addressStreet)
            .putString(KEY_ADDRESS_NUMBER, session.addressNumber)
            .putString(KEY_ADDRESS_NEIGHBORHOOD, session.addressNeighborhood)
            .putString(KEY_ADDRESS_CITY, session.addressCity)
            .putString(KEY_ADDRESS_CEP, session.addressCep)
            .putString(KEY_ADDRESS_STATE, session.addressState)
            .putString(KEY_ADDRESS_COMPLEMENT, session.addressComplement)
            .putString(KEY_ADDRESS_REFERENCE, session.addressReferencePoint)
            .putString(KEY_ACTIVE_LOCATION_STREET, session.activeLocationStreet)
            .putString(KEY_ACTIVE_LOCATION_NUMBER, session.activeLocationNumber)
            .putString(KEY_ACTIVE_LOCATION_NEIGHBORHOOD, session.activeLocationNeighborhood)
            .putString(KEY_ACTIVE_LOCATION_CITY, session.activeLocationCity)
            .putString(KEY_ACTIVE_LOCATION_STATE, session.activeLocationState)
            .putString(KEY_PIX_KEY_TYPE, session.pixKeyType)
            .putString(KEY_PIX_KEY, session.pixKey)
            .apply()
    }

    fun read(): UserSession? {
        if (!preferences.getBoolean(KEY_LOGGED_IN, false)) return null
        val userId = preferences.getString(KEY_USER_ID, "").orEmpty()
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        if (userId.isBlank() || refreshToken.isBlank()) return null
        return UserSession(
            isLoggedIn = true,
            userId = userId,
            name = preferences.getString(KEY_NAME, "").orEmpty(),
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
            accessToken = preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty(),
            refreshToken = refreshToken,
            accessTokenExpiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L),
            cpfCnpj = preferences.getString(KEY_CPF_CNPJ, "").orEmpty(),
            whatsapp = preferences.getString(KEY_WHATSAPP, "").orEmpty(),
            deliveryPin = preferences.getString(KEY_DELIVERY_PIN, "").orEmpty(),
            addressStreet = preferences.getString(KEY_ADDRESS_STREET, "").orEmpty(),
            addressNumber = preferences.getString(KEY_ADDRESS_NUMBER, "").orEmpty(),
            addressNeighborhood = preferences.getString(KEY_ADDRESS_NEIGHBORHOOD, "").orEmpty(),
            addressCity = preferences.getString(KEY_ADDRESS_CITY, "").orEmpty(),
            addressCep = preferences.getString(KEY_ADDRESS_CEP, "").orEmpty(),
            addressState = preferences.getString(KEY_ADDRESS_STATE, "").orEmpty(),
            addressComplement = preferences.getString(KEY_ADDRESS_COMPLEMENT, "").orEmpty(),
            addressReferencePoint = preferences.getString(KEY_ADDRESS_REFERENCE, "").orEmpty(),
            activeLocationStreet = preferences.getString(KEY_ACTIVE_LOCATION_STREET, "").orEmpty(),
            activeLocationNumber = preferences.getString(KEY_ACTIVE_LOCATION_NUMBER, "").orEmpty(),
            activeLocationNeighborhood = preferences.getString(KEY_ACTIVE_LOCATION_NEIGHBORHOOD, "").orEmpty(),
            activeLocationCity = preferences.getString(KEY_ACTIVE_LOCATION_CITY, "").orEmpty(),
            activeLocationState = preferences.getString(KEY_ACTIVE_LOCATION_STATE, "").orEmpty(),
            pixKeyType = preferences.getString(KEY_PIX_KEY_TYPE, "").orEmpty(),
            pixKey = preferences.getString(KEY_PIX_KEY, "").orEmpty(),
            isSessionRestored = false
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "itasuper_secure_session"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CPF_CNPJ = "cpf_cnpj"
        const val KEY_WHATSAPP = "whatsapp"
        const val KEY_DELIVERY_PIN = "delivery_pin"
        const val KEY_ADDRESS_STREET = "address_street"
        const val KEY_ADDRESS_NUMBER = "address_number"
        const val KEY_ADDRESS_NEIGHBORHOOD = "address_neighborhood"
        const val KEY_ADDRESS_CITY = "address_city"
        const val KEY_ADDRESS_CEP = "address_cep"
        const val KEY_ADDRESS_STATE = "address_state"
        const val KEY_ADDRESS_COMPLEMENT = "address_complement"
        const val KEY_ADDRESS_REFERENCE = "address_reference"
        const val KEY_ACTIVE_LOCATION_STREET = "active_location_street"
        const val KEY_ACTIVE_LOCATION_NUMBER = "active_location_number"
        const val KEY_ACTIVE_LOCATION_NEIGHBORHOOD = "active_location_neighborhood"
        const val KEY_ACTIVE_LOCATION_CITY = "active_location_city"
        const val KEY_ACTIVE_LOCATION_STATE = "active_location_state"
        const val KEY_PIX_KEY_TYPE = "pix_key_type"
        const val KEY_PIX_KEY = "pix_key"
    }
}
