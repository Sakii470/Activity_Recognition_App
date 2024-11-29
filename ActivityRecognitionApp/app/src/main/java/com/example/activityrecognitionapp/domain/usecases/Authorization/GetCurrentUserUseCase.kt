package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import io.github.jan.supabase.gotrue.user.UserInfo
import javax.inject.Inject

/**
 * Use case to retrieve the currently authenticated user's information.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {
    /**
     * Retrieves the current user's information.
     *
     * @return UserInfo object containing user details or null if no user is authenticated.
     */
    suspend operator fun invoke(): UserInfo? {
        return supabaseAuthRepository.getCurrentUser()
    }
}
