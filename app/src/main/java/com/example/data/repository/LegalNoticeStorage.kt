package com.example.data.repository

/**
 * Guarda o adiamento de um aviso prévio legal (modo `notice`) por par de versões,
 * no mesmo formato do web: a chave é "termos/privacidade" (ex.: "6.6/6.6").
 *
 * O adiamento é de sessão, como no web: ao reabrir o app o aviso aparece de novo,
 * e ele desaparece sozinho quando a versão avança. Nada aqui afeta o modo `binding` —
 * quando a vigência chega, o backend responde `binding` e o gate volta a bloquear.
 */
object LegalNoticeStorage {

    private val dismissedVersionKeys = mutableSetOf<String>()

    fun isDismissed(versionKey: String): Boolean =
        versionKey.isNotBlank() && dismissedVersionKeys.contains(versionKey)

    fun dismiss(versionKey: String) {
        if (versionKey.isBlank()) return
        dismissedVersionKeys.add(versionKey)
    }

    fun clear() {
        dismissedVersionKeys.clear()
    }
}
