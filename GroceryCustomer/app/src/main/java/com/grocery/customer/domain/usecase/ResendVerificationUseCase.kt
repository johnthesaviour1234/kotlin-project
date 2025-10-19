package com.grocery.customer.domain.usecase

import com.grocery.customer.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.resendVerification(email)
}