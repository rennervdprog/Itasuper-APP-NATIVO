package com.example.data.remote

import android.util.Log
import com.example.data.model.AddonGroup
import com.example.data.model.AddonItem
import com.example.data.model.MenuSection
import com.example.data.model.Product
import com.example.data.model.PizzaLegacySize
import com.example.data.model.PizzaSizeCatalogItem
import com.example.data.model.SavedAddress
import com.example.data.model.Store
import com.example.data.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SupabaseAuthResponse(
    val isSuccess: Boolean,
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val email: String? = null,
    val errorMessage: String? = null
)

data class RemoteCustomerProfile(
    val fullName: String = "",
    val email: String = "",
    val document: String = "",
    val whatsapp: String = "",
    val deliveryPin: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val referencePoint: String = "",
    val pixKeyType: String = "",
    val pixKey: String = ""
)

data class OrderSubmissionResponse(
    val isSuccess: Boolean,
    val orderId: String? = null,
    val createdAt: String? = null,
    val errorMessage: String? = null
)

data class PixPaymentResponse(
    val isSuccess: Boolean,
    val pixCode: String? = null,
    val qrCodeBase64: String? = null,
    val errorMessage: String? = null
)

object SupabaseClient {

    private const val TAG = "SupabaseClient"

    const val SUPABASE_URL = "https://qkjhguziuchqsbxzruea.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFramhndXppdWNocXNieHpydWVhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzUwNDg4NTUsImV4cCI6MjA5MDYyNDg1NX0.2sTeKchqAEN2gCqnH1_Zn9cJmUSmZgryt05A66tgm2Y"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. SIGN IN
    suspend fun signIn(email: String, password: String): SupabaseAuthResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/auth/v1/token?grant_type=password"
            val bodyJson = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                val accessToken = json.optNullableString("access_token")
                val refreshToken = json.optNullableString("refresh_token")
                val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: json.optString("id")
                val userEmail = userObj?.optString("email") ?: email

                SupabaseAuthResponse(
                    isSuccess = true,
                    userId = userId,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = expiresAt,
                    email = userEmail
                )
            } else {
                val errorMsg = parseErrorMessage(responseText, "E-mail ou senha incorretos")
                SupabaseAuthResponse(
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during signIn", e)
            SupabaseAuthResponse(
                isSuccess = false,
                errorMessage = "Falha na conexão com o servidor: ${e.localizedMessage}"
            )
        }
    }

    // 2. SIGN UP
    suspend fun signUp(email: String, password: String): SupabaseAuthResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/auth/v1/signup"
            val bodyJson = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                val accessToken = json.optNullableString("access_token")
                val refreshToken = json.optNullableString("refresh_token")
                val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: json.optString("id")
                val userEmail = userObj?.optString("email") ?: email

                SupabaseAuthResponse(
                    isSuccess = true,
                    userId = userId,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = expiresAt,
                    email = userEmail
                )
            } else {
                val errorMsg = parseErrorMessage(responseText, "Erro ao criar conta")
                SupabaseAuthResponse(
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during signUp", e)
            SupabaseAuthResponse(
                isSuccess = false,
                errorMessage = "Falha de conexão: ${e.localizedMessage}"
            )
        }
    }

    // 2b. REFRESH SESSION
    suspend fun refreshSession(refreshToken: String): SupabaseAuthResponse = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/auth/v1/token?grant_type=refresh_token"
            val bodyJson = JSONObject().apply { put("refresh_token", refreshToken) }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""
            if (response.isSuccessful && responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                val accessToken = json.optNullableString("access_token")
                val nextRefreshToken = json.optNullableString("refresh_token")
                val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
                val userObj = json.optJSONObject("user")
                SupabaseAuthResponse(
                    isSuccess = !accessToken.isNullOrBlank(),
                    userId = userObj?.optString("id"),
                    accessToken = accessToken,
                    refreshToken = nextRefreshToken,
                    expiresAt = expiresAt,
                    email = userObj?.optString("email")
                )
            } else {
                SupabaseAuthResponse(
                    isSuccess = false,
                    errorMessage = parseErrorMessage(responseText, "Sua sessão expirou. Entre novamente.")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing Supabase session", e)
            SupabaseAuthResponse(isSuccess = false, errorMessage = "Falha ao renovar sessão")
        }
    }

    /** Lê o perfil do cliente autenticado — mesma fonte usada pelas telas do Capacitor. */
    suspend fun fetchCustomerProfile(userId: String, accessToken: String): RemoteCustomerProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || accessToken.isBlank()) return@withContext null
        try {
            val select = listOf(
                "full_name", "email", "document", "whatsapp_number", "phone", "delivery_pin",
                "cep", "street", "number", "complement", "neighborhood",
                "city", "reference_point", "pix_type", "pix_key"
            ).joinToString(",")
            val url = "$SUPABASE_URL/rest/v1/profiles?select=$select&user_id=eq.$userId&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""
            if (!response.isSuccessful || responseText.isBlank()) {
                Log.w(TAG, "Perfil não pôde ser sincronizado: code=${response.code}")
                return@withContext null
            }
            val profile = JSONArray(responseText).optJSONObject(0) ?: return@withContext null
            RemoteCustomerProfile(
                fullName = profile.optNullableString("full_name").orEmpty().trim(),
                email = profile.optNullableString("email").orEmpty().trim(),
                document = profile.optNullableString("document").orEmpty().trim(),
                whatsapp = profile.optNullableString("whatsapp_number")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.optNullableString("phone").orEmpty(),
                deliveryPin = profile.optNullableString("delivery_pin").orEmpty().trim(),
                cep = profile.optNullableString("cep").orEmpty().trim(),
                street = profile.optNullableString("street").orEmpty().trim(),
                number = profile.optNullableString("number").orEmpty(),
                complement = profile.optNullableString("complement").orEmpty().trim(),
                neighborhood = profile.optNullableString("neighborhood").orEmpty().trim(),
                city = profile.optNullableString("city").orEmpty().trim(),
                state = profile.optNullableString("state").orEmpty().trim(),
                referencePoint = profile.optNullableString("reference_point").orEmpty().trim(),
                pixKeyType = profile.optNullableString("pix_type").orEmpty().trim(),
                pixKey = profile.optNullableString("pix_key").orEmpty().trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar perfil do cliente", e)
            null
        }
    }

    /** Endereços salvos do cliente, na mesma tabela usada pelo seletor do Capacitor. */
    suspend fun fetchSavedAddresses(userId: String, accessToken: String): List<SavedAddress> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || accessToken.isBlank()) return@withContext emptyList()
        try {
            val select = "id,label,street,number,complement,neighborhood,reference_point,is_default,cep,latitude,longitude,pin_confirmed"
            val url = "$SUPABASE_URL/rest/v1/saved_addresses?select=$select&user_id=eq.$userId&order=is_default.desc,created_at.desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "Endereços salvos indisponíveis: code=${response.code}")
                return@withContext emptyList()
            }
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optNullableString("id").orEmpty()
                    if (id.isBlank()) continue
                    add(
                        SavedAddress(
                            id = id,
                            label = item.optNullableString("label").orEmpty().ifBlank { "Casa" },
                            street = item.optNullableString("street").orEmpty(),
                            number = item.optNullableString("number").orEmpty(),
                            complement = item.optNullableString("complement").orEmpty(),
                            neighborhood = item.optNullableString("neighborhood").orEmpty(),
                            referencePoint = item.optNullableString("reference_point").orEmpty(),
                            cep = item.optNullableString("cep").orEmpty(),
                            latitude = item.takeIf { it.has("latitude") && !it.isNull("latitude") }?.optDouble("latitude"),
                            longitude = item.takeIf { it.has("longitude") && !it.isNull("longitude") }?.optDouble("longitude"),
                            pinConfirmed = item.optBoolean("pin_confirmed", false),
                            isDefault = item.optBoolean("is_default", false)
                        )
                    )
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao buscar endereços salvos", error)
            emptyList()
        }
    }

    suspend fun createSavedAddress(userId: String, accessToken: String, address: SavedAddress): SavedAddress? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || accessToken.isBlank() || address.street.isBlank() || address.number.isBlank()) return@withContext null
        try {
            val existing = fetchSavedAddresses(userId, accessToken)
            val makeDefault = address.isDefault || existing.isEmpty()
            if (makeDefault && existing.isNotEmpty()) {
                val clearRequest = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/saved_addresses?user_id=eq.$userId")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .patch(JSONObject().put("is_default", false).toString().toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(clearRequest).execute().use { if (!it.isSuccessful) return@withContext null }
            }
            val body = JSONObject().apply {
                put("user_id", userId)
                put("label", address.label.ifBlank { "Casa" })
                put("street", address.street)
                put("number", address.number)
                put("complement", address.complement)
                put("neighborhood", address.neighborhood)
                put("reference_point", address.referencePoint)
                put("cep", address.cep)
                address.latitude?.let { put("latitude", it) }
                address.longitude?.let { put("longitude", it) }
                put("pin_confirmed", address.pinConfirmed)
                put("is_default", makeDefault)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/saved_addresses")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@withContext null
            val item = JSONArray(responseBody).optJSONObject(0) ?: return@withContext null
            SavedAddress(
                id = item.optNullableString("id").orEmpty(),
                label = item.optNullableString("label").orEmpty().ifBlank { "Casa" },
                street = item.optNullableString("street").orEmpty(),
                number = item.optNullableString("number").orEmpty(),
                complement = item.optNullableString("complement").orEmpty(),
                neighborhood = item.optNullableString("neighborhood").orEmpty(),
                referencePoint = item.optNullableString("reference_point").orEmpty(),
                cep = item.optNullableString("cep").orEmpty(),
                latitude = item.takeIf { it.has("latitude") && !it.isNull("latitude") }?.optDouble("latitude"),
                longitude = item.takeIf { it.has("longitude") && !it.isNull("longitude") }?.optDouble("longitude"),
                pinConfirmed = item.optBoolean("pin_confirmed", false),
                isDefault = item.optBoolean("is_default", false)
            )
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao salvar endereço", error)
            null
        }
    }

    suspend fun setDefaultSavedAddress(userId: String, addressId: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || addressId.isBlank() || accessToken.isBlank()) return@withContext false
        try {
            val clearRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/saved_addresses?user_id=eq.$userId")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .patch(JSONObject().put("is_default", false).toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(clearRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
            }
            val defaultRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/saved_addresses?id=eq.$addressId&user_id=eq.$userId")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .patch(JSONObject().put("is_default", true).toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(defaultRequest).execute().use { it.isSuccessful }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao definir endereço padrão", error)
            false
        }
    }

    suspend fun deleteSavedAddress(userId: String, addressId: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || addressId.isBlank() || accessToken.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/saved_addresses?id=eq.$addressId&user_id=eq.$userId")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao excluir endereço salvo", error)
            false
        }
    }

    // 3. INSERT INTO PROFILES
    suspend fun insertProfile(
        userId: String,
        fullName: String,
        document: String,
        whatsappNumber: String,
        email: String,
        deliveryPin: String,
        accessToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles"
            val profileJson = JSONObject().apply {
                put("user_id", userId)
                put("full_name", fullName)
                put("document", document)
                put("whatsapp_number", whatsappNumber)
                put("email", email)
                put("delivery_pin", deliveryPin)
            }

            val bearer = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $SUPABASE_ANON_KEY"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(profileJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful || response.code == 201 || response.code == 200 || response.code == 204
            if (!isSuccess) {
                Log.e(TAG, "Failed to insert profile: code=${response.code}, body=${response.body?.string()}")
            }
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting profile", e)
            false
        }
    }

    // 3b. UPDATE PROFILE NUMBER/ADDRESS IN SUPABASE
    suspend fun updateUserProfileNumber(
        userId: String,
        accessToken: String,
        number: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
            val bodyJson = JSONObject().put("number", number)
            val bearer = if (accessToken.isNotBlank()) accessToken else SUPABASE_ANON_KEY

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful || response.code == 204 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile number", e)
            false
        }
    }

    suspend fun updateUserProfileAddress(
        userId: String,
        accessToken: String,
        cep: String,
        street: String,
        number: String,
        complement: String,
        neighborhood: String,
        city: String,
        referencePoint: String,
        whatsapp: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
            val bodyJson = JSONObject().apply {
                put("cep", cep)
                put("street", street)
                put("number", number)
                put("complement", complement)
                put("neighborhood", neighborhood)
                put("city", city)
                put("reference_point", referencePoint)
                put("whatsapp_number", whatsapp)
                put("phone", whatsapp)
            }
            val bearer = if (accessToken.isNotBlank()) accessToken else SUPABASE_ANON_KEY
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful || response.code == 204 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user address", e)
            false
        }
    }

    suspend fun updateCustomerPersonalProfile(
        userId: String,
        accessToken: String,
        fullName: String,
        document: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
            val bodyJson = JSONObject().apply {
                put("full_name", fullName)
                put("document", document)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful || response.code == 200 || response.code == 204
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar dados pessoais", e)
            false
        }
    }

    suspend fun updateCustomerDeliveryPin(
        userId: String,
        accessToken: String,
        pin: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
            val bodyJson = JSONObject().put("delivery_pin", pin)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful || response.code == 200 || response.code == 204
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar PIN de entrega", e)
            false
        }
    }

    /** Retorna nulo ao excluir com sucesso ou uma mensagem adequada para exibição ao cliente. */
    suspend fun deleteCustomerAccount(accessToken: String, reason: String): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext "Sua sessão expirou. Entre novamente para continuar."
        try {
            val url = "$SUPABASE_URL/functions/v1/delete-account"
            val bodyJson = JSONObject().put("reason", reason.ifBlank { "Solicitação do usuário" })
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                null
            } else {
                JSONObject(responseText).optNullableString("error")
                    ?: "Não foi possível excluir sua conta. Tente novamente."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir conta do cliente", e)
            "Não foi possível excluir sua conta. Verifique sua conexão e tente novamente."
        }
    }

    data class OpeningHour(
        val storeId: String,
        val dayOfWeek: Int,
        val dayOfWeekStr: String = "",
        val openTime: String,
        val closeTime: String,
        val isClosedAllDay: Boolean
    )

    suspend fun fetchOpeningHours(): List<OpeningHour> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/opening_hours?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val list = mutableListOf<OpeningHour>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val storeId = item.optString("store_id", "")
                    val dayOfWeekVal = item.optInt("day_of_week", -1)
                    val dayOfWeekStr = item.optString("day_of_week", "")
                    val openTime = item.optString("open_time", "08:00")
                    val closeTime = item.optString("close_time", "22:00")
                    val isClosedAllDay = item.optBoolean("is_closed_all_day", false)
                    if (storeId.isNotBlank()) {
                        list.add(OpeningHour(storeId, dayOfWeekVal, dayOfWeekStr, openTime, closeTime, isClosedAllDay))
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching opening_hours", e)
            emptyList()
        }
    }

    // 4. FETCH STORES FROM STORES_PUBLIC VIEW
    suspend fun fetchActiveStores(): List<Store> = withContext(Dispatchers.IO) {
        try {
            val openingHours = fetchOpeningHours()

            // Primary query to stores_public view with status=eq.ativo (not filtering is_open in backend)
            var url = "$SUPABASE_URL/rest/v1/stores_public?select=*&status=eq.ativo&order=rating.desc"

            var request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            var response = httpClient.newCall(request).execute()
            var responseText = response.body?.string() ?: ""

            // Fallback if status enum causes Postgres error
            if (!response.isSuccessful) {
                val fallbackUrl = "$SUPABASE_URL/rest/v1/stores_public?select=*&order=rating.desc"
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()
                response = httpClient.newCall(fallbackRequest).execute()
                responseText = response.body?.string() ?: ""
            }

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val storeList = mutableListOf<Store>()
                val ownerWhatsappById = fetchStoreOwnerWhatsapps(
                    (0 until jsonArray.length()).mapNotNull { index ->
                        jsonArray.optJSONObject(index)?.optString("owner_id", "")?.takeIf { it.isNotBlank() }
                    }.distinct()
                )

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    
                    val status = item.optString("status", "ativo")
                    // Filter out inactive or blocked stores
                    if (status.equals("inativo", ignoreCase = true) || status.equals("bloqueado", ignoreCase = true) ||
                        status.equals("desativado", ignoreCase = true) || status.equals("inactive", ignoreCase = true) ||
                        status.equals("blocked", ignoreCase = true) || status.equals("disabled", ignoreCase = true)) {
                        continue
                    }

                    val id = item.optString("id", "")
                    val name = item.optString("name", "Loja ItaSuper")
                    val category = item.optString("category", "Geral")
                    val rating = item.optDouble("rating", 5.0).let { if (it.isNaN()) 5.0 else it }
                    val imageUrl = item.optString("logo_url", item.optString("image_url", item.optString("banner_url", "")))
                    val bannerUrl = item.optString("banner_url", imageUrl)
                    val isForceClosed = item.optBoolean("force_closed", false)
                    val defaultIsOpen = item.optBoolean("is_open", true)

                    val computedIsOpen = checkIsStoreOpenNow(id, isForceClosed, defaultIsOpen, openingHours)

                    val ownFee = item.optDouble("own_delivery_fee", 0.0)
                    val deliveryFeeType = item.optString("delivery_fee_type", "fixed")
                    val deliveryBaseKm = item.optDouble("delivery_base_km", 0.0)
                    val deliveryFeeBase = item.optDouble("delivery_fee_base", ownFee)
                    val deliveryFeePerKm = item.optDouble("delivery_fee_per_km", 0.0)
                    val platformFee = item.optDouble("delivery_fee", 0.0)
                    val deliveryMode = item.optString("delivery_mode", "platform")
                    val platformFeeSplit = item.optString("platform_fee_split", "cliente")
                    val planType = item.optString("plan_type", "")
                    val autonomyLifetimeFree = item.optBoolean("autonomy_lifetime_free", false)
                    val pixDirectEnabled = item.optBoolean("pix_direto_enabled", false)
                    val pixDirectKey = item.optString("pix_direto_key", "").trim()
                    val pixDirectKeyType = item.optString("pix_direto_key_type", "").trim()
                    val pixDirectBeneficiary = item.optString("pix_direto_beneficiary", "").trim()
                    val pixDirectInstructions = item.optString("pix_direto_instructions", "").trim()
                    val splitOverrideRaw = item.opt("platform_delivery_split_override")
                    val platformDeliverySplitOverride = when (splitOverrideRaw) {
                        is Number -> splitOverrideRaw.toDouble()
                        is String -> splitOverrideRaw.toDoubleOrNull()
                        else -> null
                    }
                    val isAutonomy = planType.equals("autonomy", true) || autonomyLifetimeFree
                    val platformSplit = if (isAutonomy) 0.0 else (platformDeliverySplitOverride ?: 0.99)
                    val platformAddToCustomer = when (platformFeeSplit.lowercase()) {
                        "meio_a_meio" -> kotlin.math.round((platformSplit / 2.0) * 100.0) / 100.0
                        "lojista" -> 0.0
                        else -> platformSplit
                    }
                    val rawFee = when (deliveryMode.lowercase()) {
                        "pickup" -> 0.0
                        "platform" -> platformFee
                        "own", "direto" -> ownFee + platformAddToCustomer
                        else -> platformFee
                    }
                    val isFree = deliveryMode.equals("pickup", true) || rawFee <= 0.0
                    val deliveryFeeText = when {
                        deliveryMode.equals("pickup", true) -> "Retirada"
                        rawFee > 0.0 -> String.format("R$ %.2f", rawFee).replace(".", ",")
                        else -> "Grátis"
                    }
                    val rawDeliveryTime = item.optString("estimated_delivery_time", "").trim()
                    val minimumOrder = item.optDouble("minimum_order_value", 0.0)

                    val createdAt = item.optString("created_at", "")
                    val lat = if (item.has("latitude") && !item.isNull("latitude")) item.optDouble("latitude") else null
                    val lng = if (item.has("longitude") && !item.isNull("longitude")) item.optDouble("longitude") else null
                    val addressStreet = item.optString("address_street", "").trim()
                    val addressNumber = item.optString("address_number", "").trim()
                    val addressNeighborhood = item.optString("address_neighborhood", "").trim()
                    val addressCity = item.optString("address_city", "").trim()
                    val addressState = item.optString("address_state", "").trim()
                    val addressCep = item.optString("address_cep", "").trim()
                    val addressReference = item.optString("address_reference", "").trim()
                    val storeAddress = listOf(
                        listOf(addressStreet, addressNumber).filter { it.isNotBlank() }.joinToString(", "),
                        addressNeighborhood,
                        listOf(addressCity, addressState).filter { it.isNotBlank() }.joinToString(" - ")
                    ).filter { it.isNotBlank() }.joinToString(", ")
                    val ownerId = item.optString("owner_id", "").trim()
                    val storeWhatsapp = item.optString("whatsapp", item.optString("phone", "")).trim()
                        .ifBlank { ownerWhatsappById[ownerId].orEmpty() }

                    val storeOpeningHours = openingHours.filter { it.storeId == id }.map { oh ->
                        com.example.data.model.StoreOpeningHour(
                            storeId = oh.storeId,
                            dayOfWeek = oh.dayOfWeek,
                            dayOfWeekStr = oh.dayOfWeekStr,
                            openTime = oh.openTime,
                            closeTime = oh.closeTime,
                            isClosedAllDay = oh.isClosedAllDay
                        )
                    }

                    // Secondary Categories
                    val secCats = mutableListOf<String>()
                    val secArr = item.optJSONArray("secondary_categories") ?: item.optJSONArray("categories")
                    if (secArr != null) {
                        for (j in 0 until secArr.length()) {
                            val catVal = secArr.optString(j, "")
                            if (catVal.isNotBlank() && catVal != "null") {
                                secCats.add(catVal)
                            }
                        }
                    } else {
                        val secStr = item.optString("secondary_categories", item.optString("categories", ""))
                        if (secStr.isNotBlank()) {
                            secCats.addAll(secStr.split(",").map { it.trim() })
                        }
                    }

                    // Settings JSON column
                    var settingsObj = item.optJSONObject("settings")
                    if (settingsObj == null && item.has("settings") && !item.isNull("settings")) {
                        val sStr = item.optString("settings", "")
                        if (sStr.isNotBlank() && sStr.startsWith("{")) {
                            try { settingsObj = JSONObject(sStr) } catch (_: Exception) {}
                        }
                    }

                    val pizzaConfig = settingsObj?.optJSONObject("pizza_config")
                    val pastelConfig = settingsObj?.optJSONObject("pastel_config")

                    val pizzaSizesCatalog = buildList {
                        val sizes = settingsObj?.optJSONArray("pizza_sizes_catalog")
                        if (sizes != null) {
                            for (sizeIndex in 0 until sizes.length()) {
                                val size = sizes.optJSONObject(sizeIndex) ?: continue
                                val id = size.optString("id", "").trim()
                                val name = size.optString("name", "").trim()
                                if (id.isNotBlank() && name.isNotBlank()) {
                                    add(PizzaSizeCatalogItem(
                                        id = id,
                                        name = name,
                                        description = size.optString("description", "").trim(),
                                        maxFlavors = size.optInt("maxFlavors", size.optInt("max_flavors", 4)).coerceIn(1, 4),
                                        active = size.optBoolean("active", true)
                                    ))
                                }
                            }
                        }
                    }
                    val pizzaPriceMatrix = buildMap<String, Map<String, Double>> {
                        val matrix = settingsObj?.optJSONObject("pizza_price_matrix")
                        if (matrix != null) {
                            val categoryKeys = matrix.keys()
                            while (categoryKeys.hasNext()) {
                                val categoryId = categoryKeys.next()
                                val categoryMatrix = matrix.optJSONObject(categoryId) ?: continue
                                val prices = buildMap<String, Double> {
                                    val sizeKeys = categoryMatrix.keys()
                                    while (sizeKeys.hasNext()) {
                                        val sizeId = sizeKeys.next()
                                        val price = categoryMatrix.optDouble(sizeId, 0.0)
                                        if (price > 0.0) put(sizeId, price)
                                    }
                                }
                                if (prices.isNotEmpty()) put(categoryId, prices)
                            }
                        }
                    }
                    val storeSettings = StoreSettings(
                        pizzaHalfEnabled = settingsObj?.optBoolean("pizza_half_enabled", true) ?: true,
                        pastelHalfEnabled = settingsObj?.optBoolean("pastel_half_enabled", true) ?: true,
                        pizzaMaxFlavors = pizzaConfig?.optInt("max_flavors", 4) ?: (settingsObj?.optInt("pizza_max_flavors", 4) ?: 4),
                        pastelMaxFlavors = pastelConfig?.optInt("max_flavors", 4) ?: (settingsObj?.optInt("pastel_max_flavors", 4) ?: 4),
                        pastelMaxComplements = pastelConfig?.optInt("max_complements", 3) ?: (settingsObj?.optInt("pastel_max_complements", 3) ?: 3),
                        pizzaPriceMode = settingsObj?.optString("pizza_price_mode", "maior") ?: "maior",
                        pizzaSizesCatalog = pizzaSizesCatalog,
                        pizzaPriceMatrix = pizzaPriceMatrix,
                        pastelPriceMode = settingsObj?.optString("pastel_price_mode", "maior") ?: "maior",
                        pizzaSingleSize = settingsObj?.optBoolean("pizza_single_size", false) ?: false,
                        pastelSingleSize = settingsObj?.optBoolean("pastel_single_size", false) ?: false,
                        acceptPixOnline = settingsObj?.optBoolean("accept_pix_online", false) ?: false,
                        acceptPixMachine = settingsObj?.optBoolean("accept_pix_machine", false) ?: false,
                        acceptCard = settingsObj?.optBoolean("accept_card", true) ?: true,
                        acceptCash = settingsObj?.optBoolean("accept_cash", true) ?: true
                    )

                    val deliveryTime = formatDeliveryTime(rawDeliveryTime, settingsObj)

                    val store = Store(
                        id = id,
                        name = name,
                        category = category,
                        secondaryCategories = secCats,
                        rating = rating,
                        deliveryTime = deliveryTime,
                        deliveryFee = deliveryFeeText,
                        isFreeDelivery = isFree,
                        isOpen = computedIsOpen,
                        forceClosed = isForceClosed,
                        distanceKm = null,
                        logoUrl = imageUrl,
                        bannerUrl = bannerUrl,
                        minOrder = minimumOrder,
                        createdAt = createdAt,
                        slug = item.optString("slug", ""),
                        status = status,
                        latitude = lat,
                        longitude = lng,
                        settings = storeSettings,
                        deliveryMode = deliveryMode,
                        ownDeliveryFee = ownFee,
                        deliveryFeeType = deliveryFeeType,
                        deliveryBaseKm = deliveryBaseKm,
                        deliveryFeeBase = deliveryFeeBase,
                        deliveryFeePerKm = deliveryFeePerKm,
                        platformDeliveryFee = platformFee,
                        platformFeeSplit = platformFeeSplit,
                        planType = planType,
                        platformDeliverySplitOverride = platformDeliverySplitOverride,
                        autonomyLifetimeFree = autonomyLifetimeFree,
                        pixDirectEnabled = pixDirectEnabled,
                        pixDirectKey = pixDirectKey,
                        pixDirectKeyType = pixDirectKeyType,
                        pixDirectBeneficiary = pixDirectBeneficiary,
                        pixDirectInstructions = pixDirectInstructions,
                        addressStreet = addressStreet,
                        addressNumber = addressNumber,
                        addressNeighborhood = addressNeighborhood,
                        addressCity = addressCity,
                        addressState = addressState,
                        addressCep = addressCep,
                        addressReference = addressReference,
                        address = storeAddress,
                        whatsapp = storeWhatsapp,
                        openingHours = storeOpeningHours
                    )
                    storeList.add(store)
                }

                // Mesma regra de vitrine do Capacitor: lojas exclusivas de PDV não recebem
                // pedidos de delivery. Quando a RPC responder, só ficam as lojas com pelo
                // menos um entregador online vinculado. Em falha temporária da RPC, preserva
                // o catálogo elegível para não esvaziar a Home por erro de rede.
                val deliveryEligibleStores = storeList.filterNot { it.planType.equals("pdv_only", ignoreCase = true) }
                val onlineDriverStoreIds = fetchStoreIdsWithOnlineDrivers()
                if (onlineDriverStoreIds == null) {
                    deliveryEligibleStores
                } else {
                    deliveryEligibleStores.filter { it.id in onlineDriverStoreIds }
                }
            } else {
                Log.e(TAG, "Failed fetching stores_public: code=${response.code}, body=$responseText")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stores_public", e)
            emptyList()
        }
    }

    /**
     * Retorna os IDs de lojas com pelo menos um entregador online, usando a mesma RPC
     * pública da vitrine Capacitor. `null` significa indisponibilidade temporária da RPC
     * e permite que o chamador mantenha o catálogo para não produzir uma Home vazia.
     */
    private fun fetchStoreIdsWithOnlineDrivers(): Set<String>? {
        return try {
            val payload = "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/stores_with_online_drivers")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload)
                .build()
            val response = httpClient.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful || text.isBlank()) {
                Log.w(TAG, "RPC stores_with_online_drivers indisponível: HTTP ${response.code}")
                null
            } else {
                val array = JSONArray(text)
                buildSet {
                    for (index in 0 until array.length()) {
                        array.optString(index, "").trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível filtrar lojas por entregador online", e)
            null
        }
    }

    /** Busca contatos públicos dos proprietários sem criar valores substitutos. */
    private fun fetchStoreOwnerWhatsapps(ownerIds: List<String>): Map<String, String> {
        if (ownerIds.isEmpty()) return emptyMap()
        return try {
            val ownerFilter = ownerIds.joinToString(",")
            val url = "$SUPABASE_URL/rest/v1/profiles?select=user_id,whatsapp_number&user_id=in.($ownerFilter)"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful || responseText.isBlank()) return emptyMap()

            val profiles = JSONArray(responseText)
            buildMap {
                for (index in 0 until profiles.length()) {
                    val profile = profiles.optJSONObject(index) ?: continue
                    val userId = profile.optString("user_id", "").trim()
                    val whatsapp = profile.optString("whatsapp_number", "").trim()
                    if (userId.isNotBlank() && whatsapp.isNotBlank()) put(userId, whatsapp)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Store owner WhatsApp unavailable", e)
            emptyMap()
        }
    }

    /**
     * Carrega os metadados reais da loja selecionada. O mapper de lista já preserva
     * todos os campos de stores_public; esta função garante o mesmo resultado em
     * acessos diretos ao detalhe da loja, quando a Home ainda não foi carregada.
     */
    suspend fun fetchStoreById(storeId: String): Store? = withContext(Dispatchers.IO) {
        if (storeId.isBlank()) return@withContext null
        try {
            // Reutiliza o mapper central para manter os campos e as regras do card
            // de loja idênticos entre Home, Busca e Detalhe.
            fetchActiveStores().firstOrNull { it.id == storeId }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching store $storeId", e)
            null
        }
    }

    private fun formatDeliveryTime(raw: String, settings: JSONObject?): String {
        val normalized = raw.trim()
        val hasValidRawValue = normalized.isNotBlank() &&
            !normalized.equals("null", ignoreCase = true) &&
            !normalized.equals("undefined", ignoreCase = true) &&
            normalized != "-"
        if (hasValidRawValue) {
            return if (normalized.contains("min", true)) normalized else "$normalized min"
        }
        val min = settings?.optDouble("delivery_time_min", Double.NaN) ?: Double.NaN
        val max = settings?.optDouble("delivery_time_max", Double.NaN) ?: Double.NaN
        return if (min.isFinite() && max.isFinite() && min > 0 && max > 0) {
            "${min.toInt()}-${max.toInt()} min"
        } else {
            ""
        }
    }

    private fun checkIsStoreOpenNow(
        storeId: String,
        isForceClosed: Boolean,
        defaultIsOpen: Boolean,
        openingHours: List<OpeningHour>
    ): Boolean {
        if (isForceClosed) return false

        val storeHours = openingHours.filter { it.storeId == storeId }
        if (storeHours.isEmpty()) return defaultIsOpen

        val calendar = java.util.Calendar.getInstance()
        val sysDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)

        val todayHour = storeHours.find { hour ->
            when {
                hour.dayOfWeek == sysDay -> true
                hour.dayOfWeek == (sysDay - 1) -> true
                hour.dayOfWeekStr.equals("domingo", true) && sysDay == java.util.Calendar.SUNDAY -> true
                hour.dayOfWeekStr.equals("segunda", true) && sysDay == java.util.Calendar.MONDAY -> true
                hour.dayOfWeekStr.equals("terca", true) && sysDay == java.util.Calendar.TUESDAY -> true
                hour.dayOfWeekStr.equals("terça", true) && sysDay == java.util.Calendar.TUESDAY -> true
                hour.dayOfWeekStr.equals("quarta", true) && sysDay == java.util.Calendar.WEDNESDAY -> true
                hour.dayOfWeekStr.equals("quinta", true) && sysDay == java.util.Calendar.THURSDAY -> true
                hour.dayOfWeekStr.equals("sexta", true) && sysDay == java.util.Calendar.FRIDAY -> true
                hour.dayOfWeekStr.equals("sabado", true) && sysDay == java.util.Calendar.SATURDAY -> true
                hour.dayOfWeekStr.equals("sábado", true) && sysDay == java.util.Calendar.SATURDAY -> true
                else -> false
            }
        } ?: storeHours.firstOrNull()

        if (todayHour == null) return defaultIsOpen
        if (todayHour.isClosedAllDay) return false

        fun parseMinutes(timeStr: String): Int {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return h * 60 + m
            }
            return 0
        }

        val openMin = parseMinutes(todayHour.openTime)
        val closeMin = parseMinutes(todayHour.closeTime)

        if (openMin == closeMin && openMin == 0) return true

        return if (closeMin > openMin) {
            currentMinutes in openMin..closeMin
        } else {
            currentMinutes >= openMin || currentMinutes <= closeMin
        }
    }

    // 4b. FETCH PROMO BANNERS FROM SUPABASE
    suspend fun fetchBanners(): List<com.example.data.model.Banner> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/banners?select=*&is_active=eq.true"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            var response = httpClient.newCall(request).execute()
            var responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val fallbackUrl = "$SUPABASE_URL/rest/v1/banners?select=*"
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()
                response = httpClient.newCall(fallbackRequest).execute()
                responseText = response.body?.string() ?: ""
            }

            if (response.isSuccessful && responseText.isNotBlank()) {
                val array = JSONArray(responseText)
                val bannerList = mutableListOf<com.example.data.model.Banner>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", obj.optString("name", "Oferta Especial"))
                    val imageUrl = obj.optString("image_url", obj.optString("imageUrl", ""))
                    val targetStoreId = obj.optNullableString("target_store_id") ?: obj.optNullableString("store_id")
                    val desc = obj.optString("description", "")

                    if (imageUrl.isNotBlank() || title.isNotBlank()) {
                        bannerList.add(
                            com.example.data.model.Banner(
                                id = id,
                                title = title,
                                imageUrl = imageUrl,
                                targetStoreId = if (targetStoreId == "null") null else targetStoreId,
                                description = desc
                            )
                        )
                    }
                }
                bannerList
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching banners", e)
            emptyList()
        }
    }

    // 4c. FETCH MENU SECTIONS FOR STORE
    suspend fun fetchMenuSectionsForStore(storeId: String): List<MenuSection> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/menu_sections?store_id=eq.$storeId&order=sort_order.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            var response = httpClient.newCall(request).execute()
            var responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val fallbackUrl = "$SUPABASE_URL/rest/v1/menu_sections?select=*"
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()
                response = httpClient.newCall(fallbackRequest).execute()
                responseText = response.body?.string() ?: ""
            }

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val sectionsList = mutableListOf<MenuSection>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    val sortOrder = item.optInt("sort_order", 0)

                    if (id.isNotBlank() && name.isNotBlank()) {
                        sectionsList.add(
                            MenuSection(
                                id = id,
                                storeId = storeId,
                                name = name,
                                sortOrder = sortOrder
                            )
                        )
                    }
                }
                sectionsList.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching menu_sections for store $storeId", e)
            emptyList()
        }
    }

    // 5. FETCH PRODUCTS FOR STORE
    suspend fun fetchProductsForStore(storeId: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/products?select=*&store_id=eq.$storeId&order=name.asc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val productsList = mutableListOf<Product>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    
                    // Filter out empty/blank name
                    if (name.isBlank()) continue

                    // Filter out sold_by_weight
                    val soldByWeight = item.optBoolean("sold_by_weight", false)
                    if (soldByWeight) continue

                    // Preserva a metadata completa necessária ao catálogo de pizza do Capacitor.
                    var isPdvOnly = false
                    var isHidden = false
                    var hasStuffedCrust = item.optBoolean("has_stuffed_crust", false)
                    var isCombo = item.optBoolean("is_combo", false)
                    var isPastelFlavor = item.optBoolean("is_pastel_flavor", false)
                    var isBeverage = item.optBoolean("is_beverage", false)
                    var metadataObj = item.optJSONObject("metadata")
                    if (metadataObj == null && item.has("metadata") && !item.isNull("metadata")) {
                        val metaStr = item.optString("metadata", "")
                        if (metaStr.isNotBlank() && metaStr.startsWith("{")) {
                            try { metadataObj = JSONObject(metaStr) } catch (_: Exception) {}
                        }
                    }
                    val pizzaSizeOverrides = mutableMapOf<String, Double>()
                    val pizzaUnavailableSizeIds = mutableSetOf<String>()
                    val legacyPizzaSizes = mutableListOf<PizzaLegacySize>()
                    val pizzaCategoryId: String
                    if (metadataObj != null) {
                        isPdvOnly = metadataObj.optBoolean("pdv_only", false)
                        isHidden = metadataObj.optBoolean("hidden", false)
                        if (metadataObj.has("has_stuffed_crust")) hasStuffedCrust = metadataObj.optBoolean("has_stuffed_crust", false)
                        if (metadataObj.has("is_combo")) isCombo = metadataObj.optBoolean("is_combo", false)
                        if (metadataObj.has("is_pastel_flavor")) isPastelFlavor = metadataObj.optBoolean("is_pastel_flavor", false)
                        if (metadataObj.has("is_beverage")) isBeverage = metadataObj.optBoolean("is_beverage", false)
                        val overrides = metadataObj.optJSONObject("pizza_size_overrides")
                        if (overrides != null) {
                            val keys = overrides.keys()
                            while (keys.hasNext()) {
                                val sizeId = keys.next()
                                val priceForSize = overrides.optDouble(sizeId, 0.0)
                                if (priceForSize > 0.0) pizzaSizeOverrides[sizeId] = priceForSize
                            }
                        }
                        val unavailable = metadataObj.optJSONArray("pizza_unavailable_sizes")
                        if (unavailable != null) {
                            for (sizeIndex in 0 until unavailable.length()) {
                                unavailable.optString(sizeIndex, "").trim().takeIf { it.isNotBlank() }?.let(pizzaUnavailableSizeIds::add)
                            }
                        }
                        val legacySizes = metadataObj.optJSONArray("sizes")
                        if (legacySizes != null) {
                            for (sizeIndex in 0 until legacySizes.length()) {
                                val legacySize = legacySizes.optJSONObject(sizeIndex) ?: continue
                                val legacyName = legacySize.optString("name", "").trim()
                                val legacyPrice = legacySize.optDouble("price", 0.0)
                                if (legacyName.isNotBlank() && legacyPrice > 0.0) legacyPizzaSizes.add(PizzaLegacySize(legacyName, legacyPrice))
                            }
                        }
                        pizzaCategoryId = metadataObj.optString("pizza_category_id", "").trim()
                    } else {
                        pizzaCategoryId = ""
                    }

                    if (isPdvOnly || isHidden) continue

                    val rawDesc = item.optString("description", "")
                    val description = if (rawDesc.trim() == "null") "" else rawDesc
                    val price = item.optDouble("price", 0.0)
                    val category = item.optString("category", "Geral")
                    val sectionId = item.optNullableString("section_id")
                    val imageUrl = item.optString("image_url", "")
                    val isAvailable = item.optBoolean("is_available", true)

                    productsList.add(
                        Product(
                            id = id,
                            storeId = storeId,
                            name = name,
                            description = description,
                            price = price,
                            category = category,
                            sectionId = sectionId,
                            imageUrl = imageUrl,
                            isAvailable = isAvailable,
                            hasStuffedCrust = hasStuffedCrust,
                            isCombo = isCombo,
                            isPastelFlavor = isPastelFlavor,
                            isBeverage = isBeverage,
                            pizzaCategoryId = pizzaCategoryId,
                            pizzaSizeOverrides = pizzaSizeOverrides,
                            pizzaUnavailableSizeIds = pizzaUnavailableSizeIds,
                            legacyPizzaSizes = legacyPizzaSizes
                        )
                    )
                }
                productsList
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products for store $storeId", e)
            emptyList()
        }
    }

    // 5b. FETCH ADDON GROUPS FOR STORE
    suspend fun fetchAddonGroupsForStore(storeId: String): List<AddonGroup> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/addon_groups?store_id=eq.$storeId&order=sort_order.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            var response = httpClient.newCall(request).execute()
            var responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val fallbackUrl = "$SUPABASE_URL/rest/v1/addon_groups?select=*"
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()
                response = httpClient.newCall(fallbackRequest).execute()
                responseText = response.body?.string() ?: ""
            }

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val groupsList = mutableListOf<AddonGroup>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "Adicionais")
                    val minSelect = item.optInt("min_select", 0)
                    val maxSelect = item.optInt("max_select", 1)
                    val sortOrder = item.optInt("sort_order", 0)
                    val priceReplacesBase = item.optBoolean("price_replaces_base", false)
                    val productId = item.optNullableString("product_id")
                    val sId = item.optNullableString("store_id")

                    if (id.isNotBlank()) {
                        groupsList.add(
                            AddonGroup(
                                id = id,
                                storeId = sId,
                                productId = productId,
                                name = name,
                                minSelect = minSelect,
                                maxSelect = maxSelect,
                                sortOrder = sortOrder,
                                priceReplacesBase = priceReplacesBase
                            )
                        )
                    }
                }
                groupsList.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching addon_groups for store $storeId", e)
            emptyList()
        }
    }

    // 5c. FETCH PRODUCT ADDON GROUPS MAP
    suspend fun fetchProductAddonGroupsMap(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/product_addon_groups?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val map = mutableMapOf<String, MutableList<String>>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val productId = item.optString("product_id", "")
                    val addonGroupId = item.optString("addon_group_id", "")
                    if (productId.isNotBlank() && addonGroupId.isNotBlank()) {
                        val list = map.getOrPut(productId) { mutableListOf() }
                        list.add(addonGroupId)
                    }
                }
                map
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product_addon_groups", e)
            emptyMap()
        }
    }

    // 5d. FETCH ADDON ITEMS FOR STORE
    suspend fun fetchAddonItemsForStore(): List<AddonItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/addon_items?select=*&order=sort_order.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val itemsList = mutableListOf<AddonItem>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val groupId = item.optString("group_id", item.optString("addon_group_id", ""))
                    val name = item.optString("name", "")
                    val price = item.optDouble("price", 0.0)
                    val sortOrder = item.optInt("sort_order", 0)
                    val isAvailable = item.optBoolean("is_available", true)

                    if (id.isNotBlank() && groupId.isNotBlank() && name.isNotBlank()) {
                        itemsList.add(
                            AddonItem(
                                id = id,
                                groupId = groupId,
                                name = name,
                                price = price,
                                sortOrder = sortOrder,
                                isAvailable = isAvailable
                            )
                        )
                    }
                }
                itemsList.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching addon_items", e)
            emptyList()
        }
    }

    // 5e. FETCH PASTEL BORDERS FOR STORE
    suspend fun fetchPastelBordersForStore(storeId: String): List<com.example.data.model.PastelBorder> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/pastel_borders?store_id=eq.$storeId&is_available=eq.true&order=sort_order.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val bordersList = mutableListOf<com.example.data.model.PastelBorder>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    val price = item.optDouble("price", 0.0)
                    val isAvailable = item.optBoolean("is_available", true)
                    val sortOrder = item.optInt("sort_order", 0)
                    val sId = item.optString("store_id", storeId)

                    if (id.isNotBlank() && name.isNotBlank()) {
                        bordersList.add(
                            com.example.data.model.PastelBorder(
                                id = id,
                                storeId = sId,
                                name = name,
                                price = price,
                                isAvailable = isAvailable,
                                sortOrder = sortOrder
                            )
                        )
                    }
                }
                bordersList.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pastel_borders for store $storeId", e)
            emptyList()
        }
    }

    // 5f. FETCH PIZZA BORDERS FOR STORE (borda recheada, catupiry, cheddar, etc.)
    suspend fun fetchPizzaBordersForStore(storeId: String): List<com.example.data.model.PastelBorder> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/pizza_borders?store_id=eq.$storeId&is_available=eq.true&order=sort_order.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonArray = JSONArray(responseText)
                val bordersList = mutableListOf<com.example.data.model.PastelBorder>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    val price = item.optDouble("price", 0.0)
                    val isAvailable = item.optBoolean("is_available", true)
                    val sortOrder = item.optInt("sort_order", 0)
                    val sId = item.optString("store_id", storeId)

                    if (id.isNotBlank() && name.isNotBlank()) {
                        bordersList.add(
                            com.example.data.model.PastelBorder(
                                id = id,
                                storeId = sId,
                                name = name,
                                price = price,
                                isAvailable = isAvailable,
                                sortOrder = sortOrder
                            )
                        )
                    }
                }
                bordersList.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pizza_borders for store $storeId", e)
            emptyList()
        }
    }

    // 6. FETCH COUPON FROM coupons_public
    suspend fun fetchCoupon(code: String, storeId: String?): com.example.data.model.Coupon? = withContext(Dispatchers.IO) {
        try {
            val cleanCode = code.trim().uppercase()
            val url = "$SUPABASE_URL/rest/v1/coupons_public?code=eq.$cleanCode&is_active=eq.true"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonText = response.body?.string() ?: return@withContext null
                val array = JSONArray(jsonText)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val cStoreId = obj.optNullableString("store_id")
                    
                    // Filter by store_id if coupon is store-specific
                    if (cStoreId != null && cStoreId.isNotBlank() && cStoreId != "null" && storeId != null && cStoreId != storeId) {
                        return@withContext null
                    }

                    return@withContext com.example.data.model.Coupon(
                        id = obj.optString("id", ""),
                        code = obj.optString("code", cleanCode),
                        discountType = obj.optString("discount_type", "fixed"),
                        discountValue = obj.optDouble("discount_value", 0.0),
                        minOrderValue = obj.optDouble("min_order_value", 0.0),
                        expiresAt = obj.optNullableString("expires_at"),
                        firstOrderOnly = obj.optBoolean("first_order_only", false),
                        isActive = obj.optBoolean("is_active", true),
                        storeId = cStoreId
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching coupon", e)
            null
        }
    }

    suspend fun submitOrderRating(orderId: String, storeId: String, userId: String, rating: Int, comment: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (orderId.isBlank() || storeId.isBlank() || userId.isBlank() || accessToken.isBlank() || rating !in 1..5) return@withContext false
        try {
            val body = JSONObject().apply {
                put("order_id", orderId)
                put("store_id", storeId)
                put("user_id", userId)
                put("rating", rating)
                put("comment", comment.trim().ifBlank { JSONObject.NULL })
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/order_ratings")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao enviar avaliação", error)
            false
        }
    }

    suspend fun requestRefund(order: com.example.data.model.Order, userId: String, reason: String, description: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (order.id.isBlank() || order.storeId.isBlank() || userId.isBlank() || accessToken.isBlank()) return@withContext false
        try {
            val body = JSONObject().apply {
                put("order_id", order.id)
                put("store_id", order.storeId)
                put("requester_id", userId)
                put("reason", reason)
                put("description", description.trim().ifBlank { JSONObject.NULL })
                put("refund_type", "wallet_credit")
                put("requested_amount", order.total)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/refund_requests")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao solicitar reembolso", error)
            false
        }
    }

    /** Registra o consumo do cupom sem bloquear a confirmação do pedido. */
    suspend fun registerCouponUse(couponId: String, userId: String, orderId: String, accessToken: String) = withContext(Dispatchers.IO) {
        if (couponId.isBlank() || userId.isBlank() || orderId.isBlank() || accessToken.isBlank()) return@withContext
        try {
            val body = JSONObject().apply {
                put("coupon_id", couponId)
                put("user_id", userId)
                put("order_id", orderId)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/coupon_uses")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().close()
        } catch (error: Exception) {
            Log.w(TAG, "Não foi possível registrar o uso do cupom", error)
        }
    }

    /** Lê o saldo disponível da carteira do cliente autenticado. */
    suspend fun fetchWalletBalance(userId: String, accessToken: String): com.example.data.model.WalletBalance = withContext(Dispatchers.IO) {
        if (userId.isBlank() || accessToken.isBlank()) return@withContext com.example.data.model.WalletBalance()
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/user_wallet?select=balance&user_id=eq.$userId&limit=1")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext com.example.data.model.WalletBalance()
                val array = JSONArray(response.body?.string().orEmpty())
                com.example.data.model.WalletBalance(array.optJSONObject(0)?.optDouble("balance", 0.0) ?: 0.0)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Não foi possível carregar a carteira", error)
            com.example.data.model.WalletBalance()
        }
    }

    /** Lê a configuração de fidelidade habilitada para a loja. */
    suspend fun fetchLoyaltyConfig(storeId: String, accessToken: String): com.example.data.model.LoyaltyConfig? = withContext(Dispatchers.IO) {
        if (storeId.isBlank() || accessToken.isBlank()) return@withContext null
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/loyalty_config?select=store_id,is_enabled,min_points_redeem,discount_per_point,max_discount_percent,points_per_real&store_id=eq.$storeId&is_enabled=eq.true&limit=1")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val obj = JSONArray(response.body?.string().orEmpty()).optJSONObject(0) ?: return@withContext null
                com.example.data.model.LoyaltyConfig(
                    storeId = obj.optString("store_id", storeId),
                    isEnabled = obj.optBoolean("is_enabled", false),
                    minPointsRedeem = obj.optInt("min_points_redeem", 0).takeIf { it > 0 } ?: 50,
                    discountPerPoint = obj.optDouble("discount_per_point", 0.0).takeIf { it > 0 } ?: 0.10,
                    maxDiscountPercent = obj.optDouble("max_discount_percent", 0.0).takeIf { it > 0 } ?: 20.0,
                    pointsPerReal = obj.optInt("points_per_real", 0).takeIf { it > 0 } ?: 1
                )
            }
        } catch (error: Exception) {
            Log.w(TAG, "Não foi possível carregar a fidelidade", error)
            null
        }
    }

    /** Lê os pontos do cliente autenticado na loja informada. */
    suspend fun fetchLoyaltyBalance(userId: String, storeId: String, accessToken: String): com.example.data.model.LoyaltyBalance = withContext(Dispatchers.IO) {
        if (userId.isBlank() || storeId.isBlank() || accessToken.isBlank()) return@withContext com.example.data.model.LoyaltyBalance()
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/loyalty_points?select=points&user_id=eq.$userId&store_id=eq.$storeId&limit=1")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext com.example.data.model.LoyaltyBalance()
                val points = JSONArray(response.body?.string().orEmpty()).optJSONObject(0)?.optInt("points", 0) ?: 0
                com.example.data.model.LoyaltyBalance(points.coerceAtLeast(0))
            }
        } catch (error: Exception) {
            Log.w(TAG, "Não foi possível carregar os pontos de fidelidade", error)
            com.example.data.model.LoyaltyBalance()
        }
    }

    suspend fun applyWalletDiscount(orderId: String, userId: String, discountAmount: Double, accessToken: String): Result<Unit> =
        callCheckoutRpc(
            functionName = "apply_wallet_discount",
            body = JSONObject().apply {
                put("_order_id", orderId)
                put("_user_id", userId)
                put("_discount_amount", discountAmount)
            },
            accessToken = accessToken
        )

    suspend fun redeemLoyaltyPoints(orderId: String, storeId: String, pointsToUse: Int, accessToken: String): Result<Unit> =
        callCheckoutRpc(
            functionName = "redeem_loyalty_points",
            body = JSONObject().apply {
                put("_order_id", orderId)
                put("_store_id", storeId)
                put("_points_to_use", pointsToUse)
            },
            accessToken = accessToken
        )

    private suspend fun callCheckoutRpc(functionName: String, body: JSONObject, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext Result.failure(IllegalStateException("Sua sessão expirou."))
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/$functionName")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException(parseErrorMessage(responseBody, "Não foi possível aplicar o benefício.")))
            }
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao executar $functionName", error)
            Result.failure(IllegalStateException("Falha de conexão ao aplicar o benefício."))
        }
    }

    // 7. VIA CEP ADDRESS LOOKUP
    data class CepAddress(
        val cep: String = "",
        val street: String = "",
        val neighborhood: String = "",
        val city: String = "",
        val state: String = ""
    )

    suspend fun fetchAddressByCep(cep: String): CepAddress? = withContext(Dispatchers.IO) {
        try {
            val cleanCep = cep.replace(Regex("[^0-9]"), "")
            if (cleanCep.length != 8) return@withContext null

            val url = "https://viacep.com.br/ws/$cleanCep/json/"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optBoolean("erro", false)) return@withContext null

                CepAddress(
                    cep = json.optString("cep", cleanCep),
                    street = json.optString("logradouro", ""),
                    neighborhood = json.optString("bairro", ""),
                    city = json.optString("localidade", ""),
                    state = json.optString("uf", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching CEP $cep", e)
            null
        }
    }

    /** Gera o PIX online do pedido pela mesma Edge Function usada pelo Capacitor. */
    suspend fun generatePixForOrder(
        order: com.example.data.model.Order,
        payerFullName: String,
        payerDocument: String,
        accessToken: String
    ): PixPaymentResponse = withContext(Dispatchers.IO) {
        try {
            val document = payerDocument.filter { it.isDigit() }
            if (document.length != 11) {
                return@withContext PixPaymentResponse(false, errorMessage = "Cadastre um CPF válido no perfil antes de pagar com PIX.")
            }
            val nameParts = payerFullName.trim().ifBlank { "Cliente ItaSuper" }.split(Regex("\\s+"))
            val firstName = nameParts.firstOrNull().orEmpty().ifBlank { "Cliente" }
            val lastName = nameParts.drop(1).joinToString(" ").ifBlank { "ItaSuper" }
            val requestJson = JSONObject().apply {
                put("action", "order_pix")
                put("order_id", order.id)
                put("amount", order.total)
                put("description", "Pedido #${order.id.take(6).uppercase()} - ${order.storeName.ifBlank { "ItaSuper" }}")
                put("payer_first_name", firstName)
                put("payer_last_name", lastName)
                put("payer_cpf", document)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/payment-router")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful || responseText.isBlank()) {
                return@withContext PixPaymentResponse(false, errorMessage = parseErrorMessage(responseText, "Não foi possível gerar o PIX."))
            }
            val payload = JSONObject(responseText)
            if (payload.optBoolean("rate_limited", false)) {
                return@withContext PixPaymentResponse(false, errorMessage = "Muitas tentativas de PIX. Aguarde alguns minutos e tente novamente.")
            }
            val gatewayError = payload.optString("error", "").trim()
            if (gatewayError.isNotBlank()) {
                return@withContext PixPaymentResponse(false, errorMessage = gatewayError)
            }
            val pixCode = payload.optString("pix_code", payload.optString("qr_code", "")).trim().ifBlank { null }
            val qrCodeBase64 = payload.optString("qr_code_url", payload.optString("qr_code_base64", "")).trim().ifBlank { null }
            if (pixCode == null && qrCodeBase64 == null) {
                return@withContext PixPaymentResponse(false, errorMessage = "O servidor não devolveu o QR Code do PIX.")
            }
            PixPaymentResponse(true, pixCode = pixCode, qrCodeBase64 = qrCodeBase64)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating order PIX", e)
            PixPaymentResponse(false, errorMessage = "Falha de conexão ao gerar o PIX.")
        }
    }

    /** Envia o comprovante do PIX direto e vincula o arquivo ao pedido pela RPC oficial. */
    suspend fun uploadPixDirectProof(
        order: com.example.data.model.Order,
        bytes: ByteArray,
        mimeType: String,
        extension: String,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (bytes.isEmpty()) return@withContext Result.failure(IllegalArgumentException("O comprovante está vazio."))
            if (mimeType !in setOf("image/jpeg", "image/png", "application/pdf")) {
                return@withContext Result.failure(IllegalArgumentException("Envie um comprovante JPG, PNG ou PDF."))
            }
            if (bytes.size > 5 * 1024 * 1024) return@withContext Result.failure(IllegalArgumentException("O comprovante deve ter no máximo 5 MB."))
            val normalizedExtension = extension.lowercase().ifBlank { "jpg" }
            val path = "${order.storeId}/${order.id}.$normalizedExtension"
            val bearer = "Bearer $accessToken"
            val uploadRequest = Request.Builder()
                .url("$SUPABASE_URL/storage/v1/object/pix-proofs/$path")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .addHeader("x-upsert", "true")
                .addHeader("Content-Type", mimeType)
                .post(bytes.toRequestBody(mimeType.toMediaType()))
                .build()
            val uploadResponse = httpClient.newCall(uploadRequest).execute()
            val uploadBody = uploadResponse.body?.string().orEmpty()
            if (!uploadResponse.isSuccessful) {
                return@withContext Result.failure(IllegalStateException(parseErrorMessage(uploadBody, "Não foi possível enviar o comprovante.")))
            }

            val attachJson = JSONObject().apply {
                put("p_order_id", order.id)
                put("p_proof_path", path)
            }
            val attachRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/attach_pix_proof")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .addHeader("Content-Type", "application/json")
                .post(attachJson.toString().toRequestBody(jsonMediaType))
                .build()
            val attachResponse = httpClient.newCall(attachRequest).execute()
            val attachBody = attachResponse.body?.string().orEmpty()
            if (!attachResponse.isSuccessful) {
                return@withContext Result.failure(IllegalStateException(parseErrorMessage(attachBody, "O comprovante foi enviado, mas não pôde ser associado ao pedido.")))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading PIX direct proof", e)
            Result.failure(IllegalStateException("Falha de conexão ao enviar o comprovante."))
        }
    }

    /** Carrega os pedidos e os itens do cliente autenticado, sem dados demonstrativos. */
    suspend fun fetchOrdersForClient(clientId: String, accessToken: String): List<com.example.data.model.Order> = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || accessToken.isBlank()) return@withContext emptyList()
        try {
            val bearer = "Bearer $accessToken"
            val ordersRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/orders?select=*&client_id=eq.$clientId&order=created_at.desc")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .get()
                .build()
            val ordersResponse = httpClient.newCall(ordersRequest).execute()
            val ordersText = ordersResponse.body?.string().orEmpty()
            if (!ordersResponse.isSuccessful || ordersText.isBlank()) return@withContext emptyList()
            val ordersArray = JSONArray(ordersText)
            if (ordersArray.length() == 0) return@withContext emptyList()

            val orderIds = (0 until ordersArray.length()).mapNotNull { index ->
                ordersArray.optJSONObject(index)?.optString("id", "")?.takeIf { it.isNotBlank() }
            }
            val storeIds = (0 until ordersArray.length()).mapNotNull { index ->
                ordersArray.optJSONObject(index)?.optString("store_id", "")?.takeIf { it.isNotBlank() }
            }.distinct()
            val storesById = fetchStoreNames(storeIds, bearer)
            val itemsByOrder = fetchOrderItems(orderIds, bearer)

            (0 until ordersArray.length()).mapNotNull { index ->
                val item = ordersArray.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id", "")
                val storeId = item.optString("store_id", "")
                if (id.isBlank() || storeId.isBlank()) return@mapNotNull null
                com.example.data.model.Order(
                    id = id,
                    storeId = storeId,
                    storeName = storesById[storeId].orEmpty(),
                    items = itemsByOrder[id].orEmpty(),
                    subtotal = item.optDouble("subtotal", 0.0),
                    deliveryFee = item.optDouble("delivery_fee", 0.0),
                    discount = item.optDouble("discount", item.optDouble("coupon_discount", 0.0)),
                    walletDiscount = item.optDouble("wallet_discount", 0.0),
                    loyaltyPointsUsed = item.optInt("loyalty_points_used", 0),
                    loyaltyDiscount = item.optDouble("loyalty_discount", 0.0),
                    total = item.optDouble("total_price", 0.0),
                    paymentMethod = item.optString("payment_method", ""),
                    deliveryAddress = item.optString("address_details", ""),
                    status = item.optString("status", "pendente"),
                    createdAt = item.optString("created_at", ""),
                    confirmedAt = item.optString("confirmed_at", ""),
                    deliveryPin = item.optString("delivery_pin", ""),
                    neighborhood = item.optString("neighborhood", ""),
                    driverId = item.optString("driver_id", item.optString("assigned_driver_id", "")),
                    deliveryConfirmedByClient = item.optBoolean("delivery_confirmed_by_client", false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching client orders", e)
            emptyList()
        }
    }

    /** Carrega até 100 notificações da conta autenticada por uma função que valida a sessão. */
    suspend fun fetchClientNotifications(accessToken: String): List<com.example.data.model.ClientNotification> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) throw IllegalStateException("Sua sessão não possui autorização para carregar notificações.")
        val request = Request.Builder()
            .url("$SUPABASE_URL/functions/v1/client-notifications")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        try {
            val response = httpClient.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "Client notifications request failed: ${response.code} $text")
                throw IllegalStateException(parseErrorMessage(text, "Não foi possível carregar as notificações."))
            }
            val array = JSONObject(text).optJSONArray("notifications") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id", "")
                    if (id.isBlank()) continue
                    val payloadJson = item.optJSONObject("payload")
                    val payload = buildMap {
                        payloadJson?.keys()?.forEach { key ->
                            put(key, payloadJson.optString(key, ""))
                        }
                    }
                    add(
                        com.example.data.model.ClientNotification(
                            id = id,
                            orderId = item.optString("order_id", "").takeIf { it.isNotBlank() },
                            type = item.optString("notification_type", "order_update"),
                            title = item.optString("title", "ItaSuper"),
                            body = item.optString("body", ""),
                            payload = payload,
                            readAt = item.optString("read_at", "").takeIf { it.isNotBlank() },
                            createdAt = item.optString("created_at", "")
                        )
                    )
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Error fetching client notifications", error)
            throw error
        }
    }

    /** Marca uma notificação própria como lida; o RLS impede alteração de outros clientes. */
    suspend fun markClientNotificationRead(notificationId: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (notificationId.isBlank() || accessToken.isBlank()) return@withContext false
        try {
            val body = JSONObject().put("read_at", java.time.Instant.now().toString())
                .toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/client_notifications?id=eq.$notificationId")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (error: Exception) {
            Log.w(TAG, "Error marking client notification as read", error)
            false
        }
    }

    private fun fetchStoreNames(storeIds: List<String>, bearer: String): Map<String, String> {
        if (storeIds.isEmpty()) return emptyMap()
        return try {
            val ids = storeIds.joinToString(",")
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/stores_public?select=id,name&id=in.($ids)")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful || text.isBlank()) return emptyMap()
            val array = JSONArray(text)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id", "")
                    if (id.isNotBlank()) put(id, item.optString("name", ""))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Store names unavailable for order history", e)
            emptyMap()
        }
    }

    private fun fetchOrderItems(orderIds: List<String>, bearer: String): Map<String, List<com.example.data.model.CartItem>> {
        if (orderIds.isEmpty()) return emptyMap()
        return try {
            val ids = orderIds.joinToString(",")
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/order_items?select=order_id,product_id,quantity,unit_price,addons,observations,products(id,store_id,name,description,price,category,image_url)&order_id=in.($ids)")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", bearer)
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful || text.isBlank()) return emptyMap()
            val array = JSONArray(text)
            val result = mutableMapOf<String, MutableList<com.example.data.model.CartItem>>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val orderId = item.optString("order_id", "")
                val productJson = item.optJSONObject("products")
                if (orderId.isBlank() || productJson == null) continue
                val product = com.example.data.model.Product(
                    id = productJson.optString("id", item.optString("product_id", "")),
                    storeId = productJson.optString("store_id", ""),
                    name = productJson.optString("name", "Produto"),
                    description = productJson.optString("description", ""),
                    price = item.optDouble("unit_price", productJson.optDouble("price", 0.0)),
                    category = productJson.optString("category", ""),
                    imageUrl = productJson.optString("image_url", "")
                )
                result.getOrPut(orderId) { mutableListOf() }.add(
                    com.example.data.model.CartItem(
                        product = product,
                        quantity = item.optInt("quantity", 1),
                        notes = item.optString("observations", "")
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Order items unavailable for order history", e)
            emptyMap()
        }
    }

    /** Confirma a entrega pela mesma RPC protegida usada pelo cliente Capacitor. */
    suspend fun confirmDeliveryByClient(orderId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        callOrderRpc("client_confirm_delivery", orderId, accessToken)
    }

    /** Aplica a política oficial de cancelamento do pedido no backend. */
    suspend fun cancelOrderByClient(orderId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        callOrderRpc("apply_cancellation_policy", orderId, accessToken)
    }

    private fun callOrderRpc(functionName: String, orderId: String, accessToken: String): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/$functionName")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(JSONObject().put("_order_id", orderId).toString().toRequestBody(jsonMediaType))
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) Result.success(Unit) else {
                Result.failure(IllegalStateException(parseErrorMessage(body, "Não foi possível atualizar o pedido.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling $functionName", e)
            Result.failure(IllegalStateException("Falha de conexão ao atualizar o pedido."))
        }
    }

    // 8. CREATE ORDER AND ITEMS WITH THE AUTHENTICATED CLIENT SESSION
    suspend fun submitOrder(
        order: com.example.data.model.Order,
        clientId: String,
        accessToken: String,
        neighborhood: String,
        needsChange: Boolean,
        changeFor: Double?,
        clientLatitude: Double? = null,
        clientLongitude: Double? = null
    ): OrderSubmissionResponse = withContext(Dispatchers.IO) {
        try {
            val bearer = if (accessToken.isNotBlank()) accessToken else SUPABASE_ANON_KEY
            val orderJson = JSONObject().apply {
                put("client_id", clientId)
                put("store_id", order.storeId)
                put("subtotal", order.subtotal)
                put("delivery_fee", order.deliveryFee)
                put("wallet_discount", order.walletDiscount)
                put("loyalty_points_used", order.loyaltyPointsUsed)
                put("loyalty_discount", order.loyaltyDiscount)
                put("total_price", order.total)
                put("payment_method", order.paymentMethod)
                put("neighborhood", neighborhood)
                put("address_details", order.deliveryAddress)
                put("client_lat", clientLatitude ?: JSONObject.NULL)
                put("client_lng", clientLongitude ?: JSONObject.NULL)
                put("needs_change", needsChange)
                put("change_for", changeFor ?: JSONObject.NULL)
                put("status", order.status)
            }
            val orderRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/orders")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(orderJson.toString().toRequestBody(jsonMediaType))
                .build()
            val orderResponse = httpClient.newCall(orderRequest).execute()
            val orderResponseText = orderResponse.body?.string().orEmpty()
            if (!orderResponse.isSuccessful || orderResponseText.isBlank()) {
                return@withContext OrderSubmissionResponse(
                    isSuccess = false,
                    errorMessage = parseErrorMessage(orderResponseText, "Não foi possível criar o pedido.")
                )
            }

            val createdOrder = JSONArray(orderResponseText).optJSONObject(0)
                ?: return@withContext OrderSubmissionResponse(false, errorMessage = "O servidor não devolveu o identificador do pedido.")
            val orderId = createdOrder.optString("id", "")
            if (orderId.isBlank()) {
                return@withContext OrderSubmissionResponse(false, errorMessage = "O servidor não devolveu o identificador do pedido.")
            }

            val itemsJson = JSONArray()
            order.items.forEach { cartItem ->
                val addons = JSONArray()
                cartItem.selectedAddons.forEach { addon ->
                    addons.put(JSONObject().apply {
                        put("id", addon.itemId)
                        put("name", addon.itemName)
                        put("price", addon.itemPrice)
                        put("group_id", addon.groupId)
                        put("groupName", addon.groupName)
                        put("priceReplacesBase", addon.priceReplacesBase)
                    })
                }
                itemsJson.put(JSONObject().apply {
                    put("order_id", orderId)
                    put("product_id", cartItem.product.id)
                    put("quantity", cartItem.quantity)
                    put("unit_price", cartItem.unitPrice)
                    // O Capacitor grava a representação JSON dos adicionais neste campo JSONB.
                    put("addons", if (addons.length() > 0) addons.toString() else JSONObject.NULL)
                    put("observations", cartItem.notes.ifBlank { JSONObject.NULL })
                })
            }
            val itemsRequest = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/order_items")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(itemsJson.toString().toRequestBody(jsonMediaType))
                .build()
            val itemsResponse = httpClient.newCall(itemsRequest).execute()
            val itemsResponseText = itemsResponse.body?.string().orEmpty()
            if (!itemsResponse.isSuccessful) {
                Log.e(TAG, "Order $orderId was created but order_items insertion failed: $itemsResponseText")
                return@withContext OrderSubmissionResponse(
                    isSuccess = false,
                    errorMessage = parseErrorMessage(itemsResponseText, "O pedido foi criado, mas não foi possível registrar os itens.")
                )
            }

            OrderSubmissionResponse(
                isSuccess = true,
                orderId = orderId,
                createdAt = createdOrder.optString("created_at", "").ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting order", e)
            OrderSubmissionResponse(isSuccess = false, errorMessage = "Falha de conexão ao enviar o pedido.")
        }
    }

    suspend fun fetchDiscoverProducts(openStores: List<Store>): List<com.example.data.model.DiscoverProduct> = withContext(Dispatchers.IO) {
        try {
            if (openStores.isNotEmpty()) {
                val storeIds = openStores.map { it.id }
                val idFilter = storeIds.joinToString(",")
                val url = "$SUPABASE_URL/rest/v1/products?select=*&is_available=eq.true&store_id=in.($idFilter)&limit=8"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful && responseText.isNotBlank()) {
                    val jsonArray = JSONArray(responseText)
                    val list = mutableListOf<com.example.data.model.DiscoverProduct>()
                    val storeMap = openStores.associateBy { it.id }
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val id = item.optString("id", "")
                        val sId = item.optString("store_id", "")
                        val name = item.optString("name", "")
                        val price = item.optDouble("price", 0.0)
                        val img = item.optString("image_url", "")
                        val storeName = storeMap[sId]?.name ?: "Loja ItaSuper"
                        val storeCategory = storeMap[sId]?.category ?: ""
                        if (id.isNotBlank() && name.isNotBlank()) {
                            list.add(com.example.data.model.DiscoverProduct(id, sId, storeName, storeCategory, name, price, img))
                        }
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching discover products", e)
        }
        // Sem fallback fictício: se não houver produtos reais de lojas abertas,
        // a seção "Descubra" simplesmente não é exibida (lista vazia).
        emptyList()
    }

    private fun parseErrorMessage(jsonText: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(jsonText)
            when {
                json.has("error_description") -> json.getString("error_description")
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                else -> defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (this.has(key) && !this.isNull(key)) {
            val str = this.optString(key, "")
            if (str.isNotBlank() && str != "null") return str
        }
        return null
    }
}
