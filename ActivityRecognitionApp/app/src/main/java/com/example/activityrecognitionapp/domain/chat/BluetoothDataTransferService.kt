package com.example.activityrecognitionapp.domain.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.util.UUID

@Suppress("UNREACHABLE_CODE")
@SuppressLint("MissingPermission")
class BluetoothDataTransferService(private val gatt: BluetoothGatt) {

    fun listenForIncomingData(serviceUUID: UUID, characteristicUUID: UUID): Flow<BluetoothData> {
        return callbackFlow {
            // Check if the BluetoothGatt instance is not null and connected
            if (BluetoothGatt.STATE_CONNECTED != BluetoothProfile.STATE_CONNECTED) {
                close() // Close the flow if not connected
                return@callbackFlow
            }

            // Get the characteristic to read from
            val characteristic = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
                ?: run {
                    close() // Close the flow if characteristic is not found
                    return@callbackFlow
                }

            // Enable notifications for the characteristic if necessary
            gatt.setCharacteristicNotification(characteristic, true)
            
        }
    }
}