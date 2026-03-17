package com.field.survey.domain.usecase

import com.field.survey.data.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password is required"))
        return authRepository.login(email, password)
    }
}
