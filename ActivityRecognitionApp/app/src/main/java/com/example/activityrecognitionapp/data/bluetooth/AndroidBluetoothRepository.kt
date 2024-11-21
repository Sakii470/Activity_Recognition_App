package com.example.activityrecognitionapp.data.bluetooth

/**
 * AndroidBluetoothRepository is a class responsible for managing Bluetooth Low Energy (BLE) operations
 * within an Android application. It implements the BluetoothRepository interface and provides functionality
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

@SuppressLint("MissingPermission")
class AndroidBluetoothRepository(
    private val context: Context,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
) : BluetoothRepository {


    private val bluetoothAdapter: BluetoothAdapter? = bluetoothAdapterProvider.getBluetoothAdapter()
    private var bluetoothGatt: BluetoothGatt? = null

    private val uuidService: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
    private val uuidCharacteristic: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")
    private val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val connectionResultChannel = Channel<ConnectionResult>(Channel.BUFFERED)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Scan Callback
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

    // BroadcastReceiver to monitor Bluetooth state
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
        // Register BluetoothStateReceiver
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothStateReceiver, intentFilter)
    }

    override fun startScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            coroutineScope.launch {
                _errors.emit("Missing BLUETOOTH_SCAN permission.")
            }
            return
        }
        //reset scannedDevice list
        _scannedDevices.value = emptyList()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
        coroutineScope.launch {
            delay(10000) // Scan for 10 seconds
            stopScan()
        }
    }

    override fun stopScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            coroutineScope.launch {
                _errors.emit("Missing BLUETOOTH_SCAN permission.")
            }
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

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

    override fun disconnect() {
        bluetoothGatt?.disconnect()
        // Do not call close() immediately; wait for onConnectionStateChange
    }

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



    override fun isBluetoothEnabled(): Boolean {
        return _isBluetoothEnabled.value
    }

    override fun clearErrorMessage() {
        coroutineScope.launch {
            _errors.emit("")
        }
    }

    // GATT Callback
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

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int? = null): BluetoothDeviceDomain {
        return BluetoothDeviceDomain(
            name = this.name ?: "Unknown",
            address = this.address,
            signalStrength = rssi
        )
    }

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




//    private val bluetoothAdapter = bluetoothAdapterProvider.getBluetoothAdapter()
//
////    private val bluetoothManager by lazy {
////        context.getSystemService(BluetoothManager::class.java)
////    }
////    private val bluetoothAdapter by lazy {
////        bluetoothManager?.adapter
////    }
//private var bluetoothGatt: BluetoothGatt? = null
//
//    private val uuidService: UUID = UUID.fromString("020c6211-64f6-4e6d-82e8-c2c1391f75fa")
//    private val uuidCharacteristic: UUID = UUID.fromString("020c6213-64f6-4e6d-82e8-c2c1391f75fa")
//    private val CCCD_DESCRIPTOR_UUID: String = "00002902-0000-1000-8000-00805f9b34fb"
//
//    private val _isConnected = MutableStateFlow(false)
//    override val isConnected: StateFlow<Boolean>
//        get() = _isConnected.asStateFlow()
//
//    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
//    override val isBluetoothEnabled: StateFlow<Boolean>
//        get() = _isBluetoothEnabled.asStateFlow()
//
//    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
//        get() = _scannedDevices.asStateFlow()
//
//    private val _errors = MutableSharedFlow<String>()
//    override val errors: SharedFlow<String>
//        get() = _errors.asSharedFlow()
//
//    private val connectionResultChannel = Channel<ConnectionResult>(Channel.BUFFERED)
//
//    private val coroutineScope = CoroutineScope(Dispatchers.Default)
//
//    // Scan Callback
//    private val leScanCallback = object : ScanCallback() {
//        @RequiresApi(Build.VERSION_CODES.O)
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            val device = result.device
//            val rssi = result.rssi
//
//            Log.d(
//                "BLE Scan",
//                "Znaleziono urządzenie: ${device.name} - ${device.address} RSSI: $rssi"
//            )
//
//            if (device.name != null && device.name.isNotEmpty()) {
//                val bluetoothDeviceDomain = device.toBluetoothDeviceDomain(rssi)
//                Log.d(
//                    "BLE Scan",
//                    "Konwertowano urządzenie: ${bluetoothDeviceDomain.name} - ${bluetoothDeviceDomain.address}"
//                )
//
//                _scannedDevices.update { devices ->
//                    if (devices.any { it.address == bluetoothDeviceDomain.address }) {
//                        Log.d(
//                            "BLE Scan",
//                            "Urządzenie już istnieje na liście: ${bluetoothDeviceDomain.address}"
//                        )
//                        devices
//                    } else {
//                        Log.d(
//                            "BLE Scan",
//                            "Dodano urządzenie do listy: ${bluetoothDeviceDomain.name} - ${bluetoothDeviceDomain.address}"
//                        )
//                        devices + bluetoothDeviceDomain
//                    }
//                }
//
//                Log.d("BLE Scan", "Aktualna lista urządzeń: ${_scannedDevices.value}")
//            }
//        }
//    }
//
//    // BroadcastReceiver do monitorowania stanu Bluetooth
//    private val bluetoothStateReceiver = BluetoothStateReceiver(
//        onBluetoothStateChanged = { isEnabled ->
//            _isBluetoothEnabled.update { isEnabled }
//        },
//        onConnectionStateChanged = { isConnected, device ->
//            _isConnected.update { isConnected }
//            // Możesz dodać dodatkową logikę tutaj, jeśli potrzebujesz
//        }
//    )
//
//    init {
//        // Rejestracja BluetoothStateReceiver
//        val intentFilter = IntentFilter().apply {
//            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
//            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
//            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
//        }
//        context.registerReceiver(bluetoothStateReceiver, intentFilter)
//    }
//
//    override fun startScan() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            coroutineScope.launch {
//                _errors.emit("Missing BLUETOOTH_SCAN permission.")
//            }
//            return
//        }
//        bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
//        coroutineScope.launch {
//            delay(10000)
//            stopScan()
//        }
//    }
//
//    override fun stopScan() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            coroutineScope.launch {
//                _errors.emit("Missing BLUETOOTH_SCAN permission.")
//            }
//            return
//        }
//        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
//    }
//
//    // Odbiornik do znalezionych urządzeń
//    private val foundDeviceReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//            device?.let {
//                if (it.name != null && it.name.isNotEmpty()) {
//                    val bluetoothDeviceDomain = it.toBluetoothDeviceDomain(rssi = null)
//                    _scannedDevices.update { devices ->
//                        if (devices.any { d -> d.address == bluetoothDeviceDomain.address }) {
//                            devices
//                        } else {
//                            devices + bluetoothDeviceDomain
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // GATT Callback
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.d("BluetoothRepository", "Connected to GATT server.")
//                gatt.discoverServices()
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.d("BluetoothRepository", "Disconnected from GATT server.")
//                connectionResultChannel.trySend(ConnectionResult.Error("Disconnected"))
//                gatt.close()
//                _isConnected.update { false }
//            } else {
//                Log.d("BluetoothRepository", "Connection failed with status $status.")
//                connectionResultChannel.trySend(ConnectionResult.Error("Connection failed. Status: $status"))
//                gatt.close()
//                _isConnected.update { false }
//            }
//        }
//
//
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d("BluetoothRepository", "Services discovered.")
//                // Możesz tutaj wyszukać i skonfigurować usługi oraz charakterystyki
//                val characteristic =
//                    findCharacteristics(uuidService.toString(), uuidCharacteristic.toString())
//                if (characteristic != null) {
//                    enableNotification(characteristic)
//                } else {
//                    connectionResultChannel.trySend(ConnectionResult.Error("Characteristic not found."))
//                }
//            } else {
//                connectionResultChannel.trySend(ConnectionResult.Error("Service discovery failed with status $status."))
//            }
//        }
//
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//        ) {
//            if (characteristic.uuid == uuidCharacteristic) {
//                val dataFromBluetooth = characteristic.value.decodeToString()
//                Log.d("BluetoothRepository", "Received data: $dataFromBluetooth")
//                connectionResultChannel.trySend(
//                    ConnectionResult.ConnectionEstablished(
//                        dataFromBluetooth,
//                        gatt.device.toBluetoothDeviceDomain()
//                    )
//                )
//            }
//        }
//
//        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
//            super.onMtuChanged(gatt, mtu, status)
//            Log.d("BluetoothRepository", "MTU changed to $mtu with status $status")
//        }
//    }
//
//    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> = flow {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            throw SecurityException("Missing BLUETOOTH_CONNECT permission.")
//        }
//
//        bluetoothAdapter?.let { adapter ->
//            try {
//                val bluetoothDevice = adapter.getRemoteDevice(device.address)
//                bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
//
//                emitAll(connectionResultChannel.receiveAsFlow())
//            } catch (e: IllegalArgumentException) {
//                emit(ConnectionResult.Error("Device not found with provided address."))
//            }
//        } ?: emit(ConnectionResult.Error("BluetoothAdapter not initialized"))
//    }
//
//    override fun closeConnection() {
//        bluetoothGatt?.disconnect()
////        bluetoothGatt?.close()
////        bluetoothGatt = null
////        _isConnected.update { false }
//    }
//
//    // Pomocnicze funkcje
//    private fun hasPermission(permission: String): Boolean {
//        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun BluetoothDevice.toBluetoothDeviceDomain(rssi: Int?): BluetoothDeviceDomain {
//        return BluetoothDeviceDomain(
//            name = this.name ?: "Unknown",
//            address = this.address,
//            signalStrength = rssi
//        )
//    }
//
//    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
//        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
//        val payload = when {
//            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
//            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//            else -> return
//        }
//        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
//            bluetoothGatt?.setCharacteristicNotification(characteristic, true)
//            cccdDescriptor.value = payload
//            bluetoothGatt?.writeDescriptor(cccdDescriptor)
//        }
//    }
//
//    fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//        if (characteristic.uuid == uuidCharacteristic) {
//            val dataFromBluetooth = characteristic.value.decodeToString()
//            Log.d("BluetoothRepository", "Received data: $dataFromBluetooth")
//            connectionResultChannel.trySend(
//                ConnectionResult.ConnectionEstablished(
//                    dataFromBluetooth,
//                    gatt.device.toBluetoothDeviceDomain()
//                )
//            )
//        }
//    }
//
//    private fun findCharacteristics(
//        serviceUUID: String,
//        characteristicsUUID: String,
//    ): BluetoothGattCharacteristic? {
//        return bluetoothGatt?.services?.find { service ->
//            service.uuid.toString() == serviceUUID
//        }?.characteristics?.find { characteristic ->
//            characteristic.uuid.toString() == characteristicsUUID
//        }
//    }
//
//    // Unregister BroadcastReceiver przy niszczeniu kontrolera
//    fun unregister() {
//        try {
//            context.unregisterReceiver(bluetoothStateReceiver)
//            context.unregisterReceiver(foundDeviceReceiver)
//        } catch (e: IllegalArgumentException) {
//            Log.e("BluetoothRepository", "Receiver not registered", e)
//        }
//    }
//
//
//    // In AndroidBluetoothRepository.kt
//    @SuppressLint("MissingPermission")
//    override fun connectToPairedDeviceWithCharacteristic(
//        characteristicCheck: (BluetoothDeviceDomain) -> Boolean,
//    ): Flow<ConnectionResult> = flow {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            throw SecurityException("Missing BLUETOOTH_CONNECT permission.")
//        }
//
//        val pairedDevices = bluetoothAdapter?.bondedDevices
//        if (pairedDevices != null) {
//            val targetDevice = pairedDevices
//                .map { it.toBluetoothDeviceDomain(rssi = null) }
//                .find { deviceDomain -> characteristicCheck(deviceDomain) }
//
//            if (targetDevice != null) {
//                // Proceed to connect to the device
//                emitAll(connectToDevice(targetDevice))
//            } else {
//                emit(ConnectionResult.Error("No paired device with specified characteristics found."))
//            }
//        } else {
//            emit(ConnectionResult.Error("No paired devices found."))
//        }
//    }
//
//
//}








