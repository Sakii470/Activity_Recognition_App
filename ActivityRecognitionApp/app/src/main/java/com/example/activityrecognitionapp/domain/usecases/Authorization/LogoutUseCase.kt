package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

/**
 * Use case for logging out the current user.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class LogoutUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    /**
     * Executes the logout process.
     *
     * @return Result indicating success or failure of the logout operation.
     */
    suspend operator fun invoke(): Result<Unit> {
        return supabaseAuthRepository.logout()
    }
}
