package com.example.activityrecognitionapp



import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.components.BottomNavItem
import com.example.activityrecognitionapp.presentation.screens.BluetoothScreen
import com.example.activityrecognitionapp.presentation.screens.HomeScreen
import com.example.activityrecognitionapp.presentation.screens.LoginScreen
import com.example.activityrecognitionapp.presentation.screens.MainScreen
import com.example.activityrecognitionapp.presentation.screens.SignUpScreen
import com.example.activityrecognitionapp.presentation.screens.SplashScreen


@Composable
fun AppNavigation(){
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "login") {
            composable("signUp") { SignUpScreen(navController) }
            composable("login") { LoginScreen(navController) }
            //composable("bluetooth") { BluetoothScreen() }
            composable("mainScreen") { MainScreen() }
            composable("splash") { SplashScreen(navController) }
           // composable("home"){ HomeScreen()}

        }
    }
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
