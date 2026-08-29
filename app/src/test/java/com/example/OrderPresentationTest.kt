import com.example.data.model.Order
import com.example.ui.orders.customerOrderCode
import com.example.ui.orders.customerOrderLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderPresentationTest {

    private fun order(id: String) = Order(
        id = id,
        storeId = "store-1",
        storeName = "Loja Teste",
        items = emptyList(),
        subtotal = 0.0,
        deliveryFee = 0.0,
        total = 0.0,
        paymentMethod = "Cartão",
        deliveryAddress = "Rua Teste, 10",
        createdAt = "2026-08-25 20:00:00"
    )

    @Test
    fun `UUID vira codigo curto para o cliente`() {
        val result = order("167ec7ac-9205-48c6-8a93-0d916af86c37")

        assertEquals("167EC7", result.customerOrderCode())
        assertEquals("Pedido #167EC7", result.customerOrderLabel())
    }

    @Test
    fun `pedido sem id nao exibe identificador tecnico`() {
        assertEquals("PENDENTE", order("").customerOrderCode())
    }
}
