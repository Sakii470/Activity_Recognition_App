package com.example.activityrecognitionapp.presentation


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.activityrecognitionapp.presentation.components.DeviceScreen
import com.plcoding.bluetoothchat.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
//create bluetoothManager - software manadzer bluetooth
//crete bluetoothAdapter - bluetooth device module
    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//Register Launchers to ask about enable Bluetooth. It open new activity and wait for outcome. Ask user about turn on bluetooth. registerForActivityResult() - register activity
        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* Not needed */ }
//Register Permission Launcher. Lambda function defined what should do after receive answer on permission. We check version android and if app have BLUETOOTH_CONNECT permission
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if(canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }
//App need this permissions for use Bluetooth for newest Android Version 12
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }
//set activity content, create instance BluetoothViewModel use hiltViewModel it menage ViewModel lifecycle
        setContent {
            BluetoothChatTheme {
                val viewModel = hiltViewModel<BluetoothViewModel>()
//delegate state to object viewModel which one has logic to menage state. It have to update state activity and we can use it in UI.If state in ViewModel is chamge UI is update
                val state by viewModel.state.collectAsState()
//LaunchedEffect - JP Compose function inside function is notification about state is errorMessage. Effect will be call if stateMessage will be change.
                LaunchedEffect(key1 = state.errorMessage) {
                    state.errorMessage?.let {message ->
                        Toast.makeText(
                            applicationContext,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }



                LaunchedEffect(key1 = state.isConnected) {
                    if(state.isConnected) {
                        Toast.makeText(
                            applicationContext,
                            "You are connected!: ${state.dataFromBluetooth}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        state.isConnecting -> {
                            Column(modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Text(text = "Connecting...")

                            }

                        }
                        state.isConnected -> {
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ){
                                state.dataFromBluetooth?.let { Text(text = it) }
                            }
                        }
                        else -> {
//DeviceScreen - function which one render screen where are bluetooth devices. Function have 5 arguments.
                            DeviceScreen(
                                state = state,
//references to functions from viewModel
                                onStartScan = viewModel::startScan,
                                onStopScan = viewModel::stopScan,
                                onDeviceClick = viewModel::connectToDevice
                               // onStartServer = viewModel::waitForIncomingConnections
                            )
                        }
                    }

                }
            }
        }
    }
}