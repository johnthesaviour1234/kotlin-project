package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.LoginResponse
import com.grocery.customer.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<LoginResponse> =
        repository.login(email, password)
}