package com.example.data.remote

import android.util.Log
import com.example.data.model.Product
import com.example.data.model.Store
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
    val email: String? = null,
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
                val accessToken = json.optString("access_token", null)
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: json.optString("id")
                val userEmail = userObj?.optString("email") ?: email

                SupabaseAuthResponse(
                    isSuccess = true,
                    userId = userId,
                    accessToken = accessToken,
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
                val accessToken = json.optString("access_token", null)
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: json.optString("id")
                val userEmail = userObj?.optString("email") ?: email

                SupabaseAuthResponse(
                    isSuccess = true,
                    userId = userId,
                    accessToken = accessToken,
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

    // 4. FETCH STORES FROM STORES_PUBLIC VIEW
    suspend fun fetchActiveStores(): List<Store> = withContext(Dispatchers.IO) {
        try {
            // Query stores_public view ordered by rating desc.
            // Omit status=eq.active parameter from query string to avoid Postgres enum type mismatch (22P02)
            val url = "$SUPABASE_URL/rest/v1/stores_public?order=rating.desc"

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
                val storeList = mutableListOf<Store>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    
                    val status = item.optString("status", "active")
                    // Filter out inactive or blocked stores if status column is present
                    if (status.equals("inactive", ignoreCase = true) || status.equals("blocked", ignoreCase = true) || status.equals("disabled", ignoreCase = true)) {
                        continue
                    }

                    val id = item.optString("id", "")
                    val name = item.optString("name", "Loja ItaSuper")
                    val category = item.optString("category", "Geral")
                    val rating = item.optDouble("rating", 5.0).let { if (it.isNaN()) 5.0 else it }
                    val imageUrl = item.optString("image_url", "")
                    val isForceClosed = item.optBoolean("force_closed", false)
                    val isOpen = item.optBoolean("is_open", true) && !isForceClosed

                    val ownFee = item.optDouble("own_delivery_fee", 0.0)
                    val isFree = ownFee <= 0.0
                    val deliveryFeeText = if (isFree) "Grátis" else String.format("R$ %.2f", ownFee).replace(".", ",")

                    val store = Store(
                        id = id,
                        name = name,
                        category = category,
                        rating = rating,
                        deliveryTime = "30-40 min",
                        deliveryFee = deliveryFeeText,
                        isFreeDelivery = isFree,
                        isOpen = isOpen,
                        distanceKm = 1.2,
                        logoUrl = imageUrl,
                        bannerUrl = imageUrl,
                        minOrder = 15.0
                    )
                    storeList.add(store)
                }
                storeList
            } else {
                Log.e(TAG, "Failed fetching stores_public: code=${response.code}, body=$responseText")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stores_public", e)
            emptyList()
        }
    }

    // 5. FETCH PRODUCTS FOR STORE
    suspend fun fetchProductsForStore(storeId: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/products?store_id=eq.$storeId"

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
                    val name = item.optString("name", "Produto")
                    val description = item.optString("description", "")
                    val price = item.optDouble("price", 0.0)
                    val category = item.optString("category", "Geral")
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
                            imageUrl = imageUrl,
                            isAvailable = isAvailable
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

    // 6. CREATE ORDER
    suspend fun submitOrder(order: com.example.data.model.Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/orders"

            val bodyJson = JSONObject().apply {
                put("id", order.id)
                put("store_id", order.storeId)
                put("store_name", order.storeName)
                put("total", order.total)
                put("status", order.status)
                put("payment_method", order.paymentMethod)
                put("delivery_address", order.deliveryAddress)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting order", e)
            false
        }
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
}
