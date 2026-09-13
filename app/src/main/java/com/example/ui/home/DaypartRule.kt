package com.example.ui.home

/**
 * Faixa de horário usada para destacar na Home o que faz sentido pedir agora.
 *
 * O destaque nunca esconde loja: a lista regional completa continua abaixo.
 * Isso é regra de negócio, não estética — quem anuncia paga mensalidade e
 * sumir do catálogo em certas horas não é aceitável.
 */
data class Daypart(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    /** Categorias do catálogo que entram no destaque, da mais forte para a mais fraca. */
    val categorias: List<String>
) {
    /** Categoria levada para a Busca quando o cliente toca em "Ver todas". */
    val categoriaPrimaria: String get() = categorias.first()
}

/**
 * As faixas ficam no código de propósito: são regra de produto, mudam junto com
 * a UI e não precisam de ida ao banco para serem lidas na abertura da Home.
 */
object DaypartRule {

    private val MADRUGADA = Daypart(
        id = "madrugada",
        titulo = "Aberto de madrugada",
        subtitulo = "Quem ainda está atendendo agora",
        categorias = listOf("lanches", "bebidas", "farmacias")
    )

    private val MANHA = Daypart(
        id = "manha",
        titulo = "Bom dia",
        subtitulo = "Para resolver o começo do dia",
        categorias = listOf("mercado", "pastel", "farmacias")
    )

    private val ALMOCO = Daypart(
        id = "almoco",
        titulo = "Hora do almoço",
        subtitulo = "Pratos prontos perto de você",
        categorias = listOf("marmita", "churrasco", "pastel")
    )

    private val TARDE = Daypart(
        id = "tarde",
        titulo = "Lanche da tarde",
        subtitulo = "Um docinho ou um salgado",
        categorias = listOf("acai", "pastel", "lanches")
    )

    private val NOITE = Daypart(
        id = "noite",
        titulo = "Hora do jantar",
        subtitulo = "O que pedir hoje à noite",
        categorias = listOf("pizzaria", "lanches", "bebidas")
    )

    /**
     * Recebe o minuto do dia (0..1439) para poder ser testado sem relógio real.
     * Valor fora da faixa cai na noite, que é o horário de maior movimento.
     */
    fun forMinuteOfDay(minuteOfDay: Int): Daypart = when {
        minuteOfDay < 0 -> NOITE
        minuteOfDay < 6 * 60 -> MADRUGADA
        minuteOfDay < 10 * 60 + 30 -> MANHA
        minuteOfDay < 15 * 60 -> ALMOCO
        minuteOfDay < 18 * 60 -> TARDE
        minuteOfDay < 24 * 60 -> NOITE
        else -> NOITE
    }

    /**
     * Mesmo fuso que `SupabaseClient.checkIsStoreOpenNow` usa para decidir se a
     * loja está aberta. Usar outro relógio faria o destaque contradizer o selo
     * de "aberto" da própria loja.
     *
     * Calendar em vez de java.time porque o app roda a partir do minSdk 24 e
     * não há desugaring configurado.
     */
    fun current(): Daypart {
        val calendar = java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone("America/Sao_Paulo")
        )
        val minuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(java.util.Calendar.MINUTE)
        return forMinuteOfDay(minuteOfDay)
    }
}
