package com.example.ui.orders

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.remote.DriverLocation
import com.example.data.remote.SupabaseClient
import com.example.data.repository.UserSessionRepository
import com.example.ui.theme.ItaSuperPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun trackingMapHtml(
    driverLat: Double?,
    driverLng: Double?,
    storeLat: Double?,
    storeLng: Double?,
    clientLat: Double?,
    clientLng: Double?
): String {
    val centerLat = driverLat ?: storeLat ?: clientLat ?: -22.5
    val centerLng = driverLng ?: storeLng ?: clientLng ?: -48.5
    val markers = buildString {
        if (driverLat != null && driverLng != null) {
            append("L.marker([$driverLat, $driverLng], {icon: driverIcon}).addTo(map).bindPopup('Entregador');")
        }
        if (storeLat != null && storeLng != null) {
            append("L.marker([$storeLat, $storeLng], {icon: storeIcon}).addTo(map).bindPopup('Loja');")
        }
        if (clientLat != null && clientLng != null) {
            append("L.marker([$clientLat, $clientLng], {icon: homeIcon}).addTo(map).bindPopup('Você');")
        }
    }
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
html, body { margin: 0; padding: 0; height: 100%; }
#map { height: 100%; width: 100%; }
.driver-pin { width: 40px; height: 40px; background: #2563eb; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
  border: 3px solid white; box-shadow: 0 4px 12px rgba(37,99,235,0.5); }
.store-pin { width: 36px; height: 36px; background: #ea580c; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
  border: 3px solid white; box-shadow: 0 3px 10px rgba(234,88,12,0.4); }
.home-pin { width: 36px; height: 36px; background: #16a34a; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
  border: 3px solid white; box-shadow: 0 3px 10px rgba(22,163,74,0.4); }
</style>
</head>
<body>
<div id="map"></div>
<script>
var map = L.map('map').setView([$centerLat, $centerLng], 14);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
var driverIcon = L.divIcon({ html: '<div class="driver-pin">🏍️</div>', className: '', iconSize: [40, 40], iconAnchor: [20, 20] });
var storeIcon = L.divIcon({ html: '<div class="store-pin">🏪</div>', className: '', iconSize: [36, 36], iconAnchor: [18, 18] });
var homeIcon = L.divIcon({ html: '<div class="home-pin">🏠</div>', className: '', iconSize: [36, 36], iconAnchor: [18, 18] });
$markers
</script>
</body>
</html>
""".trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DeliveryTrackingScreen(
    orderId: String,
    ordersViewModel: OrdersViewModel,
    onBack: () -> Unit
) {
    val orders by ordersViewModel.ordersList.collectAsState()
    val order = remember(orderId, orders) { orders.find { it.id == orderId } }
    var driverLocation by remember { mutableStateOf<DriverLocation?>(null) }
    var storeCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(order?.id) {
        val o = order ?: return@LaunchedEffect
        storeCoords = SupabaseClient.fetchStoreCoordinates(o.storeId)
        val token = UserSessionRepository.userSession.value.accessToken.orEmpty()
        // Primeira busca imediata
        driverLocation = SupabaseClient.fetchDriverLocation(o.driverId, token)
        isLoading = false
        // Atualiza a cada 20 segundos
        while (isActive) {
            delay(20_000)
            SupabaseClient.fetchDriverLocation(o.driverId, token)?.let { driverLocation = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acompanhar entrega", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (order == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pedido não encontrado", color = Color(0xFF737373))
                }
                return@Column
            }

            // Card com dados do pedido
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            order.storeName.ifBlank { "Loja" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF737373), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            order.deliveryAddress.ifBlank { "Endereço de entrega" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D5D5D)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Motorcycle, null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (driverLocation != null) "Entregador a caminho" else "Buscando localização do entregador...",
                            style = MaterialTheme.typography.bodySmall,
                            color = ItaSuperPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ItaSuperPrimary)
                    }
                } else {
                    val html = remember(driverLocation, storeCoords) {
                        trackingMapHtml(
                            driverLat = driverLocation?.latitude,
                            driverLng = driverLocation?.longitude,
                            storeLat = storeCoords?.first,
                            storeLng = storeCoords?.second,
                            clientLat = order.clientLatitude,
                            clientLng = order.clientLongitude
                        )
                    }
                    var webView by remember { mutableStateOf<WebView?>(null) }
                    // Recarrega o mapa quando a localização muda
                    LaunchedEffect(html) {
                        webView?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                    DisposableEffect(Unit) {
                        onDispose { webView?.destroy() }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                webView = this
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
