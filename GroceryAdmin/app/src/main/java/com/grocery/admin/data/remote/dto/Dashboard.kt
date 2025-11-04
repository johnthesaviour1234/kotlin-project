package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardMetrics(
    @SerializedName("summary") val summary: DashboardSummary,
    @SerializedName("orders_by_status") val ordersByStatus: Map<String, Int>?,
    @SerializedName("recent_orders") val recentOrders: List<RecentOrder>?,
    @SerializedName("time_range") val timeRange: String?
) {
    // Convenience properties for easy access to summary fields
    val totalOrders: Int get() = summary.totalOrders
    val totalRevenue: Double get() = summary.totalRevenue
    val averageOrderValue: Double get() = summary.averageOrderValue
    val activeCustomers: Int get() = summary.activeCustomers
    val pendingOrders: Int get() = summary.pendingOrders
    val lowStockItems: Int? get() = summary.lowStockItems
}

data class DashboardSummary(
    @SerializedName("total_orders") val totalOrders: Int,
    @SerializedName("total_revenue") val totalRevenue: Double,
    @SerializedName("average_order_value") val averageOrderValue: Double,
    @SerializedName("active_customers") val activeCustomers: Int,
    @SerializedName("active_delivery_personnel") val activeDeliveryPersonnel: Int?,
    @SerializedName("pending_orders") val pendingOrders: Int,
    @SerializedName("low_stock_items") val lowStockItems: Int?
)

data class RecentOrder(
    @SerializedName("id") val id: String,
    @SerializedName("total") val total: Double,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class TrendData(
    @SerializedName("date") val date: String,
    @SerializedName("value") val value: Double
)
