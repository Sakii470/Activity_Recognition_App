package com.example.activityrecognitionapp.domain.repository

import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


interface BluetoothRepository {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val isBluetoothEnabled: StateFlow<Boolean> // Dodane pole
    //val pairedDevices: StateFlow<List<BluetoothDevice>>
    val errors: SharedFlow<String>


//    fun startDiscovery()
//    fun stopDiscovery()
//
//    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>
//
//    fun closeConnection()
//
//    fun connectToPairedDeviceWithCharacteristic(
//        characteristicCheck: (BluetoothDeviceDomain) -> Boolean
//    ): Flow<ConnectionResult>

    fun startScan()
    fun stopScan()
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>
    fun disconnect()
    fun enableBluetooth() : Boolean
    fun isBluetoothEnabled(): Boolean
    fun clearErrorMessage()

}