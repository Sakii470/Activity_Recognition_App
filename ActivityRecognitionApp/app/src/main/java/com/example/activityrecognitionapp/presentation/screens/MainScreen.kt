package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.BottomNavigationBar
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel

@Composable
fun MainScreen(viewModel: BluetoothViewModel = hiltViewModel()) {

    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(viewModel=viewModel) }
            composable("data") { DataScreen(navController) }
            composable("device") { BluetoothScreen(viewModel=viewModel) }
        }
    }

}