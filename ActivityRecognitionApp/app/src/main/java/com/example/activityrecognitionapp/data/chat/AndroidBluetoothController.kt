package com.example.activityrecognitionapp.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.activityrecognitionapp.domain.chat.BluetoothController
import com.example.activityrecognitionapp.domain.chat.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi


            if (device.name != null && device.name.isNotEmpty()) {
                val bluetoothDeviceDomain = device.toBluetoothDeviceDomain(rssi)
                _scannedDevices.update { devices ->
                    // Sprawdź, czy urządzenie już istnieje na liście
                    if (devices.any { it.address == bluetoothDeviceDomain.address }) {
                        // Jeśli urządzenie już istnieje, zwróć niezmienioną listę
                        devices
                    } else {
                        // Jeśli urządzenia nie ma na liście, dodaj je
                        devices + bluetoothDeviceDomain
                    }
                }
            }
        }
    }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

//    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
//        _scannedDevices.update { devices ->
//            val newDevice = device.toBluetoothDeviceDomain()
//            if(newDevice in devices) devices else devices + newDevice
//        }
//    }

    private var bluetoothStateReceiver = BluetoothStateReceiver { isConnected,bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true){
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("Can't connect to device")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply{
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        )
    }

//    override fun startDiscovery() {
//        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            return
//        }
//
//        context.registerReceiver(
//            foundDeviceReceiver,
//            IntentFilter(BluetoothDevice.ACTION_FOUND)
//        )
//
//        updatePairedDevices()
//
//        bluetoothAdapter?.startDiscovery()
//    }

override fun startDiscovery() {
    if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
        return
    }

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    bluetoothAdapter?.bluetoothLeScanner?.startScan(null, settings, leScanCallback)
}

//    override fun stopDiscovery() {
//        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            return
//        }
//
//        bluetoothAdapter?.cancelDiscovery()
//    }

override fun stopDiscovery() {
    if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
        return
    }

    bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
}

    override fun startBluetoothServer(): Flow<ConnectionResult> {
      return flow {
          if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
              throw SecurityException("Permission denied")

          }
         currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "Data_service",
              UUID.fromString(Service_UUID)
          )

          var shouldLoop = true;
          while(shouldLoop){
              currentClientSocket = try {
                    currentServerSocket?.accept()

              }catch (e: IOException){
                  shouldLoop = false
                  null
              }
              emit(ConnectionResult.ConnectionEstabilished)
              currentClientSocket?.let {
                  currentServerSocket?.close()
              }
          }
      }.onCompletion {
          closeConnection()
      }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("Permission denied")
            }

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)


            currentClientSocket= bluetoothDevice

                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(Service_UUID)
                )

            stopDiscovery()

            if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false){

            }

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstabilished)

                } catch (e: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interapted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket= null
        currentServerSocket= null
    }

//    override fun release() {
//        context.unregisterReceiver(foundDeviceReceiver)
//    }

//    private fun updatePairedDevices() {
//        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return
//        }
//        bluetoothAdapter
//            ?.bondedDevices
//            ?.map { it.toBluetoothDeviceDomain() }
//            ?.also { devices ->
//                _pairedDevices.update { devices }
//            }
//    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val Service_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}