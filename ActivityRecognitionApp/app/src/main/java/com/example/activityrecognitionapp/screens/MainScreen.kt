package com.example.activityrecognitionapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainScreen(
    navController: NavController
) {


    // LaunchedEffect zostanie wywołany, gdy MainScreen zostanie uruchomiony
    LaunchedEffect(Unit) {
        println("MainScreen has been launched")
    }

    // Wyświetlanie treści na ekranie
    Surface(
        //modifier = Modifier.wrapContentSize(),
        color = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center,
            // modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Witamy na głównym ekranie!",
                color = Color.Black,
                fontSize = 24.sp
            )
        }
    }
}