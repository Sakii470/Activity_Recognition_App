package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

/**
 * Use case for logging in a user with email and password.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class LoginUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    /**
     * Executes the login process with the provided credentials.
     *
     * @param email The email address of the user.
     * @param password The password for the user account.
     * @return Result indicating success or failure of the login operation.
     */
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return supabaseAuthRepository.login(email, password)
    }
}
