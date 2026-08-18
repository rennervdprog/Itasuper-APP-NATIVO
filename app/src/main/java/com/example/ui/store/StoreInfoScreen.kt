package com.example.ui.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
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
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val StoreInfoBackground = Color(0xFFFAFAFA)
private val StoreInfoDivider = Color(0xFFEDEDED)

@Composable
fun StoreInfoScreen(
    storeId: String,
    viewModel: StoreDetailViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(storeId) { viewModel.loadStore(storeId) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = StoreInfoBackground,
        topBar = {
            Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onBackClick),
                        shape = CircleShape,
                        color = Color(0xFFF4F4F4)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = ItaSuperTextPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sobre a loja",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = ItaSuperTextPrimary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
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

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                item { StoreInfoIdentity(store) }
                item { SectionHeading(title = "Horários", subtitle = "Horário de Brasília") }
                if (hours.isEmpty()) {
                    item { NeutralInfoRow("Horários não informados pela loja") }
                } else {
                    items(hours, key = { "hour_${it.dayOfWeekStr}_${it.dayOfWeek}_${it.openTime}" }) { hour ->
                        OpeningHourRow(hour)
                    }
                }

                item { SectionHeading(title = "Formas de pagamento") }
                if (payments.isEmpty()) {
                    item { NeutralInfoRow("Formas de pagamento não informadas pela loja") }
                } else {
                    items(payments, key = { it }) { method -> PaymentMethodRow(method) }
                }

                item { SectionHeading(title = "Endereço") }
                item {
                    AddressMapRow(
                        address = address,
                        onOpenMap = {
                            if (address.isNotBlank()) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}"))
                                    )
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun StoreInfoIdentity(store: Store?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(17.dp),
            color = Color(0xFFFFE9AD)
        ) {
            if (!store?.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = store?.logoUrl,
                    contentDescription = store?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp))
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = store?.name?.trim()?.firstOrNull()?.uppercase() ?: "I",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ItaSuperPrimary
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store?.name ?: "Loja ItaSuper",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ItaSuperTextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = store?.category?.takeIf { it.isNotBlank() } ?: "Categoria não informada",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF727272)
                )
            )
        }
    }
    HorizontalDivider(color = StoreInfoDivider)
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 25.dp, end = 16.dp, bottom = 9.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = ItaSuperTextPrimary
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF777777)
                )
            )
        }
    }
}

@Composable
private fun OpeningHourRow(hour: StoreOpeningHour) {
    val schedule = if (hour.isClosedAllDay) {
        "Fechado"
    } else {
        "${hour.openTime.toBrasiliaClock()} – ${hour.closeTime.toBrasiliaClock()}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hour.brasiliaDayLabel(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = ItaSuperTextPrimary
            ),
            modifier = Modifier.width(46.dp)
        )
        Text(
            text = schedule,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (hour.isClosedAllDay) Color(0xFFC53D43) else Color(0xFF555555)
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = Color(0xFF9A9A9A),
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = StoreInfoDivider)
}

@Composable
private fun PaymentMethodRow(method: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFF2E8)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (method.contains("Cart", ignoreCase = true)) {
                        Icons.Default.CreditCard
                    } else {
                        Icons.Default.AccountBalanceWallet
                    },
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = method,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ItaSuperTextPrimary
            )
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = StoreInfoDivider)
}

@Composable
private fun AddressMapRow(address: String, onOpenMap: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = address.isNotBlank(), onClick = onOpenMap),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE6E6E6))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF2E8)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (address.isBlank()) "Endereço não informado pela loja" else address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ver no mapa",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ItaSuperPrimary
                        )
                    )
                }
            }
            if (address.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Abrir no mapa",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun NeutralInfoRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF777777)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    )
}

private fun Store.paymentMethods(): List<String> = buildList {
    if (settings.acceptPixOnline) add("PIX online")
    if (settings.acceptPixMachine) add("PIX na maquininha")
    if (settings.acceptCard) add("Cartão")
    if (settings.acceptCash) add("Dinheiro")
}

private fun Store.displayAddress(): String {
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

private fun StoreOpeningHour.brasiliaDayLabel(): String {
    val raw = dayOfWeekStr.trim().lowercase(Locale.ROOT)
        .replace("ç", "c")
        .replace("á", "a")
        .replace("ã", "a")
        .replace("é", "e")
        .replace("-feira", "")
    return when {
        raw.startsWith("dom") || raw == "0" -> "Dom"
        raw.startsWith("seg") || raw == "1" -> "Seg"
        raw.startsWith("ter") || raw == "2" -> "Ter"
        raw.startsWith("qua") || raw == "3" -> "Qua"
        raw.startsWith("qui") || raw == "4" -> "Qui"
        raw.startsWith("sex") || raw == "5" -> "Sex"
        raw.startsWith("sab") || raw == "6" -> "Sáb"
        else -> when (dayOfWeek) {
            0 -> "Dom"
            1 -> "Seg"
            2 -> "Ter"
            3 -> "Qua"
            4 -> "Qui"
            5 -> "Sex"
            6 -> "Sáb"
            else -> "Dia"
        }
    }
}

private fun StoreOpeningHour.brasiliaDayOrder(): Int = when (brasiliaDayLabel()) {
    "Dom" -> 0
    "Seg" -> 1
    "Ter" -> 2
    "Qua" -> 3
    "Qui" -> 4
    "Sex" -> 5
    "Sáb" -> 6
    else -> 7
}

private fun String.toBrasiliaClock(): String {
    val plain = Regex("^\\d{1,2}:\\d{2}(?::\\d{2})?$")
    if (plain.matches(this.trim())) return this.trim().take(5).padStart(5, '0')

    val formatters = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    )
    for (inputFormat in formatters) {
        runCatching {
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val parsed: Date = inputFormat.parse(this.trim()) ?: return@runCatching null
            val outputFormat = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
            outputFormat.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
            outputFormat.format(parsed)
        }.getOrNull()?.let { return it }
    }
    return this.trim()
}
