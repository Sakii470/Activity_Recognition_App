package com.example.activityrecognitionapp.domain.usecase.Authorization

import com.example.activityrecognitionapp.domain.repository.SupabaseAuthRepository
import io.github.jan.supabase.gotrue.user.UserInfo
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val supabaseAuthRepository: SupabaseAuthRepository
) {
    suspend operator fun invoke(): UserInfo? {
        return supabaseAuthRepository.getCurrentUser()
    }
}