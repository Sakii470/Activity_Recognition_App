package com.example.activityrecognitionapp.domain.usecase.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {

    suspend operator fun invoke() : Result<Unit>{

        return supabaseAuthRepository.logout()
    }
}