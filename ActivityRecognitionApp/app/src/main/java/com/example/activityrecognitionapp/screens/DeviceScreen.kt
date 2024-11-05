package com.example.activityrecognitionapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.presentation.BluetoothUiState
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.BluetoothDeviceList
import com.example.activityrecognitionapp.components.ButtonComponent
import com.example.activityrecognitionapp.components.NormalTextComponent


@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    modifier: Modifier
){

    DeviceScreenContent(
        state = state,
        onStartScan = onStartScan,
        onStopScan = onStopScan,
        onDeviceClick = onDeviceClick,
        )
    }

@Composable
fun DeviceScreenContent(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
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

        // Pokazanie statusu połączenia, jeśli aplikacja jest połączona
        if (state.isConnected) {
            Text(
                text = "Connected to ${state.dataFromBluetooth ?: "Device"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }


        BluetoothDeviceList(
            scannedDevices = state.scannedDevices,
            onClick = onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // Zmiana na SpaceBetween, aby wykorzystać weight dla każdego elementu
        ) {
            ButtonComponent(
                value = stringResource(id = R.string.start_scan),
                onButtonClick = onStartScan,
                modifier = Modifier.weight(1f) // Umożliwia przyciskowi zajęcie 50% szerokości
            )
            Spacer(modifier = Modifier.weight(0.1f)) // Dodaje odstęp między przyciskami; ustal proporcję, np. 0.1f
            ButtonComponent(
                value = stringResource(id = R.string.stop_scan),
                onButtonClick = onStopScan,
                modifier = Modifier.weight(1f) // Umożliwia przyciskowi zajęcie 50% szerokości
            )
        }
    }

}



