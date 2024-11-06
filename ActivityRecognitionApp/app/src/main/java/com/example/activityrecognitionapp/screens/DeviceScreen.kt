package com.example.activityrecognitionapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.activityrecognitionapp.BottomNavigationBar
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.presentation.BluetoothUiState
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.BluetoothDeviceList
import com.example.activityrecognitionapp.components.ButtonComponent
import com.example.activityrecognitionapp.components.HeadingTextComponent
import com.example.activityrecognitionapp.components.NormalTextComponent
import com.example.activityrecognitionapp.presentation.BluetoothViewModel


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

//        LaunchedEffect(state.isConnected) {
//            if (state.isConnected) {
//                onNavigateToHome() // Przełącza na zakładkę "Home" po połączeniu
//            }
//        }

  //       Pokazanie statusu połączenia, jeśli aplikacja jest połączona
        if (state.isConnected) {
            Text(
                text = " ${state.dataFromBluetooth}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
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
                                .padding(0.dp,20.dp,0.dp,2.dp) // Dodatkowy padding, aby oddzielić od krawędzi
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














