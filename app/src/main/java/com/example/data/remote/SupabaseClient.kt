package com.example.data.remote

import android.util.Log
import com.example.data.model.AddonGroup
import com.example.data.model.AddonItem
import com.example.data.model.MenuSection
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

    // 3b. UPDATE PROFILE NUMBER/ADDRESS IN SUPABASE
    suspend fun updateUserProfileNumber(userId: String, number: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId"
            val bodyJson = JSONObject().apply {
                put("number", number)
                put("address_number", number)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
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
                    val isFree = ownFee <= 0.0
                    val deliveryFeeText = if (isFree) "Grátis" else String.format("R$ %.2f", ownFee).replace(".", ",")

                    val createdAt = item.optString("created_at", "")
                    val lat = if (item.has("latitude") && !item.isNull("latitude")) item.optDouble("latitude") else null
                    val lng = if (item.has("longitude") && !item.isNull("longitude")) item.optDouble("longitude") else null

                    val store = Store(
                        id = id,
                        name = name,
                        category = category,
                        rating = rating,
                        deliveryTime = "30-40 min",
                        deliveryFee = deliveryFeeText,
                        isFreeDelivery = isFree,
                        isOpen = computedIsOpen,
                        distanceKm = 1.2,
                        logoUrl = imageUrl,
                        bannerUrl = bannerUrl,
                        minOrder = 15.0,
                        createdAt = createdAt,
                        latitude = lat,
                        longitude = lng
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
                    val targetStoreId = obj.optString("target_store_id", obj.optString("store_id", null))
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

                    // Parse metadata for pdv_only and hidden
                    var isPdvOnly = false
                    var isHidden = false
                    val metadataObj = item.optJSONObject("metadata")
                    if (metadataObj != null) {
                        isPdvOnly = metadataObj.optBoolean("pdv_only", false)
                        isHidden = metadataObj.optBoolean("hidden", false)
                    } else if (item.has("metadata") && !item.isNull("metadata")) {
                        val metaStr = item.optString("metadata", "")
                        if (metaStr.isNotBlank() && metaStr.startsWith("{")) {
                            try {
                                val parsed = JSONObject(metaStr)
                                isPdvOnly = parsed.optBoolean("pdv_only", false)
                                isHidden = parsed.optBoolean("hidden", false)
                            } catch (_: Exception) {}
                        }
                    }

                    if (isPdvOnly || isHidden) continue

                    val description = item.optString("description", "")
                    val price = item.optDouble("price", 0.0)
                    val category = item.optString("category", "Geral")
                    val sectionId = if (item.has("section_id") && !item.isNull("section_id")) item.optString("section_id", null) else null
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
                    val productId = if (item.has("product_id") && !item.isNull("product_id")) item.optString("product_id", null) else null
                    val sId = if (item.has("store_id") && !item.isNull("store_id")) item.optString("store_id", null) else null

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
                    val cStoreId = obj.optString("store_id", null)
                    
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
                        expiresAt = if (obj.isNull("expires_at")) null else obj.optString("expires_at", null),
                        firstOrderOnly = obj.optBoolean("first_order_only", false),
                        isActive = obj.optBoolean("is_active", true),
                        storeId = if (cStoreId == "null") null else cStoreId
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching coupon", e)
            null
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

    // 8. CREATE ORDER (RETURNS OFFICIAL SUPABASE ID)
    suspend fun submitOrder(order: com.example.data.model.Order): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/orders"

            val bodyJson = JSONObject().apply {
                put("store_id", order.storeId)
                put("store_name", order.storeName)
                put("subtotal", order.subtotal)
                put("delivery_fee", order.deliveryFee)
                put("discount", order.discount)
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
                .addHeader("Prefer", "return=representation")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseText = response.body?.string()
                if (!responseText.isNullOrBlank()) {
                    val array = JSONArray(responseText)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        return@withContext obj.opt("id")?.toString()
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting order", e)
            null
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
