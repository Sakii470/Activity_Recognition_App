package com.example.activityrecognitionapp.domain.usecase.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    suspend operator fun invoke(name: String, email: String, password: String): Result<Unit> {

        return supabaseAuthRepository.signUp(name,email,password)
    }
}