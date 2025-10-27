package com.grocery.admin.domain.repository

import com.grocery.admin.data.remote.dto.LoginResponse
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<LoginResponse>>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}
