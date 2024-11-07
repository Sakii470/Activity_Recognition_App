package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.presentation.states.BluetoothUiState
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.BluetoothDeviceList
import com.example.activityrecognitionapp.components.HeadingTextComponent
import com.example.activityrecognitionapp.components.NormalTextComponent
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain


@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    connectedDevice: BluetoothDeviceDomain?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    modifier: Modifier
) {

    DeviceScreenContent(
        state = state,
        connectedDevice = connectedDevice,
        onStartScan = onStartScan,
        onStopScan = onStopScan,
        onDisconnect = onDisconnect,
        onDeviceClick = onDeviceClick,
    )
}

@Composable
fun DeviceScreenContent(
    state: BluetoothUiState,
    connectedDevice: BluetoothDeviceDomain?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        // Pokazanie wskaźnika łaczenia, jeśli aplikacja się łączy
        if (state.isConnecting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Connecting...")
            }
        }

//        LaunchedEffect(state.isConnected) {
//            if (state.isConnected) {
//                onNavigateToHome() // Przełącza na zakładkę "Home" po połączeniu
//            }
//        }

        //       Pokazanie statusu połączenia, jeśli aplikacja jest połączona
        if (state.isConnected && !state.isConnecting) {
            if (connectedDevice != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally // Wyśrodkowanie horyzontalne
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected with ${connectedDevice.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .weight(1f),
                            textAlign = TextAlign.Center

                        )
                        // Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onDisconnect, // Wywołanie funkcji obsługi kliknięcia
                            modifier = Modifier
                                .padding(
                                    0.dp,
                                    5.dp,
                                    10.dp,
                                    0.dp
                                ) // Dodatkowy padding, aby oddzielić od krawędzi
                                .size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.unlink),
                                contentDescription = "Add Device",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }




        HeadingTextComponent(
            value = stringResource(id = R.string.devices),
//                fontWeight = FontWeight.Bold,
//                fontSize = 24.sp,
            modifier = Modifier.padding(10.dp, 10.dp, 0.dp, 0.1.dp),
            textAlign = TextAlign.Left,
        )




        NormalTextComponent(
            value = stringResource(id = R.string.add_devices),
            modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 10.dp),
            style = TextStyle(fontSize = 15.sp),
            textAlign = TextAlign.Left
        )

        Scaffold(
            // containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium), // Zaokrąglenie rogów
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        IconButton(
                            onClick = onStartScan, // Wywołanie funkcji obsługi kliknięcia
                            modifier = Modifier
                                .padding(
                                    0.dp,
                                    20.dp,
                                    0.dp,
                                    2.dp
                                ) // Dodatkowy padding, aby oddzielić od krawędzi
                                .size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = "Add Device",
                                tint = MaterialTheme.colorScheme.tertiary

                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Dodaje odstęp między ikoną a tekstem
                        Text(
                            text = "Add Device",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        BluetoothDeviceList(
                            scannedDevices = state.scannedDevices,
                            onClick = onDeviceClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )


                    }
                }
            }
        }
    }
}




















