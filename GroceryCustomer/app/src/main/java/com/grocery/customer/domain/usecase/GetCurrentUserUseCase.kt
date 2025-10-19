package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.UserProfileDto
import com.grocery.customer.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserProfileDto> = repository.getCurrentUser()
}