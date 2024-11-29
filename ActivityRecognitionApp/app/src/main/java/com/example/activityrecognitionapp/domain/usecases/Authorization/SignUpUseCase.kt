package com.example.activityrecognitionapp.domain.usecases.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

/**
 * Use case for signing up a new user.
 *
 * @param supabaseAuthRepository Repository handling authentication with Supabase.
 */
class SignUpUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {
    /**
     * Executes the sign-up process with the provided user details.
     *
     * @param name The name of the user.
     * @param email The email address of the user.
     * @param password The password for the user account.
     * @return Result indicating success or failure of the sign-up operation.
     */
    suspend operator fun invoke(name: String, email: String, password: String): Result<Unit> {
        return supabaseAuthRepository.signUp(name, email, password)
    }
}
