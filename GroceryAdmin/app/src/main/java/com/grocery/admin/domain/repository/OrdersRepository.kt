package com.grocery.admin.domain.repository

import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    fun getOrders(page: Int = 1, limit: Int = 20, status: String? = null): Flow<Resource<OrdersListResponse>>
    fun getOrderById(orderId: String): Flow<Resource<OrderDto>>
    fun updateOrderStatus(orderId: String, status: String, notes: String? = null): Flow<Resource<OrderDto>>
    fun assignOrder(orderId: String, deliveryPersonnelId: String, estimatedMinutes: Int = 30): Flow<Resource<AssignOrderResponse>>
}
