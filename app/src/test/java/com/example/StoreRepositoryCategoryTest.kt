import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryCategoryTest {

    private fun store(category: String, secondaryCategories: List<String> = emptyList()) = Store(
        id = "store-1",
        name = "Loja de bebidas",
        category = category,
        secondaryCategories = secondaryCategories,
        rating = 5.0
    )

    @Test
    fun `bebidas inclui loja classificada como adega`() {
        assertTrue(StoreRepository.matchesCategory(store("Adega"), "bebidas"))
    }

    @Test
    fun `bebidas inclui loja classificada como adegas`() {
        assertTrue(StoreRepository.matchesCategory(store("Adegas"), "bebidas"))
    }

    @Test
    fun `bebidas inclui adega em categoria secundaria`() {
        assertTrue(
            StoreRepository.matchesCategory(
                store(category = "Mercado", secondaryCategories = listOf("Adega")),
                "bebidas"
            )
        )
    }

    @Test
    fun `bebidas nao inclui categoria sem relacao`() {
        assertFalse(StoreRepository.matchesCategory(store("Pizzaria"), "bebidas"))
    }
}
