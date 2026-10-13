package com.example.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Store
import com.example.ui.theme.ItaSuperPrimary

@Composable
fun StoreInfoScreen(
    storeId: String,
    viewModel: StoreDetailViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(storeId) { viewModel.loadStore(storeId) }
    val uiState by viewModel.uiState.collectAsState()
    val store = uiState.store

    Scaffold(containerColor = Color.White) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Top nav: back | share heart
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(26.dp).clickable(onClick = onBackClick)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Compartilhar",
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Store name + logo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = store?.name ?: "",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A1A),
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(store?.category?.replaceFirstChar { it.uppercase() } ?: "")
                                store?.distanceKm?.let { d ->
                                    if (d > 0) append(" • ${formatDist(d)}")
                                }
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF777777)
                        )
                    }
                    val logoUrl = store?.logoUrl
                    if (!logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = store?.name?.firstOrNull()?.toString() ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                        }
                    }
                }
            }

            // Rating row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rating = store?.rating ?: 0.0
                        Text(
                            text = if (rating > 0) String.format("%.1f", rating).replace(".", ",") else "–",
                            fontSize = 17.sp,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { i ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (rating > i) Color(0xFF1A1A1A) else Color(0xFFE0E0E0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "avaliações",
                        fontSize = 14.sp,
                        color = Color(0xFF777777)
                    )
                }
            }

            // Tabs
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Avaliações", fontSize = 15.sp, color = Color(0xFF999999))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Informações", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ItaSuperPrimary)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(2.dp).background(ItaSuperPrimary)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            }

            // Descrição da loja
            item {
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Descrição da loja", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(12.dp))
                    val desc = store?.description?.takeIf { it.isNotBlank() }
                    if (desc != null) {
                        Text(
                            text = if (expanded) desc else desc.take(120) + "...",
                            fontSize = 15.sp,
                            color = Color(0xFF555555),
                            lineHeight = 22.sp
                        )
                        if (!expanded && desc.length > 120) {
                            Text(
                                text = "ver mais",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333),
                                modifier = Modifier.clickable { expanded = true }.padding(top = 2.dp)
                            )
                        }
                    } else {
                        Text(text = "Descrição não informada.", fontSize = 15.sp, color = Color(0xFF999999))
                    }
                }
            }

            // Aberto agora
            item {
                var showHours by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showHours = !showHours },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Aberto agora", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Hoje", fontSize = 15.sp, color = Color(0xFF777777), modifier = Modifier.width(110.dp))
                        Text(text = "Consulte os horários", fontSize = 15.sp, color = Color(0xFF777777))
                    }
                    if (showHours) {
                        val hours = store?.openingHours.orEmpty()
                        hours.forEach { h ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(text = h.dayOfWeekStr ?: "", fontSize = 15.sp, color = Color(0xFF777777), modifier = Modifier.width(110.dp))
                                Text(text = "${h.openTime} às ${h.closeTime}", fontSize = 15.sp, color = Color(0xFF777777))
                            }
                        }
                    }
                }
            }

            // Formas de pagamento
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Formas de pagamento", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                        Text(text = "Ver mais", fontSize = 15.sp, color = ItaSuperPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PaymentRow(label = "Online pelo ItaSuper", brands = listOf("PIX", "VISA", "MASTER", "ELO"))
                    Spacer(modifier = Modifier.height(12.dp))
                    PaymentRow(label = "Na entrega", brands = listOf("DINHEIRO", "VISA", "MASTER"))
                }
            }

            // Endereço
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Endereço", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(14.dp))
                    Row {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = store?.displayAddress() ?: "Endereço não informado",
                            fontSize = 15.sp,
                            color = Color(0xFF555555),
                            lineHeight = 23.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PaymentRow(label: String, brands: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = Color(0xFF777777))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            brands.take(4).forEach { brand ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF0F0F0),
                    modifier = Modifier.height(22.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(text = brand, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF555555))
                    }
                }
            }
            Text(text = "+", fontSize = 18.sp, color = Color(0xFF999999))
        }
    }
}

private fun formatDist(km: Double): String {
    return if (km < 1) "${(km * 1000).toInt()} m"
    else String.format("%.1f", km).replace(".", ",") + " km"
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
