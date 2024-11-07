package com.example.activityrecognitionapp.data.bluetooth

/**
 * AndroidBluetoothController is a class responsible for managing Bluetooth Low Energy (BLE) operations
 * within an Android application. It implements the BluetoothController interface and provides functionality
 * for discovering, connecting, and communicating with BLE devices. This class handles various tasks,
 * including scanning for devices, connecting to GATT servers, discovering available services and characteristics,
 * and enabling notifications from a BLE device.
 *
 * Key features include:
 * - **Device Discovery**: Scans for nearby BLE devices and updates the list of discovered devices.
 * - **Connection Management**: Connects to and manages connections with BLE devices, supporting connection state changes.
 * - **Data Transfer**: Reads data from BLE characteristics and enables notifications for real-time data updates.
 * - **Error Handling**: Provides shared flows to manage and emit errors related to BLE operations.
 *
 * The class makes use of coroutines to handle asynchronous tasks and channels for transferring connection results.
 * It also utilizes Android's BluetoothManager and BluetoothGatt classes to manage the BLE connection lifecycle,
 * ensuring seamless and efficient Bluetooth communication.
 *
 * This controller is designed for BLE interactions specifically and requires Android permissions
 * for Bluetooth operations such as scanning and connecting.
 */



import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.activityrecognitionapp.domain.BluetoothController

import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null

    private val uuidService: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
    private val uuidCharacteristic: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

//    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
//        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val connectionResultChannel = Channel<ConnectionResult>(Channel.BUFFERED)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    //The leScanCallback processes Bluetooth LE scan results, updating the list of scanned devices by adding a new device if it does not already exist in the list.
    //It is call after press scanButton
    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            if (device.name != null && device.name.isNotEmpty()) {
                val bluetoothDeviceDomain = device.toBluetoothDeviceDomain(rssi)
                _scannedDevices.update { devices ->
                    // Check if the device already exists in the list
                    if (devices.any { it.address == bluetoothDeviceDomain.address }) {
                        // If the device exists, return the unchanged list
                        devices
                    } else {
                        // If the device is not in the list, add it
                        devices + bluetoothDeviceDomain
                    }
                }
            }
        }
    }

    // Defines a receiver for found Bluetooth devices, updating the list of scanned devices.
    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain(rssi = null)
            if (newDevice in devices) devices
            else devices + newDevice
        }
    }

    //Update connection status
    private var bluetoothStateReceiver = BluetoothStateReceiver { isConnected, _ ->
        _isConnected.update { isConnected }
    }

    init {
        context.registerReceiver( // Register the bluetoothStateReceiver to listen for Bluetooth connection state changes.
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        )
    }

    //startDiscovery - Initiates Bluetooth device scanning and registers a receiver to handle found devices.
    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        // Register a receiver to listen for discovered Bluetooth devices.
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback) // Start scanning for Bluetooth Low Energy (BLE) devices using the provided callback.
        coroutineScope.launch {// Launch a coroutine to stop scanning after a specified delay.
            delay(10000)
            stopDiscovery()
        }
    }

    // stopDiscovery - Stops Bluetooth device scanning
    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)

    }

    // gattCallback - Defines Bluetooth GATT event handling, managing connection states and characteristic data.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionResultChannel.trySend(ConnectionResult.Error("Disconnected"))
                gatt.close()
            } else {
                connectionResultChannel.trySend(ConnectionResult.Error("Error.Try Again."))
                gatt.close()
            }
        }

        // onServicesDiscovered - Triggered upon discovering services; prints the GATT table and adjusts MTU size.
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable() // Print the GATT table to log the discovered services and characteristics.
                gatt.requestMtu(517) // Request an MTU (Maximum Transmission Unit) size of 517 bytes for data transfer
            }
        }

        // onMtuChanged - Called after the MTU size is changed; locates a specific characteristic and enables notification if found.
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic =
                findCharacteristics(uuidService.toString(), uuidCharacteristic.toString())
            if (characteristic == null) { // If the characteristic is not found, launch a coroutine to send an error message through the connection result channel.
                coroutineScope.launch {
                    connectionResultChannel.trySend(
                        ConnectionResult.ConnectionEstablished("Can't find characteristic. Choose another device",null)
                    )
                }
                return
            }
            enableNotification(characteristic)
        }

        //onCharacteristicChanged - Listens for changes in characteristic values; if the characteristic matches the defined UUID, decodes data and sends it via the connectionResultChannel.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                when (uuid) {
                    uuidCharacteristic -> {
                        val datafromBluetooth = value
                        coroutineScope.launch {// Launch a coroutine to send the decoded data through the connection result channel.
                            val connectedDevice = gatt.device
                            connectionResultChannel.trySend(
                                ConnectionResult.ConnectionEstablished(
                                    datafromBluetooth.decodeToString(),
                                    connectedDevice.toBluetoothDeviceDomain()
                                )
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    // enableNotification - Enables notifications for a specified Bluetooth GATT characterist
    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    // writeDescription - Writes a descriptor value to enable notifications or indications for a characteristic.
    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    // findCharacteristics - Finds and returns a Bluetooth GATT characteristic based on provided service and characteristic UUIDs
    private fun findCharacteristics(
        serviceUUID: String,
        characteristicsUUID: String
    ): BluetoothGattCharacteristic? {
        return bluetoothGatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    //connectToDevice - Initiates a connection to a specified Bluetooth device and emits connection results.
    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {

        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("Permission denied")
            }

            bluetoothAdapter?.let {
                try {
                    val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

                    bluetoothGatt =
                        bluetoothDevice?.connectGatt(context, false, gattCallback)

                    for (result in connectionResultChannel) {
                        emit(result) // Emit Connection outcomes
                        updateScannedDevices()
                    }


                } catch (exception: IllegalArgumentException) {

                    emit(ConnectionResult.Error("Device not found with provided address."))

                }
                // connect to the GATT server on the device
            } ?: run {
                emit(ConnectionResult.Error("BluetoothAdapter not initialized"))
            }
        }
    }

    // closeConnection - Disconnects and closes the Bluetooth GATT connection.
    override fun closeConnection() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // updatePairedDevices - Retrieves and updates the list of currently paired Bluetooth devices
//    private fun updatePairedDevices() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return
//        }
//        bluetoothAdapter
//            ?.bondedDevices
//            ?.map { it.toBluetoothDeviceDomain() }
//            ?.also { devices ->
//                _pairedDevices.update { devices }
//            }
//    }

    // updateScannedDevices - Clears the current list of scanned devices and restarts the discovery process.
    private fun updateScannedDevices() {
        _scannedDevices.update { emptyList() }
        startDiscovery()
    }

    // hasPermission - Checks if the required Bluetooth permission is granted in the context.
    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}




