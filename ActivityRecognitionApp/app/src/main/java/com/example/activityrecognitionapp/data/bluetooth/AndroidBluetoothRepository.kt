package com.example.activityrecognitionapp.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages Bluetooth Low Energy (BLE) operations such as scanning, connecting, and data communication.
 * Implements the [BluetoothRepository] interface to provide BLE functionalities within the application.
 *
 * This repository handles scanning for BLE devices, managing connections, discovering services and characteristics,
 * and enabling notifications for real-time data updates. It also monitors Bluetooth state changes and
 * emits relevant status updates and errors through [StateFlow] and [SharedFlow].
 *
 * @param context The application context used for accessing system services and registering receivers.
 * @param bluetoothAdapterProvider Provides an instance of [BluetoothAdapter] for BLE operations.
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothRepository(
    private val context: Context,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
) : BluetoothRepository {

    // BluetoothAdapter used to perform BLE operations
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothAdapterProvider.getBluetoothAdapter()

    // BluetoothGatt instance for managing GATT connections
    private var bluetoothGatt: BluetoothGatt? = null

    // UUIDs for the specific BLE service and characteristic to interact with
    private val uuidService: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
    private val uuidCharacteristic: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")
    private val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // StateFlow to track connection status
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // StateFlow to track if Bluetooth is enabled
    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // StateFlow to hold the list of scanned BLE devices
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    // SharedFlow to emit error messages related to BLE operations
    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    // Channel to handle connection results asynchronously
    private val connectionResultChannel = Channel<ConnectionResult>(Channel.BUFFERED)

    // CoroutineScope for managing asynchronous tasks
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Callback for handling BLE scan results.
     * Updates the list of scanned devices when new devices are found.
     */
    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            Log.d(
                "BLE Scan",
                "Found device: ${device.name} - ${device.address} RSSI: $rssi"
            )

            if (!device.name.isNullOrEmpty()) {
                val bluetoothDeviceDomain = device.toBluetoothDeviceDomain(rssi)
                _scannedDevices.update { devices ->
                    if (devices.none { it.address == bluetoothDeviceDomain.address }) {
                        devices + bluetoothDeviceDomain
                    } else devices
                }
            }
        }
    }

    /**
     * BroadcastReceiver to monitor Bluetooth state changes such as enabling/disabling and device connections.
     * Updates the relevant StateFlows based on the received actions.
     */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.update { state == BluetoothAdapter.STATE_ON }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    _isConnected.update { true }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    _isConnected.update { false }
                }
            }
        }
    }

    init {
        // Register the BroadcastReceiver to listen for Bluetooth state changes
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothStateReceiver, intentFilter)
    }

    /**
     * Initiates BLE scanning for nearby devices.
     * Emits an error if the required BLUETOOTH_SCAN permission is missing.
     * Automatically stops scanning after 10 seconds.
     */
    override fun startScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            coroutineScope.launch {
                _errors.emit("Missing BLUETOOTH_SCAN permission.")
            }
            return
        }
        // Reset the list of scanned devices before starting a new scan
        _scannedDevices.value = emptyList()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
        coroutineScope.launch {
            delay(10000) // Scan duration: 10 seconds
            stopScan()
        }
    }

    /**
     * Stops the ongoing BLE scan.
     * Emits an error if the required BLUETOOTH_SCAN permission is missing.
     */
    override fun stopScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            coroutineScope.launch {
                _errors.emit("Missing BLUETOOTH_SCAN permission.")
            }
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

    /**
     * Connects to a specified BLE device and returns a Flow emitting connection results.
     *
     * @param device The [BluetoothDeviceDomain] representing the device to connect to.
     * @return A [Flow] emitting [ConnectionResult] indicating the outcome of the connection attempt.
     */
    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> = flow {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            throw SecurityException("Missing BLUETOOTH_CONNECT permission.")
        }

        bluetoothAdapter?.let { adapter ->
            try {
                val bluetoothDevice = adapter.getRemoteDevice(device.address)
                bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
                emitAll(connectionResultChannel.receiveAsFlow())
            } catch (e: IllegalArgumentException) {
                emit(ConnectionResult.Error("Device not found with provided address."))
            }
        } ?: emit(ConnectionResult.Error("BluetoothAdapter not initialized"))
    }

    /**
     * Disconnects from the currently connected BLE device.
     * The actual closure of the GATT connection is handled in the callback.
     */
    override fun disconnect() {
        bluetoothGatt?.disconnect()
        // Do not call close() immediately; wait for onConnectionStateChange callback
    }

    /**
     * Enables Bluetooth on the device.
     * If Bluetooth is already enabled, it returns true. Otherwise, it prompts the user to enable Bluetooth.
     *
     * @return `true` if Bluetooth is already enabled, `false` otherwise.
     */
    override fun enableBluetooth(): Boolean {
        return if (bluetoothAdapter?.isEnabled == true) {
            // Bluetooth is already enabled
            true
        } else {
            // Launch intent to request enabling Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(enableBtIntent)
            false // Return false because enabling depends on user's action
        }
    }

    /**
     * Checks if Bluetooth is currently enabled.
     *
     * @return `true` if Bluetooth is enabled, `false` otherwise.
     */
    override fun isBluetoothEnabled(): Boolean {
        return _isBluetoothEnabled.value
    }

    /**
     * Clears any existing error messages by emitting an empty string.
     */
    override fun clearErrorMessage() {
        coroutineScope.launch {
            _errors.emit("")
        }
    }

    /**
     * Callback for handling GATT events such as connection state changes, service discoveries,
     * and characteristic updates.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BluetoothRepository", "Connected to GATT server.")
                    _isConnected.update { true }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BluetoothRepository", "Disconnected from GATT server.")
                    _isConnected.update { false }
                    connectionResultChannel.trySend(ConnectionResult.Error("Disconnected"))
                    gatt.close()
                    bluetoothGatt = null
                }
                else -> {
                    Log.d("BluetoothRepository", "Connection state changed: $newState")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothRepository", "Services discovered.")
                val characteristic = findCharacteristic(uuidService, uuidCharacteristic)
                if (characteristic != null) {
                    enableNotification(characteristic)
                } else {
                    connectionResultChannel.trySend(ConnectionResult.Error("Characteristic not found."))
                }
            } else {
                connectionResultChannel.trySend(ConnectionResult.Error("Service discovery failed with status $status."))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == uuidCharacteristic) {
                val dataFromBluetooth = characteristic.value.decodeToString()
                Log.d("BluetoothRepository", "Received data: $dataFromBluetooth")
                connectionResultChannel.trySend(
                    ConnectionResult.ConnectionEstablished(
                        dataFromBluetooth,
                        gatt.device.toBluetoothDeviceDomain()
                    )
                )
            }
        }
    }

    /**
     * Checks if the specified permission is granted.
     *
     * @param permission The permission to check (e.g., Manifest.permission.BLUETOOTH_SCAN).
     * @return `true` if the permission is granted, `false` otherwise.
     */
    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Converts a [BluetoothDevice] to a [BluetoothDeviceDomain] model.
     *
     * @param rssi The signal strength of the device, optional.
     * @return A [BluetoothDeviceDomain] representing the BLE device.
     */
    private fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int? = null): BluetoothDeviceDomain {
        return BluetoothDeviceDomain(
            name = this.name ?: "Unknown",
            address = this.address,
            signalStrength = rssi
        )
    }

    /**
     * Enables notifications for a specific GATT characteristic.
     *
     * @param characteristic The [BluetoothGattCharacteristic] to enable notifications for.
     */
    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = CCCD_DESCRIPTOR_UUID
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            bluetoothGatt?.setCharacteristicNotification(characteristic, true)
            cccdDescriptor.value = payload
            bluetoothGatt?.writeDescriptor(cccdDescriptor)
        }
    }

    /**
     * Finds a specific GATT characteristic within a given service.
     *
     * @param serviceUUID The UUID of the BLE service.
     * @param characteristicUUID The UUID of the BLE characteristic.
     * @return The [BluetoothGattCharacteristic] if found, `null` otherwise.
     */
    private fun findCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID
    ): BluetoothGattCharacteristic? {
        return bluetoothGatt?.services?.find { service ->
            service.uuid == serviceUUID
        }?.characteristics?.find { characteristic ->
            characteristic.uuid == characteristicUUID
        }
    }
}
