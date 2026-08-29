package com.example.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class CurrentLocationAddress(
    val latitude: Double,
    val longitude: Double,
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val cep: String = ""
)

/**
 * Obtém uma posição atual, e não uma posição antiga mantida pelo provedor do aparelho,
 * e tenta convertê-la em um endereço utilizável pelo checkout.
 */
object CurrentLocationProvider {
    suspend fun getCurrentAddress(context: Context): CurrentLocationAddress? {
        val location = getCurrentLocation(context) ?: return null
        var street = ""
        var number = ""
        var neighborhood = ""
        var city = ""
        var state = ""
        var cep = ""
        try {
            val address = withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context.applicationContext, java.util.Locale("pt", "BR"))
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            }
            if (address != null) {
                street = address.thoroughfare.orEmpty().trim()
                number = address.subThoroughfare.orEmpty().trim()
                neighborhood = (address.subLocality ?: address.subAdminArea).orEmpty().trim()
                city = (address.locality ?: address.subAdminArea ?: address.adminArea).orEmpty().trim()
                state = address.adminArea.orEmpty().trim()
                cep = address.postalCode.orEmpty().filter(Char::isDigit).take(8)
            }
        } catch (_: Exception) {
            // A coordenada ainda pode ser usada para catálogo, mas não vira um endereço inventado.
        }
        return CurrentLocationAddress(
            latitude = location.latitude,
            longitude = location.longitude,
            street = street,
            number = number,
            neighborhood = neighborhood,
            city = city,
            state = state,
            cep = cep
        )
    }

    private suspend fun getCurrentLocation(context: Context): Location? {
        val appContext = context.applicationContext
        val hasFine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            val client = LocationServices.getFusedLocationProviderClient(appContext)
            client.getCurrentLocation(
                if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellation.token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location?.takeIf(::isUsable))
            }.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }

    private fun isUsable(location: Location): Boolean {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return false
        return !location.hasAccuracy() || location.accuracy <= MAX_ACCEPTED_ACCURACY_METERS
    }

    private const val MAX_ACCEPTED_ACCURACY_METERS = 250f
}
