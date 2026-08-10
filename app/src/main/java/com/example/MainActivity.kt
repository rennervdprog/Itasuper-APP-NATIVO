package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.UserSessionRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.store.StoreDetailScreen
import com.example.ui.store.StoreDetailViewModel
import com.example.ui.theme.ItaSuperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItaSuperTheme {
                ItaSuperApp()
            }
        }
    }
}

@Composable
fun ItaSuperApp() {
    val navController = rememberNavController()
    val userSession by UserSessionRepository.userSession.collectAsState()

    val startDestination = if (userSession.isLoggedIn) "home" else "auth"

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

        composable("pedidos") {
            PlaceholderNavScreen(
                title = "Meus Pedidos",
                currentRoute = "pedidos",
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
            PlaceholderNavScreen(
                title = "Perfil do Cliente",
                currentRoute = "perfil",
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
                    navController.navigate("pedidos") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
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
