package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.activityrecognitionapp.components.ActivityBarChart
import com.example.activityrecognitionapp.presentation.viewmodels.BluetoothViewModel

@Composable
fun HomeScreen(viewModel: BluetoothViewModel) {

    val state by viewModel.state.collectAsState()
    val homeUiState by viewModel.homeUiState.collectAsState()


    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Możesz dostosować wysokość
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.medium), // Zaokrąglenie rogów
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isConnected) {
                        Text(
                            text = "${state.dataFromBluetooth}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            text = "You have to connect to device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ActivityBarChart(
                standingPercentage = homeUiState.standPercentage,  // 30%
                walkingPercentage =  homeUiState.walkPercentage,  // 50%
                runningPercentage =  homeUiState.runPercentage,  // 20%
            )

            //PreviewActivityBarChart()

        }
    }
}

@Composable
fun PreviewActivityBarChart() {
    var standing by remember { mutableStateOf(0.3f) }
    var walking by remember { mutableStateOf(0.4f) }
    var running by remember { mutableStateOf(0.3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ActivityBarChart(
            standingPercentage = standing,
            walkingPercentage = walking,
            runningPercentage = running,
            height = 40.dp,
            cornerRadius = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Przykładowe przyciski do zmiany wartości
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                standing = 0.2f
                walking = 0.5f
                running = 0.3f
            }) {
                Text("Zmiana 1")
            }
            Button(onClick = {
                standing = 0.4f
                walking = 0.3f
                running = 0.3f
            }) {
                Text("Zmiana 2")
            }
        }
    }
}





















