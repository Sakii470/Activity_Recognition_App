package com.example.activityrecognitionapp.domain.usecases.Bluetooth

import com.example.activityrecognitionapp.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * ClearErrorMessageUseCase clears any error messages in the Bluetooth repository.
 */
class ClearErrorMessageUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.clearErrorMessage()
    }
}