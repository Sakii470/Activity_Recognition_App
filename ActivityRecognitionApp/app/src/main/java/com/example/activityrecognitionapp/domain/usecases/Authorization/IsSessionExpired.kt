package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

/**
 * Use case to check if the current user session has expired.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class IsSessionExpiredUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    /**
     * Checks whether the current user session is expired.
     *
     * @return Boolean indicating if the session has expired.
     */
    suspend operator fun invoke(): Boolean {
        return supabaseAuthRepository.isSessionExpired()
    }
}
