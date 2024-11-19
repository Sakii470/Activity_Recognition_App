package com.example.activityrecognitionapp.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val isBluetoothEnabled: StateFlow<Boolean> // Dodane pole
    //val pairedDevices: StateFlow<List<BluetoothDevice>>
    val errors: SharedFlow<String>


    fun startDiscovery()
    fun stopDiscovery()

    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>

    fun closeConnection()

}