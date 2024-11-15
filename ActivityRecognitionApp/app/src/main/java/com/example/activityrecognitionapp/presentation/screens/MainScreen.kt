package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.BottomNavigationBar
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel

@Composable
fun MainScreen(navController: NavController, viewModel: BluetoothViewModel = hiltViewModel()) {

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        val innerNavController = rememberNavController()
        NavHost(
            navController = innerNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController,viewModel = viewModel) }
            composable("data") { DataScreen(navController) }
            composable("device") { BluetoothScreen(viewModel = viewModel) }
        }
    }

}