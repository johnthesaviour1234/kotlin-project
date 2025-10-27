package com.grocery.admin.domain.repository

import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getInventory(lowStock: Boolean? = null, threshold: Int = 10): Flow<Resource<InventoryListResponse>>
    fun updateInventory(request: UpdateInventoryRequest): Flow<Resource<UpdateInventoryResponse>>
}
