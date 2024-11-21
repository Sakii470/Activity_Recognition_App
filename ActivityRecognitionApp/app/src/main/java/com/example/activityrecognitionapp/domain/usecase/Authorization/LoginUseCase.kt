package com.example.activityrecognitionapp.domain.usecase.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository) {



    suspend operator fun invoke(email:String, password:String) : Result<Unit>{
        // Login logic
        return supabaseAuthRepository.login(email,password)
    }

}