package com.example

import com.example.data.model.LegalAcceptanceMode
import com.example.data.model.PendingLegalChanges
import com.example.data.model.compareLegalVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalConsentPolicyTest {

    private fun pending(
        mode: LegalAcceptanceMode = LegalAcceptanceMode.BINDING,
        needsTerms: Boolean = true,
        needsPrivacy: Boolean = false,
        termsVersion: String = "6.6",
        privacyVersion: String = "6.6"
    ) = PendingLegalChanges(
        needsTerms = needsTerms,
        needsPrivacy = needsPrivacy,
        currentTermsVersion = termsVersion,
        currentPrivacyVersion = privacyVersion,
        termsChanges = emptyList(),
        privacyChanges = emptyList(),
        mode = mode
    )

    @Test
    fun `modo ausente ou desconhecido e tratado como binding`() {
        assertEquals(LegalAcceptanceMode.BINDING, LegalAcceptanceMode.fromRemote(null))
        assertEquals(LegalAcceptanceMode.BINDING, LegalAcceptanceMode.fromRemote(""))
        assertEquals(LegalAcceptanceMode.BINDING, LegalAcceptanceMode.fromRemote("binding"))
        assertEquals(LegalAcceptanceMode.BINDING, LegalAcceptanceMode.fromRemote("qualquer-coisa"))
    }

    @Test
    fun `somente notice explicito virou notice`() {
        assertEquals(LegalAcceptanceMode.NOTICE, LegalAcceptanceMode.fromRemote("notice"))
        assertEquals(LegalAcceptanceMode.NOTICE, LegalAcceptanceMode.fromRemote(" NOTICE "))
    }

    @Test
    fun `aceite obrigatorio bloqueia a tela`() {
        assertTrue(pending(mode = LegalAcceptanceMode.BINDING).isBlocking)
    }

    @Test
    fun `aviso previo nunca bloqueia a tela`() {
        assertFalse(pending(mode = LegalAcceptanceMode.NOTICE).isBlocking)
    }

    @Test
    fun `sem mudanca pendente nada bloqueia`() {
        val nothingPending = pending(needsTerms = false, needsPrivacy = false)
        assertFalse(nothingPending.requiresAcceptance)
        assertFalse(nothingPending.isBlocking)
    }

    @Test
    fun `chave de adiamento usa o par de versoes como no web`() {
        assertEquals("6.6/6.6", pending().versionKey)
        assertEquals("6.6/6.5", pending(privacyVersion = "6.5").versionKey)
    }

    @Test
    fun `comparacao de versao e numerica e nao lexicografica`() {
        assertTrue(compareLegalVersions("10.0", "4.2") > 0)
        assertTrue(compareLegalVersions("6.6", "6.10") < 0)
        assertEquals(0, compareLegalVersions("6.6", "6.6.0"))
        assertTrue(compareLegalVersions("6.6", "6.5") > 0)
    }

    @Test
    fun `versao ausente conta como zero`() {
        assertTrue(compareLegalVersions("6.6", null) > 0)
        assertTrue(compareLegalVersions(null, "6.6") < 0)
        assertEquals(0, compareLegalVersions(null, ""))
    }

    @Test
    fun `modo enviado ao banco preserva o valor do contrato`() {
        assertEquals("binding", LegalAcceptanceMode.BINDING.remoteValue)
        assertEquals("notice", LegalAcceptanceMode.NOTICE.remoteValue)
    }

    @Test
    fun `data de vigencia cai para a da privacidade quando a dos termos falta`() {
        val onlyPrivacy = pending().copy(termsEffectiveDate = "", privacyEffectiveDate = "2026-10-13T00:00:00-03:00")
        assertEquals("2026-10-13T00:00:00-03:00", onlyPrivacy.effectiveDate)
    }
}
