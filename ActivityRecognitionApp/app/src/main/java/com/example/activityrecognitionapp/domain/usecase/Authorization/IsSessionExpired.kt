package com.example.activityrecognitionapp.domain.usecase.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

class IsSessionExpiredUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return supabaseAuthRepository.isSessionExpired()
    }
}