package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardMetrics(
    @SerializedName("total_orders") val totalOrders: Int,
    @SerializedName("total_revenue") val totalRevenue: Double,
    @SerializedName("average_order_value") val averageOrderValue: Double,
    @SerializedName("active_customers") val activeCustomers: Int,
    @SerializedName("pending_orders") val pendingOrders: Int,
    @SerializedName("confirmed_orders") val confirmedOrders: Int,
    @SerializedName("out_for_delivery_orders") val outForDeliveryOrders: Int,
    @SerializedName("delivered_orders") val deliveredOrders: Int,
    @SerializedName("cancelled_orders") val cancelledOrders: Int,
    @SerializedName("low_stock_items") val lowStockItems: Int?,
    @SerializedName("revenue_trend") val revenueTrend: List<TrendData>?,
    @SerializedName("orders_trend") val ordersTrend: List<TrendData>?
)

data class TrendData(
    @SerializedName("date") val date: String,
    @SerializedName("value") val value: Double
)
