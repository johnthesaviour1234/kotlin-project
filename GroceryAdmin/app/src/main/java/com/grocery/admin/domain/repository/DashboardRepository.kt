package com.grocery.admin.domain.repository

import com.grocery.admin.data.remote.dto.DashboardMetrics
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardMetrics(range: String = "7d"): Flow<Resource<DashboardMetrics>>
}
