package com.example.activityrecognitionapp.domain.repository

import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Bluetooth operations.
 */
interface BluetoothRepository {

    /** Indicates if a device is connected */
    val isConnected: StateFlow<Boolean>

    /** List of scanned Bluetooth devices */
    val scannedDevices: StateFlow<List<BluetoothDevice>>

    /** Bluetooth enabled status */
    val isBluetoothEnabled: StateFlow<Boolean>

    /** Emits error messages */
    val errors: SharedFlow<String>

    /** Starts scanning for Bluetooth devices */
    fun startScan()

    /** Stops scanning for Bluetooth devices */
    fun stopScan()

    /** Connects to a specified Bluetooth device */
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>

    /** Disconnects from the current Bluetooth device */
    fun disconnect()

    /** Enables Bluetooth on the device */
    fun enableBluetooth(): Boolean

    /** Checks if Bluetooth is enabled */
    fun isBluetoothEnabled(): Boolean

    /** Clears any error messages */
    fun clearErrorMessage()
}
