// TODO(DEBUG): REMOVER ANTES DE PUBLICAR - tela temporária para debug do mapa
package com.example.ui.debug

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.debug.MapDebugLog
import com.example.network.SupabaseClient
import com.example.session.UserSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDebugScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by MapDebugLog.logs.collectAsState()
    var testHtml by remember { mutableStateOf<String?>(null) }
    var testTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Mapa (temporário)") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão: Testar WebView básico
            Button(
                onClick = {
                    MapDebugLog.add("Testando WebView com HTML simples...")
                    testTitle = "Teste 1: HTML simples"
                    testHtml = """
                        <html><body style="background: #4CAF50; color: white; font-size: 24px; text-align: center; padding: 50px;">
                        <h1>WebView OK!</h1>
                        <p>Se você vê isso, o WebView renderiza.</p>
                        </body></html>
                    """.trimIndent()
                    MapDebugLog.add("HTML simples carregado no WebView")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("1. Testar WebView básico")
            }

            // Botão: Testar Leaflet JS
            Button(
                onClick = {
                    MapDebugLog.add("Testando carregamento do Leaflet JS...")
                    testTitle = "Teste 2: Leaflet JS"
                    testHtml = """
                        <html><head>
                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                        </head><body style="font-size: 20px; padding: 20px;">
                        <div id="result">Carregando Leaflet...</div>
                        <script>
                        try {
                            if (typeof L !== 'undefined') {
                                document.getElementById('result').innerHTML = '<h2 style="color: green;">Leaflet OK!</h2><p>Versão: ' + L.version + '</p>';
                            } else {
                                document.getElementById('result').innerHTML = '<h2 style="color: red;">Leaflet FALHOU</h2><p>L is undefined - script não carregou</p>';
                            }
                        } catch (e) {
                            document.getElementById('result').innerHTML = '<h2 style="color: red;">Erro: ' + e.message + '</h2>';
                        }
                        </script>
                        </body></html>
                    """.trimIndent()
                    MapDebugLog.add("Teste Leaflet iniciado - veja o resultado no WebView abaixo")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Text("2. Testar Leaflet JS")
            }

            // Botão: Testar mapa real
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            MapDebugLog.add("Buscando localização do entregador...")
                            val session = UserSessionRepository.userSession.value
                            val token = session.accessToken ?: ""
                            // Busca o pedido ativo e a localização
                            // Por simplicidade, usa coordenadas fixas de teste
                            val driverLat = -22.8606
                            val driverLng = -42.3324
                            MapDebugLog.add("Coordenadas de teste: $driverLat, $driverLng")

                            withContext(Dispatchers.Main) {
                                testTitle = "Teste 3: Mapa Leaflet real"
                                testHtml = """
                                <!DOCTYPE html>
                                <html><head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                <style>html, body { margin: 0; padding: 0; height: 100%; } #map { height: 400px; width: 100%; }</style>
                                </head><body>
                                <div id="map"></div>
                                <script>
                                try {
                                    var map = L.map('map').setView([$driverLat, $driverLng], 15);
                                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                                    L.marker([$driverLat, $driverLng]).addTo(map).bindPopup('Entregador').openPopup();
                                    document.body.insertAdjacentHTML('beforeend', '<p style="color: green; padding: 10px;">Mapa OK!</p>');
                                } catch (e) {
                                    document.body.insertAdjacentHTML('beforeend', '<p style="color: red; padding: 10px;">Erro: ' + e.message + '</p>');
                                }
                                </script>
                                </body></html>
                                """.trimIndent()
                                MapDebugLog.add("Mapa de teste carregado")
                            }
                        } catch (t: Throwable) {
                            MapDebugLog.add("FALHA: ${t::class.simpleName}: ${t.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("3. Testar mapa Leaflet real")
            }

            Button(
                onClick = { MapDebugLog.clear() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF26522))
            ) {
                Text("Limpar logs")
            }

            // WebView de teste
            if (testHtml != null) {
                Text(testTitle, style = MaterialTheme.typography.titleSmall)
                Box(modifier = Modifier.height(450.dp).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                        MapDebugLog.add("WebView ERRO: ${error?.description}")
                                    }
                                }
                                loadDataWithBaseURL("https://localhost/", testHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://localhost/", testHtml, "text/html", "UTF-8", null)
                        }
                    )
                }
            }

            // Logs
            Text("Logs (${logs.size})", style = MaterialTheme.typography.titleSmall)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    logs.takeLast(50).forEach { log ->
                        Text(
                            text = log,
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (logs.isEmpty()) {
                        Text("Nenhum log ainda", color = Color.Gray)
                    }
                }
            }
        }
    }
}
