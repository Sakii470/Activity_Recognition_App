package com.example.activityrecognitionapp



import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.components.BottomNavItem
import com.example.activityrecognitionapp.presentation.screens.DataScreen
import com.example.activityrecognitionapp.presentation.screens.DeviceScreen
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

    val bottomBarScreens = listOf("home", "data", "device")


    val bluetoothUiState by bluetoothViewModel.bluetoothUiState.collectAsState()

    // Pobierz referencję do Activity
    val activity = LocalContext.current as? Activity

    // Globalny BackHandler, który kończy aplikację
    BackHandler {
        activity?.finish() // Zakończ Activity i wyjdź z aplikacji
    }


    val startDestination = "splash"




    Scaffold(
        topBar = {
            if(isLoggedIn){
                TopBarWithMenu(onLogoutClicked = {
                    supabaseViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                })
            }
        },
        bottomBar = {
            Log.d("AppNavigation", "isLogged po zalogowaniem ${isLoggedIn}")
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            if (currentDestination?.route in bottomBarScreens) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Ekrany uwierzytelniania
            composable("splash") { SplashScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("signUp") { SignUpScreen(navController) }

            // Ekrany główne
            composable("home") { HomeScreen (navController,bluetoothViewModel) }
            composable("data") { DataScreen(navController) }
            composable("device") {
                DeviceScreen(
                    state = bluetoothUiState,
                    connectedDevice = bluetoothViewModel.bluetoothUiState.value.connectedDevice,
                    onStartScan = { bluetoothViewModel.startScan() },
                    onStopScan = { bluetoothViewModel.stopScan() },
                    onDisconnect = { bluetoothViewModel.disconnectFromDevice() },
                    onDeviceClick = { bluetoothViewModel.connectToDevice(it) },
                    modifier = Modifier
                )
            }
        }
    }
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu(onLogoutClicked: () -> Unit) {
    val supabaseViewModel: SupabaseAuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val uiLoginState by supabaseViewModel.uiLoginState.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.hello," ",uiLoginState.userEmail )  )
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        expanded = false
                        onLogoutClicked()
                    }
                )
            }
        }
    )
}


@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Data", Icons.Default.Storage, "data"),
        BottomNavItem("Device", Icons.Default.Watch, "device")
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.route == item.route
            val iconColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor // Animowany kolor ikony
                    )
                },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}
