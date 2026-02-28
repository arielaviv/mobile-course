package com.lux.field.data.repository

import com.lux.field.BuildConfig
import com.lux.field.data.mock.MockDataProvider
import com.lux.field.data.remote.LuxApi
import com.lux.field.data.remote.dto.LoginRequest
import com.lux.field.domain.model.CrewMember
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: LuxApi,
    private val tokenProvider: TokenProvider,
    private val mockDataProvider: MockDataProvider,
) {
    suspend fun login(phone: String, code: String): Result<CrewMember> {
        return try {
            if (BuildConfig.USE_MOCK_API) {
                val mockUser = mockDataProvider.mockLogin(phone, code)
                tokenProvider.saveTokens("mock_token_${System.currentTimeMillis()}", "mock_refresh")
                tokenProvider.saveUserInfo(mockUser.id, mockUser.name, mockUser.crewId)
                Result.success(mockUser)
            } else {
                val response = api.login(LoginRequest(phone, code))
                tokenProvider.saveTokens(response.token, response.refreshToken)
                val user = response.user
                tokenProvider.saveUserInfo(user.id, user.name, user.crewId)
                Result.success(
                    CrewMember(
                        id = user.id,
                        name = user.name,
                        phone = user.phone,
                        role = user.role,
                        crewId = user.crewId,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, phone: String): Result<String> {
        return try {
            if (BuildConfig.USE_MOCK_API) {
                val code = mockDataProvider.register(name, phone)
                Result.success(code)
            } else {
                Result.failure(UnsupportedOperationException("Registration not available in production yet"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyRegistration(phone: String, code: String): Result<CrewMember> {
        return try {
            if (BuildConfig.USE_MOCK_API) {
                val user = mockDataProvider.verifyRegistration(phone, code)
                tokenProvider.saveTokens("mock_token_${System.currentTimeMillis()}", "mock_refresh")
                tokenProvider.saveUserInfo(user.id, user.name, user.crewId)
                Result.success(user)
            } else {
                Result.failure(UnsupportedOperationException("Registration not available in production yet"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = tokenProvider.isLoggedIn()

    fun getUserName(): String = tokenProvider.getUserName()

    fun logout() {
        tokenProvider.clear()
    }
}
