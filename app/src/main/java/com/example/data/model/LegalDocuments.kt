package com.example.data.model

enum class LegalDocumentKind {
    TERMS,
    PRIVACY
}

object LegalDocumentLinks {
    const val TERMS_URL = "https://itasuper.com.br/termos-de-uso"
    const val PRIVACY_URL = "https://itasuper.com.br/politica-de-privacidade"
}

data class LegalChange(
    val version: String,
    val effectiveDate: String,
    val section: String,
    val changeType: String,
    val summary: String,
    val legalBasis: String?
)

data class PendingLegalChanges(
    val needsTerms: Boolean,
    val needsPrivacy: Boolean,
    val currentTermsVersion: String,
    val currentPrivacyVersion: String,
    val termsChanges: List<LegalChange>,
    val privacyChanges: List<LegalChange>
) {
    val requiresAcceptance: Boolean
        get() = needsTerms || needsPrivacy
}

data class LegalAcceptanceResult(
    val success: Boolean,
    val errorMessage: String? = null
)
