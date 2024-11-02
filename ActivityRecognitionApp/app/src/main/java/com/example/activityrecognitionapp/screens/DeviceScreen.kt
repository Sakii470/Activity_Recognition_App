package com.example.activityrecognitionapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.presentation.BluetoothUiState
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.BluetoothDeviceList
import com.example.activityrecognitionapp.components.ButtonComponent


@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BluetoothDeviceList(
            scannedDevices = state.scannedDevices,
            onClick = onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // Umożliwia równomierne rozmieszczenie przycisków
        ) {
            ButtonComponent(
                value = stringResource(id = R.string.start_scan),
                onButtonClick = onStartScan,
                modifier = Modifier.weight(1f) // Umożliwia przyciskowi zajęcie 50% szerokości
            )
            Spacer(modifier = Modifier.width(8.dp)) // Dodaje odstęp między przyciskami
            ButtonComponent(
                value = stringResource(id = R.string.stop_scan), // Zmiana etykiety przycisku
                onButtonClick = onStopScan,
                modifier = Modifier.weight(1f) // Umożliwia przyciskowi zajęcie 50% szerokości
            )
        }

//            Button(onClick = onStartScan) {
//                Text(text = "Start scan")
//            }
//            Button(onClick = onStopScan) {
//                Text(text = "Stop scan")
//            }
    }
}


