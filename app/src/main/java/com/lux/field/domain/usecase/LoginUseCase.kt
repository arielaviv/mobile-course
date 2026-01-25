package com.lux.field.domain.usecase

import com.lux.field.data.repository.AuthRepository
import com.lux.field.domain.model.CrewMember
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(phone: String, code: String): Result<CrewMember> {
        if (phone.isBlank()) return Result.failure(IllegalArgumentException("Phone number is required"))
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Code is required"))
        return authRepository.login(phone, code)
    }
}
