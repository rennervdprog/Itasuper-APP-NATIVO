import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogVisibilityTest {

    private fun store(id: String, planType: String = "essencial") = Store(
        id = id,
        name = "Loja $id",
        category = "Mercado",
        rating = 5.0,
        planType = planType
    )

    @Test
    fun `loja com menos de cinco produtos nao aparece`() {
        val visible = SupabaseClient.keepClientCatalogStores(
            listOf(store("small"), store("valid")),
            mapOf("small" to 4, "valid" to 5)
        )

        assertEquals(listOf("valid"), visible.map { it.id })
    }

    @Test
    fun `loja com exatamente cinco produtos aparece`() {
        val visible = SupabaseClient.keepClientCatalogStores(
            listOf(store("valid")),
            mapOf("valid" to 5)
        )

        assertTrue(visible.any { it.id == "valid" })
    }

    @Test
    fun `loja pdv continua fora da vitrine`() {
        val visible = SupabaseClient.keepClientCatalogStores(
            listOf(store("pdv", "pdv_only")),
            mapOf("pdv" to 10)
        )

        assertTrue(visible.isEmpty())
    }
}
