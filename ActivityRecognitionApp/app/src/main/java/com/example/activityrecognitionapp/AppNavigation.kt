package com.example.activityrecognitionapp



import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.components.BottomNavigationBar
import com.example.activityrecognitionapp.components.NetworkBanner
import com.example.activityrecognitionapp.components.TopBarWithMenu
import com.example.activityrecognitionapp.presentation.screens.BluetoothScreen
import com.example.activityrecognitionapp.presentation.screens.DataScreen
import com.example.activityrecognitionapp.presentation.screens.HomeScreen
import com.example.activityrecognitionapp.presentation.screens.LoginScreen
import com.example.activityrecognitionapp.presentation.screens.SignUpScreen
import com.example.activityrecognitionapp.presentation.screens.SplashScreen
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel
import com.example.activityrecognitionapp.presentation.viewmodels.SupabaseAuthViewModel


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AppNavigation(){
    val bluetoothViewModel: BluetoothViewModel = hiltViewModel()
    val supabaseViewModel: SupabaseAuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val isLoggedIn = supabaseViewModel.uiLoginState.collectAsState().value.isLoggedIn

    val navController = rememberNavController()
    val bottomBarScreens = listOf("home", "data", "bluetooth")

    val isNetworkAvailable by bluetoothViewModel.isNetworkAvailable.collectAsState()
    val showNetworkBanner by bluetoothViewModel.showNetworkBanner.collectAsState()

    
    

    // Pobierz referencję do Activity
    val activity = LocalContext.current as? Activity

    // Globalny BackHandler, który kończy aplikację
    BackHandler {
        activity?.finish() // Zakończ Activity i wyjdź z aplikacji
    }

    val startDestination = "splash"





    Column(modifier = Modifier.fillMaxSize()) {
        // Wyświetlanie NetworkBanner na górze, jeśli jest widoczny
        if (showNetworkBanner) {
            NetworkBanner(
                isConnected = isNetworkAvailable,
                onBannerDismissed = { bluetoothViewModel.hideNetworkBanner() },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f) // Upewnij się, że jest na wierzchu
            )
        }

        // Scaffold zawiera resztę UI
        Scaffold(
            topBar = {
                if (isLoggedIn) {
                    TopBarWithMenu(onLogoutClicked = {
                        supabaseViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    })
                }
            },
            bottomBar = {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                if (currentDestination?.route in bottomBarScreens) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()) {
                // Główna nawigacja
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    // Ekrany uwierzytelniania
                    composable("splash") { SplashScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("signUp") { SignUpScreen(navController) }

                    // Ekrany główne
                    composable("home") { HomeScreen(navController, bluetoothViewModel) }
                    composable("data") { DataScreen(navController) }
                    composable("bluetooth") { BluetoothScreen(viewModel = bluetoothViewModel) }
                }
            }
        }
    }

    // Logowanie stanu NetworkBanner dla diagnostyki
    LaunchedEffect(showNetworkBanner, isNetworkAvailable) {
        Log.d("AppNavigation", "showNetworkBanner: $showNetworkBanner, isNetworkAvailable: $isNetworkAvailable")
    }
}












