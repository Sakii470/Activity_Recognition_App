package com.example.activityrecognitionapp.domain.usecases.Bluetooth

import com.example.activityrecognitionapp.domain.BluetoothDeviceDomain
import com.example.activityrecognitionapp.domain.ConnectionResult
import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ConnectToDeviceUseCase handles connecting to a specific Bluetooth device.
 */
class ConnectToDeviceUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return repository.connectToDevice(device)
    }
}