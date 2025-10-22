package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.OrderDTO
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying order history list.
 * Shows order summary with status, date, total, and item count.
 */
class OrderHistoryAdapter(
    private val onOrderClick: (String) -> Unit
) : ListAdapter<OrderDTO, OrderHistoryAdapter.OrderHistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_history, parent, false)
        return OrderHistoryViewHolder(view, onOrderClick)
    }

    override fun onBindViewHolder(holder: OrderHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderHistoryViewHolder(
        itemView: View,
        private val onOrderClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val textViewOrderNumber: TextView = itemView.findViewById(R.id.textViewOrderNumber)
        private val textViewOrderDate: TextView = itemView.findViewById(R.id.textViewOrderDate)
        private val textViewOrderStatus: Chip = itemView.findViewById(R.id.textViewOrderStatus)
        private val textViewOrderTotal: TextView = itemView.findViewById(R.id.textViewOrderTotal)
        private val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)

        fun bind(order: OrderDTO) {
            // Set order number with fallback to ID if order_number is empty
            val displayOrderNumber = if (order.order_number.isNullOrEmpty()) {
                order.id.take(8).uppercase()
            } else {
                order.order_number
            }
            textViewOrderNumber.text = displayOrderNumber
            
            // Log for debugging
            android.util.Log.d("OrderHistoryAdapter", "Order: order_number='${order.order_number}', displayOrderNumber='$displayOrderNumber'")
            
            textViewOrderDate.text = formatDate(order.created_at)
            textViewOrderStatus.text = order.status.replaceFirstChar { it.uppercase() }
            textViewOrderTotal.text = itemView.context.getString(R.string.price_format, order.total_amount)
            
            // Calculate item count from order items
            val itemCount = order.order_items?.sumOf { it.quantity } ?: 0
            textViewItemCount.text = itemView.context.resources.getQuantityString(
                R.plurals.item_count, itemCount, itemCount
            )
            
            // Set status color
            setStatusColor(order.status)
            
            // Handle click
            itemView.setOnClickListener {
                onOrderClick(order.id)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }

        private fun setStatusColor(status: String) {
            val (colorResId, labelText) = when (status.lowercase()) {
                "pending" -> Pair(R.color.status_pending, "PENDING")
                "confirmed" -> Pair(R.color.status_confirmed, "CONFIRMED")
                "preparing" -> Pair(R.color.status_preparing, "PREPARING")
                "ready" -> Pair(R.color.status_ready, "READY")
                "delivered" -> Pair(R.color.status_delivered, "DELIVERED")
                "cancelled" -> Pair(R.color.status_cancelled, "CANCELLED")
                else -> Pair(R.color.text_secondary, status.uppercase())
            }
            
            textViewOrderStatus.setChipBackgroundColorResource(colorResId)
            textViewOrderStatus.text = labelText
            textViewOrderStatus.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderDTO>() {
        override fun areItemsTheSame(oldItem: OrderDTO, newItem: OrderDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderDTO, newItem: OrderDTO): Boolean {
            return oldItem == newItem
        }
    }
}