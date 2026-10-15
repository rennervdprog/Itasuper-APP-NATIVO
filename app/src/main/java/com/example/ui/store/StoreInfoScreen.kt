package com.example.ui.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Store
import com.example.data.model.StoreOpeningHour
import com.example.data.repository.FavoriteStoresStorage
import androidx.compose.material.icons.filled.Favorite
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import java.util.Locale

@Composable
fun StoreInfoScreen(
    storeId: String,
    viewModel: StoreDetailViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(storeId) { viewModel.loadStore(storeId) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val favoriteStorage = remember(context) { FavoriteStoresStorage(context) }
    val favoriteIds by favoriteStorage.favorites.collectAsState()
    var selectedTab by remember { mutableStateOf(1) } // 0 = Avaliações, 1 = Informações
    var ratingAgg by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var reviewsList by remember { mutableStateOf<org.json.JSONArray>(org.json.JSONArray()) }
    var reviewsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(storeId, selectedTab) {
        if (selectedTab == 0 && storeId.isNotBlank()) {
            reviewsLoading = true
            try {
                ratingAgg = com.example.data.remote.SupabaseClient.getStoreRatingAggregate(storeId)
                reviewsList = com.example.data.remote.SupabaseClient.getStoreReviews(storeId)
            } catch (e: Exception) { /* mantém vazio */ }
            reviewsLoading = false
        }
    }

    Scaffold(containerColor = Color(0xFFF7F7F8)) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ItaSuperPrimary) }
        } else {
            val store = uiState.store
            val address = store?.displayAddress().orEmpty()
            val payments = store?.paymentMethods().orEmpty()
            val hours = store?.openingHours.orEmpty().sortedWith(
                compareBy<StoreOpeningHour> { it.brasiliaDayOrder() }.thenBy { it.openTime }
            )
            val aggAvgTop = ratingAgg?.optDouble("avg_rating", 0.0) ?: 0.0
            val aggCountTop = ratingAgg?.optInt("review_count", 0) ?: 0

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                // Topo: voltar | nome | compartilhar + favoritar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = ItaSuperPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(onClick = onBackClick)
                                .padding(8.dp)
                        )
                        Text(
                            text = (store?.name ?: "").uppercase(Locale.ROOT),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        val isFav = favoriteIds.contains(storeId)
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = ItaSuperPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { favoriteStorage.toggleFavorite(storeId) }
                                .padding(8.dp)
                        )
                    }
                }

                // Nome + logo + categoria • distância
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store?.name ?: "",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1A1A),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    val cat = store?.category?.takeIf { it.isNotBlank() }
                                        ?.replaceFirstChar { it.uppercase() } ?: ""
                                    append(cat)
                                    val d = store?.distanceKm ?: 0.0
                                    if (d > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append(formatDistance(d))
                                    }
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF777777)
                            )
                        }
                        val logoUrl = store?.logoUrl
                        if (!logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = store?.name?.firstOrNull()?.uppercase() ?: "",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary
                                )
                            }
                        }
                    }
                }

                // Avaliação: nota + estrelas | nº avaliações
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (aggCountTop > 0) String.format("%.1f", aggAvgTop).replace(".", ",") else "–",
                                fontSize = 15.sp,
                                color = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(5) { i ->
                                    Text(
                                        text = "★",
                                        fontSize = 15.sp,
                                        color = if (aggAvgTop > i) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (aggCountTop == 1) "1 avaliação" else "$aggCountTop avaliações",
                            fontSize = 14.sp,
                            color = Color(0xFF777777)
                        )
                    }
                }

                // Abas: Avaliações | Informações
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTab = 0 }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Avaliações",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == 0) ItaSuperPrimary else Color(0xFF999999)
                                )
                            }
                            if (selectedTab == 0) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ItaSuperPrimary))
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTab = 1 }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Informações",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == 1) ItaSuperPrimary else Color(0xFF999999)
                                )
                            }
                            if (selectedTab == 1) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ItaSuperPrimary))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }

                // Conteúdo da aba Avaliações
                if (selectedTab == 0) {
                    item {
                        StoreReviewsContent(
                            ratingAgg = ratingAgg,
                            reviewsList = reviewsList,
                            isLoading = reviewsLoading
                        )
                    }
                }

                // Conteúdo da aba Informações — Opção A2 (Mapa Destaque)
                if (selectedTab == 1) {
                    val isOpen = store?.isOpen == true

                    // Mapa hero com chip da loja
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                                .height(190.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDCEFE0))
                                .clickable {
                                    if (address.isNotBlank()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                                )
                                            )
                                        }
                                    }
                                }
                        ) {
                            // Pin central
                            Text(
                                text = "📍",
                                fontSize = 40.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            // Chip da loja
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val logoUrl = store?.logoUrl
                                if (!logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1A1A1A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = store?.name?.firstOrNull()?.uppercase() ?: "",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = store?.name ?: "",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = buildString {
                                            val cat = store?.category?.takeIf { it.isNotBlank() }
                                                ?.replaceFirstChar { it.uppercase() } ?: ""
                                            append(cat)
                                            val d = store?.distanceKm ?: 0.0
                                            if (d > 0) {
                                                if (isNotEmpty()) append(" · ")
                                                append(formatDistance(d))
                                            }
                                        },
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF777777)
                                    )
                                }
                            }
                            // Botão traçar rota
                            if (address.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFF1A1A1A))
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Traçar rota →",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Card Funcionamento
                    item {
                        InfoCard(title = "🕐", titleText = "Funcionamento") {
                            // Status pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        if (isOpen) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOpen) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                )
                                Spacer(modifier = Modifier.width(9.dp))
                                Text(
                                    text = if (isOpen) "Aberto agora" else "Fechado agora",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOpen) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                val nextInfo = nextOpeningInfo(hours, isOpen)
                                if (nextInfo.isNotBlank()) {
                                    Text(
                                        text = nextInfo,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF777777)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Horários agrupados
                            val grouped = groupHoursByRange(hours)
                            if (grouped.isEmpty()) {
                                Text(
                                    text = "Horários não informados pela loja.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF999999)
                                )
                            } else {
                                grouped.forEach { (label, range) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF777777)
                                        )
                                        Text(
                                            text = range,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card Pagamento
                    item {
                        InfoCard(title = "💳", titleText = "Pagamento") {
                            if (payments.isEmpty()) {
                                Text(
                                    text = "Formas de pagamento não informadas pela loja.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF999999)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    payments.chunked(2).forEach { rowMethods ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowMethods.forEach { m ->
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(13.dp))
                                                        .background(Color(0xFFF9FAFB))
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    PaymentBadge(method = m)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = paymentDisplayName(m),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF374151)
                                                    )
                                                }
                                            }
                                            // Preenche a grade se ímpar
                                            if (rowMethods.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card Endereço
                    item {
                        InfoCard(title = "📍", titleText = "Endereço") {
                            Text(
                                text = address.ifBlank { "Endereço não informado pela loja." },
                                fontSize = 14.sp,
                                color = Color(0xFF374151),
                                lineHeight = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } // fim if (selectedTab == 1)
            }
        }
    }
}

@Composable
private fun PaymentBrandRow(label: String, methods: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = Color(0xFF777777))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            methods.take(4).forEach { m ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF0F0F0)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) {
                        Text(
                            text = brandShort(m),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
            if (methods.size > 4) {
                Text(text = "+", fontSize = 16.sp, color = Color(0xFF999999))
            }
        }
    }
}

private fun brandShort(method: String): String {
    val m = method.lowercase(Locale.ROOT)
    return when {
        m.contains("pix") -> "PIX"
        m.contains("visa") -> "VISA"
        m.contains("master") -> "MASTER"
        m.contains("elo") -> "ELO"
        m.contains("hiper") -> "HIPER"
        m.contains("amex") || m.contains("american") -> "AMEX"
        m.contains("dinheiro") -> "R$"
        m.contains("debito") || m.contains("débito") -> "DÉB"
        m.contains("credito") || m.contains("crédito") -> "CRÉD"
        else -> method.take(4).uppercase(Locale.ROOT)
    }
}

private fun isOnlinePayment(method: String): Boolean {
    val m = method.lowercase(Locale.ROOT)
    return m.contains("pix") || m.contains("online") || m.contains("app") || m.contains("itasuper")
}

private fun isToday(h: StoreOpeningHour): Boolean {
    val cal = java.util.Calendar.getInstance()
    val todayDow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    return h.dayOfWeek == todayDow
}

private fun formatDistance(km: Double): String {
    return if (km < 1) "${(km * 1000).toInt()} m"
    else String.format("%.1f", km).replace(".", ",") + " km"
}

fun Store.paymentMethods(): List<String> = buildList {
    if (settings.acceptPixOnline) add("PIX online")
    if (settings.acceptPixMachine) add("PIX na maquininha")
    if (settings.acceptCard) add("Cartão")
    if (settings.acceptCash) add("Dinheiro")
}


@Composable
private fun InfoCard(
    title: String,
    titleText: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = titleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A),
                letterSpacing = (-0.2).sp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun PaymentBadge(method: String) {
    val m = method.lowercase(java.util.Locale.ROOT)
    val (bg, label) = when {
        m.contains("visa") -> Color(0xFF1A1F71) to "VISA"
        m.contains("master") -> Color(0xFFEB001B) to "MC"
        m.contains("elo") -> Color(0xFF0066B3) to "ELO"
        m.contains("hiper") -> Color(0xFFED1C24) to "HP"
        m.contains("amex") || m.contains("american") -> Color(0xFF2E77BC) to "AMEX"
        m.contains("pix") -> Color(0xFF32BCAD) to "PIX"
        m.contains("dinheiro") -> Color(0xFF4CAF50) to "R$"
        m.contains("debito") || m.contains("débito") -> Color(0xFF5C6BC0) to "DÉB"
        m.contains("credito") || m.contains("crédito") -> Color(0xFF7B1FA2) to "CRÉD"
        else -> Color(0xFF9E9E9E) to method.take(2).uppercase(java.util.Locale.ROOT)
    }
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

private fun paymentDisplayName(method: String): String {
    val m = method.lowercase(java.util.Locale.ROOT)
    return when {
        m.contains("pix") && m.contains("online") -> "Pix online"
        m.contains("pix") && (m.contains("maquininha") || m.contains("machine")) -> "Pix na maquininha"
        m.contains("pix") -> "Pix"
        m.contains("visa") -> "Visa"
        m.contains("master") -> "Mastercard"
        m.contains("elo") -> "Elo"
        m.contains("hiper") -> "Hipercard"
        m.contains("amex") || m.contains("american") -> "Amex"
        m.contains("dinheiro") -> "Dinheiro"
        m.contains("debito") || m.contains("débito") -> "Débito"
        m.contains("credito") || m.contains("crédito") -> "Crédito"
        else -> method.replaceFirstChar { it.uppercase() }
    }
}

private fun formatTimeShort(time: String): String {
    // "08:00:00" -> "08:00"
    return time.trim().split(":").take(2).joinToString(":")
}

private fun nextOpeningInfo(hours: List<StoreOpeningHour>, isOpen: Boolean): String {
    if (isOpen || hours.isEmpty()) return ""
    // Simplificado: informa o próximo horário de hoje ou amanhã
    val today = hours.firstOrNull { isToday(it) }
    return if (today != null && !today.isClosedAllDay) {
        "Abre hoje às ${formatTimeShort(today.openTime)}"
    } else {
        "Abre amanhã às 08:00"
    }
}

private fun groupHoursByRange(hours: List<StoreOpeningHour>): List<Pair<String, String>> {
    if (hours.isEmpty()) return emptyList()
    // Agrupa dias consecutivos com o mesmo horário
    val sorted = hours.sortedBy { it.brasiliaDayOrder() }
    val result = mutableListOf<Pair<String, String>>()
    var groupStart = sorted[0]
    var groupEnd = sorted[0]
    fun rangeLabel(start: StoreOpeningHour, end: StoreOpeningHour): String {
        val s = start.brasiliaDayLabel()
        val e = end.brasiliaDayLabel()
        return if (s == e) s else "$s – $e"
    }
    fun timeRange(h: StoreOpeningHour): String {
        return if (h.isClosedAllDay) "Fechado"
        else "${formatTimeShort(h.openTime)} – ${formatTimeShort(h.closeTime)}"
    }
    for (i in 1 until sorted.size) {
        val h = sorted[i]
        if (h.brasiliaDayOrder() == groupEnd.brasiliaDayOrder() + 1 && timeRange(h) == timeRange(groupEnd)) {
            groupEnd = h
        } else {
            result.add(rangeLabel(groupStart, groupEnd) to timeRange(groupEnd))
            groupStart = h
            groupEnd = h
        }
    }
    result.add(rangeLabel(groupStart, groupEnd) to timeRange(groupEnd))
    // Expande abreviações para nomes completos curtos
    return result.map { (label, range) ->
        val full = label
            .replace("Seg", "Segunda").replace("Ter", "Terça")
            .replace("Qua", "Quarta").replace("Qui", "Quinta")
            .replace("Sex", "Sexta").replace("Sáb", "Sábado")
            .replace("Dom", "Domingo")
        full to range
    }
}

@Composable
private fun StoreReviewsContent(
    ratingAgg: org.json.JSONObject?,
    reviewsList: org.json.JSONArray,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ItaSuperPrimary)
            }
            return
        }

        val avgRating = ratingAgg?.optDouble("avg_rating", 0.0) ?: 0.0
        val reviewCount = ratingAgg?.optInt("review_count", 0) ?: 0

        if (reviewCount == 0) {
            // Estado vazio
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "★", fontSize = 40.sp, color = Color(0xFFDDDDDD))
                Text(
                    text = "Nenhuma avaliação ainda",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "Seja o primeiro a avaliar esta loja!",
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
            return
        }

        // Resumo: nota + histograma
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", avgRating),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= avgRating.toInt()) "★" else "☆",
                            fontSize = 14.sp,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
                Text(
                    text = "$reviewCount avaliações",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dists = listOf(
                    5 to ratingAgg?.optInt("dist_5", 0),
                    4 to ratingAgg?.optInt("dist_4", 0),
                    3 to ratingAgg?.optInt("dist_3", 0),
                    2 to ratingAgg?.optInt("dist_2", 0),
                    1 to ratingAgg?.optInt("dist_1", 0)
                )
                val maxDist = dists.maxOf { it.second ?: 0 }.coerceAtLeast(1)
                dists.forEach { (star, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "$star", fontSize = 11.sp, color = Color(0xFF999999))
                        Box(
                            modifier = Modifier.weight(1f).height(6.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxHeight()
                                    .fillMaxWidth((count ?: 0).toFloat() / maxDist)
                                    .background(
                                        if (star >= 4) Color(0xFFFFA000) else Color(0xFFCCCCCC),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                        Text(text = "${count ?: 0}", fontSize = 11.sp, color = Color(0xFF999999))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // Lista de avaliações
        for (i in 0 until reviewsList.length()) {
            val review = reviewsList.optJSONObject(i) ?: continue
            val rating = review.optInt("rating", 5)
            val comment = review.optString("comment", "")
            val createdAt = review.optString("created_at", "")
            val authorName = review.optString("author_name", "").ifBlank { "Cliente" }
            val responses = review.optJSONArray("review_responses")
            val hasResponse = responses != null && responses.length() > 0

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = authorName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = formatRelativeTime(createdAt),
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= rating) "★" else "☆",
                            fontSize = 13.sp,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
                if (comment.isNotBlank() && comment != "null") {
                    Text(text = comment, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                }
                if (hasResponse) {
                    val responseMsg = responses!!.optJSONObject(0)?.optString("message", "") ?: ""
                    if (responseMsg.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Resposta da loja",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaSuperPrimary
                            )
                            Text(text = responseMsg, fontSize = 13.sp, color = Color(0xFF444444))
                        }
                    }
                }
            }
            if (i < reviewsList.length() - 1) {
                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatReviewDate(iso: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        val date = parser.parse(iso.substringBefore("+").substringBefore(".")) ?: return ""
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatRelativeTime(iso: String): String {
    return try {
        // O banco guarda timestamptz; o PostgREST devolve com offset (ex.: "+00:00").
        // Convertemos para o instante UTC correto antes de comparar com "agora".
        var s = iso.trim()
        if (s.endsWith("Z", ignoreCase = true)) s = s.dropLast(1) + "+00:00"
        var offsetMs = 0L
        Regex("([+-])(\\d{2}):?(\\d{2})$").find(s)?.let { m ->
            val sign = if (m.groupValues[1] == "+") 1 else -1
            offsetMs = sign * (m.groupValues[2].toLong() * 3600000L + m.groupValues[3].toLong() * 60000L)
            s = s.substring(0, m.range.first)
        }
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(s.substringBefore(".")) ?: return ""
        val diffMs = System.currentTimeMillis() - (date.time - offsetMs)
        if (diffMs < 0) return ""
        val minutes = diffMs / 60000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> "agora mesmo"
            minutes < 60 -> "há $minutes min"
            hours < 24 -> "há $hours h"
            days < 7 -> if (days == 1L) "há 1 dia" else "há $days dias"
            days < 30 -> { val w = days / 7; if (w == 1L) "há 1 semana" else "há $w semanas" }
            days < 365 -> { val m = days / 30; if (m == 1L) "há 1 mês" else "há $m meses" }
            else -> { val y = days / 365; if (y == 1L) "há 1 ano" else "há $y anos" }
        }
    } catch (e: Exception) {
        ""
    }
}

fun Store.displayAddress(): String {
    if (address.isNotBlank()) return address
    return listOfNotNull(
        addressStreet.takeIf { it.isNotBlank() },
        addressNumber.takeIf { it.isNotBlank() },
        addressNeighborhood.takeIf { it.isNotBlank() },
        addressCity.takeIf { it.isNotBlank() },
        addressState.takeIf { it.isNotBlank() },
        addressCep.takeIf { it.isNotBlank() }
    ).joinToString(", ")
}

fun StoreOpeningHour.brasiliaDayOrder(): Int = when (brasiliaDayLabel()) {
    "Dom" -> 0
    "Seg" -> 1
    "Ter" -> 2
    "Qua" -> 3
    "Qui" -> 4
    "Sex" -> 5
    "Sáb" -> 6
    else -> 7
}

fun StoreOpeningHour.brasiliaDayLabel(): String {
    val raw = dayOfWeekStr.trim().lowercase(Locale.ROOT)
        .replace("ç", "c")
        .replace("á", "a")
        .replace("ã", "a")
        .replace("é", "e")
        .replace("-feira", "")
    return when {
        raw.startsWith("dom") || raw == "0" || dayOfWeek == 0 -> "Dom"
        raw.startsWith("seg") || raw == "1" || dayOfWeek == 1 -> "Seg"
        raw.startsWith("ter") || raw == "2" || dayOfWeek == 2 -> "Ter"
        raw.startsWith("qua") || raw == "3" || dayOfWeek == 3 -> "Qua"
        raw.startsWith("qui") || raw == "4" || dayOfWeek == 4 -> "Qui"
        raw.startsWith("sex") || raw == "5" || dayOfWeek == 5 -> "Sex"
        raw.startsWith("sab") || raw == "6" || dayOfWeek == 6 -> "Sáb"
        else -> dayOfWeekStr.takeIf { it.isNotBlank() } ?: ""
    }
}

