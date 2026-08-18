package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.SupabaseClient
import com.example.data.repository.UserSessionRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * Centraliza o contrato FCM do cliente Android. O token é associado ao usuário autenticado
 * pela mesma Edge Function `register-push-device` utilizada pelo Capacitor.
 */
object PushNotificationManager {
    const val ORDER_CHANNEL_ID = "itasuper_orders"
    const val EXTRA_DESTINATION = "itasuper_push_destination"
    const val DESTINATION_ORDERS = "pedidos"
    private const val PUSH_PREFS = "itasuper_push_session"
    private const val DEVICE_ID_KEY = "device_id"
    private const val PENDING_DESTINATION_KEY = "pending_destination"
    private const val PENDING_ORDER_ID_KEY = "pending_order_id"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private val _pendingDestination = MutableStateFlow<String?>(null)
    val pendingDestination: StateFlow<String?> = _pendingDestination.asStateFlow()
    private val _pendingOrderId = MutableStateFlow<String?>(null)
    val pendingOrderId: StateFlow<String?> = _pendingOrderId.asStateFlow()

    fun createOrderNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ORDER_CHANNEL_ID,
            "Atualizações de pedidos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos sobre preparo, entrega e conclusão dos seus pedidos"
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /** Obtém o token atual e o vincula à conta autenticada quando houver permissão. */
    fun registerCurrentDevice(context: Context) {
        if (!hasNotificationPermission(context)) return
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.accessToken.isBlank()) return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> registerFcmToken(context.applicationContext, token) }
            .addOnFailureListener { /* O Firebase fará nova tentativa quando o token estiver disponível. */ }
    }

    fun registerFcmToken(context: Context, token: String) {
        val session = UserSessionRepository.userSession.value
        if (token.isBlank() || !session.isLoggedIn || session.accessToken.isBlank()) return
        val payload = JSONObject().apply {
            put("fcm_token", token)
            put("device_info", "itasuper:native:android:${getOrCreateDeviceId(context)}")
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        scope.launch {
            try {
                val request = Request.Builder()
                    .url("${SupabaseClient.SUPABASE_URL}/functions/v1/register-push-device")
                    .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload)
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.w("PushNotification", "Não foi possível registrar FCM: HTTP ${response.code}")
                    }
                }
            } catch (error: Exception) {
                android.util.Log.w("PushNotification", "Falha ao registrar token FCM", error)
            }
        }
    }

    fun showOrderNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        createOrderNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val destination = when (data["link"] ?: data["click_action"]) {
            null, "", "/pedidos" -> DESTINATION_ORDERS
            else -> DESTINATION_ORDERS
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, destination)
            data["order_id"]?.let { putExtra("order_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            data["order_id"]?.hashCode() ?: title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "ItaSuper" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            data["order_id"]?.hashCode() ?: System.currentTimeMillis().toInt(),
            notification
        )
    }

    fun captureLaunchIntent(context: Context, intent: Intent?) {
        val rawDestination = intent?.getStringExtra(EXTRA_DESTINATION)
            ?: intent?.getStringExtra("link")
            ?: intent?.getStringExtra("click_action")
            ?: return
        val destination = when {
            rawDestination.contains("pedidos", ignoreCase = true) -> DESTINATION_ORDERS
            else -> DESTINATION_ORDERS
        }
        val orderId = intent?.getStringExtra("order_id")?.takeIf { it.isNotBlank() }
        context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PENDING_DESTINATION_KEY, destination)
            .apply {
                if (orderId != null) putString(PENDING_ORDER_ID_KEY, orderId)
                else remove(PENDING_ORDER_ID_KEY)
            }
            .apply()
        _pendingDestination.value = destination
        _pendingOrderId.value = orderId
    }

    fun consumePendingDestination(context: Context): String? {
        val preferences = context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
        val destination = _pendingDestination.value ?: preferences.getString(PENDING_DESTINATION_KEY, null)
        _pendingDestination.value = null
        preferences.edit().remove(PENDING_DESTINATION_KEY).apply()
        return destination
    }

    fun consumePendingOrderId(context: Context): String? {
        val preferences = context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
        val orderId = _pendingOrderId.value ?: preferences.getString(PENDING_ORDER_ID_KEY, null)
        _pendingOrderId.value = null
        preferences.edit().remove(PENDING_ORDER_ID_KEY).apply()
        return orderId
    }

    private fun getOrCreateDeviceId(context: Context): String {
        val preferences: SharedPreferences = context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
        val current = preferences.getString(DEVICE_ID_KEY, null)
        if (!current.isNullOrBlank()) return current
        return UUID.randomUUID().toString().also { generated ->
            preferences.edit().putString(DEVICE_ID_KEY, generated).apply()
        }
    }
}
