package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.RegisterResponse
import com.grocery.customer.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        fullName: String,
        phone: String?,
        userType: String
    ): Result<RegisterResponse> = repository.register(email, password, fullName, phone, userType)
}