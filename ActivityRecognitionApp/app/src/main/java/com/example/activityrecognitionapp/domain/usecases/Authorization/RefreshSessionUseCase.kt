package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

/**
 * Use case for refreshing the current user session.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class RefreshSessionUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    /**
     * Executes the session refresh process.
     *
     * @return Result indicating success or failure of the session refresh operation.
     */
    suspend operator fun invoke(): Result<Unit> {
        return supabaseAuthRepository.refreshSession()
    }
}
