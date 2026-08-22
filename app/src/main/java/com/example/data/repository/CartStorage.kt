package com.example.data.repository

import android.content.Context
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.Product
import com.example.data.model.SelectedAddonItem
import org.json.JSONArray
import org.json.JSONObject

/** Persistência local do carrinho, isolada por usuário autenticado. */
class CartStorage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(userId: String): CartState? {
        if (userId.isBlank()) return null
        val raw = prefs.getString(keyFor(userId), null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val storeId = root.optString("store_id", "").ifBlank { null }
            val itemsJson = root.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (index in 0 until itemsJson.length()) {
                    val item = itemsJson.optJSONObject(index) ?: continue
                    val product = item.optJSONObject("product")?.toProduct() ?: continue
                    val quantity = item.optInt("quantity", 0)
                    if (quantity <= 0) continue
                    val addons = item.optJSONArray("addons")?.toAddons().orEmpty()
                    add(
                        CartItem(
                            product = product,
                            quantity = quantity,
                            notes = item.optString("notes", ""),
                            selectedAddons = addons
                        )
                    )
                }
            }
            if (storeId == null || items.isEmpty()) {
                clear(userId)
                null
            } else {
                CartState(
                    storeId = storeId,
                    storeName = root.optString("store_name", ""),
                    items = items,
                    deliveryType = root.optString("delivery_type", "DELIVERY"),
                    deliveryLatitude = root.takeIf { it.has("delivery_latitude") && !it.isNull("delivery_latitude") }?.optDouble("delivery_latitude"),
                    deliveryLongitude = root.takeIf { it.has("delivery_longitude") && !it.isNull("delivery_longitude") }?.optDouble("delivery_longitude"),
                    storeDeliveryFeeType = root.optString("store_delivery_fee_type", ""),
                    storeOfficialDeliveryFee = root.takeIf { it.has("store_official_delivery_fee") && !it.isNull("store_official_delivery_fee") }?.optDouble("store_official_delivery_fee"),
                    officialDeliveryFee = root.takeIf { it.optString("store_delivery_fee_type", "").equals("fixed", ignoreCase = true) && it.has("store_official_delivery_fee") }?.optDouble("store_official_delivery_fee"),
                    officialDeliveryQuoteKey = root.takeIf { it.optString("store_delivery_fee_type", "").equals("fixed", ignoreCase = true) && it.has("store_official_delivery_fee") }?.optString("store_id")?.takeIf { it.isNotBlank() }?.let { "fixed:$it" },
                    appliedCoupon = root.optJSONObject("coupon")?.toCoupon(),
                    discountAmount = root.optDouble("discount_amount", 0.0).coerceAtLeast(0.0)
                )
            }
        }.getOrElse {
            clear(userId)
            null
        }
    }

    fun save(userId: String, state: CartState) {
        if (userId.isBlank()) return
        if (state.storeId.isNullOrBlank() || state.items.isEmpty()) {
            clear(userId)
            return
        }
        val root = JSONObject().apply {
            put("store_id", state.storeId)
            put("store_name", state.storeName)
            put("delivery_type", state.deliveryType)
            state.deliveryLatitude?.let { put("delivery_latitude", it) }
            state.deliveryLongitude?.let { put("delivery_longitude", it) }
            put("store_delivery_fee_type", state.storeDeliveryFeeType)
            state.storeOfficialDeliveryFee?.let { put("store_official_delivery_fee", it) }
            put("discount_amount", state.discountAmount)
            state.appliedCoupon?.let { put("coupon", it.toJson()) }
            put("items", JSONArray().apply {
                state.items.forEach { cartItem ->
                    put(JSONObject().apply {
                        put("product", cartItem.product.toJson())
                        put("quantity", cartItem.quantity)
                        put("notes", cartItem.notes)
                        put("addons", JSONArray().apply {
                            cartItem.selectedAddons.forEach { addon -> put(addon.toJson()) }
                        })
                    })
                }
            })
        }
        prefs.edit().putString(keyFor(userId), root.toString()).apply()
    }

    fun clear(userId: String) {
        if (userId.isBlank()) return
        prefs.edit().remove(keyFor(userId)).apply()
    }

    private fun keyFor(userId: String) = "cart_$userId"

    private fun JSONObject.toProduct() = Product(
        id = optString("id", ""),
        storeId = optString("store_id", ""),
        name = optString("name", "Produto"),
        description = optString("description", ""),
        price = optDouble("price", 0.0),
        originalPrice = if (has("original_price") && !isNull("original_price")) optDouble("original_price") else null,
        category = optString("category", ""),
        sectionId = optString("section_id", "").ifBlank { null },
        imageUrl = optString("image_url", ""),
        isAvailable = optBoolean("is_available", true),
        hasStuffedCrust = optBoolean("has_stuffed_crust", false),
        isCombo = optBoolean("is_combo", false),
        isPastelFlavor = optBoolean("is_pastel_flavor", false),
        isBeverage = optBoolean("is_beverage", false),
        requiresPrescription = optBoolean("requires_prescription", false),
        isControlled = optBoolean("controlled", false),
        pharmacySaleMode = optString("pharmacy_sale_mode", "platform_checkout"),
        pharmacyType = optString("pharmacy_type", ""),
        activeIngredient = optString("active_ingredient", ""),
        dosage = optString("dosage", ""),
        pharmaForm = optString("pharma_form", ""),
        manufacturer = optString("manufacturer", ""),
        packQuantity = optString("pack_quantity", ""),
        isGeneric = optBoolean("is_generic", false)
    ).takeIf { it.id.isNotBlank() && it.storeId.isNotBlank() }

    private fun Product.toJson() = JSONObject().apply {
        put("id", id)
        put("store_id", storeId)
        put("name", name)
        put("description", description)
        put("price", price)
        originalPrice?.let { put("original_price", it) }
        put("category", category)
        sectionId?.let { put("section_id", it) }
        put("image_url", imageUrl)
        put("is_available", isAvailable)
        put("has_stuffed_crust", hasStuffedCrust)
        put("is_combo", isCombo)
        put("is_pastel_flavor", isPastelFlavor)
        put("is_beverage", isBeverage)
        put("requires_prescription", requiresPrescription)
        put("controlled", isControlled)
        put("pharmacy_sale_mode", pharmacySaleMode)
        put("pharmacy_type", pharmacyType)
        put("active_ingredient", activeIngredient)
        put("dosage", dosage)
        put("pharma_form", pharmaForm)
        put("manufacturer", manufacturer)
        put("pack_quantity", packQuantity)
        put("is_generic", isGeneric)
    }

    private fun JSONArray.toAddons() = buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val id = item.optString("item_id", "")
            if (id.isBlank()) continue
            add(
                SelectedAddonItem(
                    itemId = id,
                    itemName = item.optString("item_name", ""),
                    itemPrice = item.optDouble("item_price", 0.0),
                    groupId = item.optString("group_id", ""),
                    groupName = item.optString("group_name", ""),
                    priceReplacesBase = item.optBoolean("price_replaces_base", false)
                )
            )
        }
    }

    private fun SelectedAddonItem.toJson() = JSONObject().apply {
        put("item_id", itemId)
        put("item_name", itemName)
        put("item_price", itemPrice)
        put("group_id", groupId)
        put("group_name", groupName)
        put("price_replaces_base", priceReplacesBase)
    }

    private fun JSONObject.toCoupon() = Coupon(
        id = optString("id", ""),
        code = optString("code", ""),
        discountType = optString("discount_type", "fixed"),
        discountValue = optDouble("discount_value", 0.0),
        minOrderValue = optDouble("min_order_value", 0.0),
        expiresAt = optString("expires_at", "").ifBlank { null },
        firstOrderOnly = optBoolean("first_order_only", false),
        isActive = optBoolean("is_active", true),
        storeId = optString("store_id", "").ifBlank { null }
    ).takeIf { it.code.isNotBlank() }

    private fun Coupon.toJson() = JSONObject().apply {
        put("id", id)
        put("code", code)
        put("discount_type", discountType)
        put("discount_value", discountValue)
        put("min_order_value", minOrderValue)
        expiresAt?.let { put("expires_at", it) }
        put("first_order_only", firstOrderOnly)
        put("is_active", isActive)
        storeId?.let { put("store_id", it) }
    }

    private companion object {
        const val PREFS_NAME = "itasuper_cart_storage"
    }
}
