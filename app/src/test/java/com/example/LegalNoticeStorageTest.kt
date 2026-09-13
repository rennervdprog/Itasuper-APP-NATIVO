package com.example

import com.example.data.repository.LegalNoticeStorage
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LegalNoticeStorageTest {

    @Before
    fun reset() = LegalNoticeStorage.clear()

    @After
    fun cleanup() = LegalNoticeStorage.clear()

    @Test
    fun `adiamento vale apenas para o par de versoes adiado`() {
        LegalNoticeStorage.dismiss("6.6/6.6")
        assertTrue(LegalNoticeStorage.isDismissed("6.6/6.6"))
        assertFalse(LegalNoticeStorage.isDismissed("6.7/6.7"))
    }

    @Test
    fun `chave vazia nunca conta como adiada`() {
        LegalNoticeStorage.dismiss("")
        assertFalse(LegalNoticeStorage.isDismissed(""))
    }
}
