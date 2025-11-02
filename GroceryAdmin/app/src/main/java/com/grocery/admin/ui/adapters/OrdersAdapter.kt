package com.grocery.admin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.OrderDto
import com.grocery.admin.databinding.ItemOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying orders in the admin panel
 */
class OrdersAdapter(
    private val onOrderClick: (OrderDto) -> Unit,
    private val onUpdateStatusClick: (OrderDto) -> Unit,
    private val onAssignDriverClick: (OrderDto) -> Unit
) : ListAdapter<OrderDto, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderDto) {
            // Order basic info - use actual order_number from backend
            binding.tvOrderNumber.text = order.orderNumber ?: "ORD-${order.id.take(8)}"
            
            // Handle customer name with better fallbacks - use customerInfo from backend
            val customerName = when {
                !order.customerInfo?.name.isNullOrBlank() -> order.customerInfo?.name
                !order.customerInfo?.email.isNullOrBlank() -> order.customerInfo?.email?.substringBefore('@')
                else -> "Guest Customer"
            }
            binding.tvCustomerName.text = customerName
            
            // Show email only if available
            val customerEmail = order.customerInfo?.email
            if (!customerEmail.isNullOrBlank()) {
                binding.tvCustomerEmail.text = customerEmail
                binding.tvCustomerEmail.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCustomerEmail.text = "No email"
                binding.tvCustomerEmail.visibility = android.view.View.VISIBLE
            }
            
            binding.tvTotalAmount.text = formatCurrency(order.totalAmount)
            binding.tvOrderDate.text = formatDate(order.createdAt)
            
            // Order items count
            binding.tvItemsCount.text = "${order.items?.size ?: 0} items"
            
            // Status
            binding.tvStatus.text = order.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            // Status color
            val statusColor = getStatusColor(order.status)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, statusColor))
            binding.cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, getStatusBackgroundColor(order.status))
            )
            
            // Delivery address (first line only)
            val address = order.deliveryAddress
            val addressText = when {
                address != null -> {
                    "${address.street}${if (!address.apartment.isNullOrBlank()) ", ${address.apartment}" else ""}"
                }
                else -> "No address"
            }
            binding.tvDeliveryAddress.text = addressText
            
            // Click listeners
            binding.root.setOnClickListener { onOrderClick(order) }
            
            binding.btnUpdateStatus.setOnClickListener { onUpdateStatusClick(order) }
            
            binding.btnAssignDriver.setOnClickListener { onAssignDriverClick(order) }
            
            // Show/hide assign driver button based on status
            binding.btnAssignDriver.visibility = if (order.status == "pending") {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 2
            return format.format(amount)
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "pending" -> R.color.status_pending
                "confirmed" -> R.color.status_confirmed
                "out_for_delivery" -> R.color.primary
                "arrived" -> R.color.primary
                "delivered" -> R.color.status_delivered
                "cancelled" -> R.color.status_cancelled
                else -> R.color.text_secondary
            }
        }

        private fun getStatusBackgroundColor(status: String): Int {
            return when (status.lowercase()) {
                "pending" -> R.color.surface_variant
                "confirmed" -> R.color.surface_variant
                "out_for_delivery" -> R.color.surface_variant
                "arrived" -> R.color.surface_variant
                "delivered" -> R.color.surface_variant
                "cancelled" -> R.color.surface_variant
                else -> R.color.surface
            }
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<OrderDto>() {
        override fun areItemsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderDto, newItem: OrderDto): Boolean {
            return oldItem == newItem
        }
    }
}