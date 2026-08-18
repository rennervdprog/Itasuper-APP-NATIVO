package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.network.ConnectivityMonitor
import com.example.data.repository.CartRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import com.example.notifications.PushNotificationManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.legal.LegalConsentGate
import com.example.ui.legal.LegalConsentViewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.notifications.NotificationsViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.orders.CartScreen
import com.example.ui.orders.CheckoutScreen
import com.example.ui.orders.OrdersHistoryScreen
import com.example.ui.orders.OrdersScreen
import com.example.ui.orders.OrdersViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.store.StoreDetailScreen
import com.example.ui.store.StoreInfoScreen
import com.example.ui.store.StoreDetailViewModel
import com.example.ui.theme.ItaSuperTheme
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNotificationManager.createOrderNotificationChannel(applicationContext)
        PushNotificationManager.captureLaunchIntent(applicationContext, intent)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error enabling edge to edge", e)
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            ItaSuperTheme {
                ItaSuperApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UserSessionRepository.refreshSession()
        PushNotificationManager.registerCurrentDevice(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushNotificationManager.captureLaunchIntent(applicationContext, intent)
    }
}

@Composable
fun ItaSuperApp() {
    val context = LocalContext.current
    val userSession by UserSessionRepository.userSession.collectAsState()
    val pendingPushDestination by PushNotificationManager.pendingDestination.collectAsState()
    val pendingPushOrderId by PushNotificationManager.pendingOrderId.collectAsState()
    val connectivityMonitor = remember(context) { ConnectivityMonitor(context) }
    val isOnline by connectivityMonitor.isOnline.collectAsState()
    var hasLostConnection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserSessionRepository.initialize(context)
    }
    LaunchedEffect(userSession.isLoggedIn, userSession.userId) {
        CartRepository.initialize(
            context = context,
            userId = if (userSession.isLoggedIn) userSession.userId else ""
        )
        if (userSession.isLoggedIn) {
            PushNotificationManager.registerCurrentDevice(context)
        }
    }

    if (!userSession.isSessionRestored) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Restaurando sua sessão...")
            }
        }
        return
    }

    val navController = rememberNavController()
    // Carrinho, checkout e histórico compartilham a mesma cotação e estado de endereço.
    val ordersViewModel: OrdersViewModel = viewModel()
    val legalConsentViewModel: LegalConsentViewModel = viewModel()
    val startDestination = if (userSession.isLoggedIn) "home" else "auth"

    LaunchedEffect(userSession.isLoggedIn, userSession.userId, userSession.accessToken) {
        legalConsentViewModel.checkForUpdates(userSession)
    }

    LaunchedEffect(isOnline, userSession.isLoggedIn) {
        if (!isOnline) {
            hasLostConnection = true
        } else if (hasLostConnection) {
            hasLostConnection = false
            StoreRepository.refreshStoresFromSupabase()
            if (userSession.isLoggedIn) {
                ordersViewModel.refreshOrders()
            }
        }
    }

    LaunchedEffect(pendingPushDestination, pendingPushOrderId, userSession.isLoggedIn) {
        if (userSession.isLoggedIn && pendingPushDestination == PushNotificationManager.DESTINATION_ORDERS) {
            val orderId = PushNotificationManager.consumePendingOrderId(context)
            PushNotificationManager.consumePendingDestination(context)
            val route = orderId?.let { "pedidos?orderId=$it" } ?: "pedidos"
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        composable("auth") {
            val authViewModel: AuthViewModel = viewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToStore = { storeId ->
                    navController.navigate("loja/$storeId")
                },
                onNavigateToOrders = {
                    navController.navigate("pedidos")
                },
                onNavigateToRoute = { route ->
                    if (route != "home") {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        composable("busca") {
            val searchViewModel: SearchViewModel = viewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToStore = { storeId ->
                    navController.navigate("loja/$storeId")
                },
                onNavigateToRoute = { route ->
                    if (route != "busca") {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        composable("carrinho") {
            CartScreen(
                viewModel = ordersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCheckout = { navController.navigate("checkout") },
                onNavigateToOrders = { navController.navigate("pedidos") },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable("checkout") {
            CheckoutScreen(
                viewModel = ordersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrders = {
                    navController.navigate("pedidos") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "pedidos?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            OrdersHistoryScreen(
                viewModel = ordersViewModel,
                initialOrderId = backStackEntry.arguments?.getString("orderId"),
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCart = {
                    navController.navigate("carrinho")
                },
                onExploreClick = {
                    navController.navigate("home") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable("notificacoes") {
            val notificationsViewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable("perfil") {
            ProfileScreen(
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLogout = {
                    UserSessionRepository.logout()
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "loja/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            val storeViewModel: StoreDetailViewModel = viewModel()
            StoreDetailScreen(
                storeId = storeId,
                viewModel = storeViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToCart = {
                    navController.navigate("carrinho")
                },
                onNavigateToInfo = {
                    navController.navigate("loja-info/$storeId")
                }
            )
        }

        composable(
            route = "loja-info/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            val storeInfoViewModel: StoreDetailViewModel = viewModel()
            StoreInfoScreen(
                storeId = storeId,
                viewModel = storeInfoViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        }
        LegalConsentGate(
            session = userSession,
            viewModel = legalConsentViewModel
        )
        if (!isOnline) {
            OfflineConnectivityBanner()
        }
    }
}

@Composable
private fun OfflineConnectivityBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color(0xFFF4F4F4),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE8E8E8))
        ) {
            Text(
                text = "Sem internet. Exibindo informações já carregadas.",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Color(0xFF686868),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
fun PlaceholderNavScreen(
    title: String,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    Scaffold(
        bottomBar = {
            if (currentRoute in listOf("home", "busca", "pedidos", "perfil")) {
                ItaSuperBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (onLogout != null) {
                    Button(onClick = onLogout) {
                        Text("Sair da Conta")
                    }
                }
            }
        }
    }
}
