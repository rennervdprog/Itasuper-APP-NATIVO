import com.example.data.model.UserSession
import com.example.ui.orders.gpsAddressIfComplete
import com.example.ui.orders.isActiveLocationFresh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutLocationPolicyTest {

    private val now = 1_700_000_000_000L

    private fun session(
        activeCep: String = "01311000",
        capturedAt: Long = now,
        activeStreet: String = "Avenida Paulista",
        savedCep: String = "22222222"
    ) = UserSession(
        addressCep = savedCep,
        activeLocationStreet = activeStreet,
        activeLocationNumber = "1000",
        activeLocationNeighborhood = "Bela Vista",
        activeLocationCity = "São Paulo",
        activeLocationState = "SP",
        activeLocationCep = activeCep,
        activeLocationLatitude = -23.5617,
        activeLocationLongitude = -46.6560,
        activeLocationUpdatedAt = capturedAt
    )

    @Test
    fun `GPS completo monta endereco somente com dados do GPS`() {
        val address = gpsAddressIfComplete(session(), now)

        assertNotNull(address)
        assertEquals("01311000", address?.cep)
        assertEquals("Avenida Paulista", address?.street)
    }

    @Test
    fun `GPS sem CEP nao usa CEP salvo como fallback`() {
        val address = gpsAddressIfComplete(session(activeCep = ""), now)

        assertNull(address)
    }

    @Test
    fun `GPS sem rua nao e considerado endereco completo`() {
        val address = gpsAddressIfComplete(session(activeStreet = ""), now)

        assertNull(address)
    }

    @Test
    fun `localizacao expirada nao e considerada atual`() {
        val expired = session(capturedAt = now - 5 * 60 * 1000L - 1L)

        assertFalse(isActiveLocationFresh(expired, now))
        assertNull(gpsAddressIfComplete(expired, now))
    }

    @Test
    fun `localizacao dentro do TTL e considerada atual`() {
        val fresh = session(capturedAt = now - 5 * 60 * 1000L)

        assertTrue(isActiveLocationFresh(fresh, now))
    }
}
