package com.example.ui.orders

import com.example.data.model.DeliveryAddressInput
import com.example.data.model.UserSession

internal const val ACTIVE_LOCATION_TTL_MILLIS = 5 * 60 * 1000L

internal fun isActiveLocationFresh(
    session: UserSession,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val capturedAt = session.activeLocationUpdatedAt
    val latitude = session.activeLocationLatitude
    val longitude = session.activeLocationLongitude
    if (capturedAt <= 0L || latitude == null || longitude == null) return false
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    return nowMillis - capturedAt in 0..ACTIVE_LOCATION_TTL_MILLIS
}

/**
 * Monta somente um endereço inteiramente originado do GPS. O CEP salvo nunca é
 * usado como fallback, porque isso criaria um endereço híbrido com coordenadas diferentes.
 */
internal fun gpsAddressIfComplete(
    session: UserSession,
    nowMillis: Long = System.currentTimeMillis()
): DeliveryAddressInput? {
    if (!isActiveLocationFresh(session, nowMillis)) return null
    val address = DeliveryAddressInput(
        street = session.activeLocationStreet,
        number = session.activeLocationNumber,
        neighborhood = session.activeLocationNeighborhood,
        city = session.activeLocationCity,
        state = session.activeLocationState,
        cep = session.activeLocationCep.filter(Char::isDigit).take(8)
    )
    return address.takeIf { it.isComplete() }
}
