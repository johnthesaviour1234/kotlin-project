package com.grocery.admin.data.repository

import android.util.Log
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.DashboardMetrics
import com.grocery.admin.domain.repository.DashboardRepository
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DashboardRepository {
    
    companion object {
        private const val TAG = "DashboardRepositoryImpl"
    }
    
    override fun getDashboardMetrics(range: String): Flow<Resource<DashboardMetrics>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching dashboard metrics for range: $range")
            
            val response = apiService.getDashboardMetrics(range)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Dashboard metrics fetched successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch metrics"
                Log.e(TAG, "Failed to fetch metrics: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching metrics: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
}
