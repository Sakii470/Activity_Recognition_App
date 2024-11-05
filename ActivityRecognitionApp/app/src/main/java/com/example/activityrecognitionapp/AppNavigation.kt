package com.example.activityrecognitionapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.screens.BluetoothScreen
import com.example.activityrecognitionapp.screens.LoginScreen
import com.example.activityrecognitionapp.screens.MainScreen
import com.example.activityrecognitionapp.screens.SignUpScreen

@Composable
fun AppNavigation(){
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "login") {
            composable("signUp") { SignUpScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("bluetooth") { BluetoothScreen(navController) }
        }
    }
}