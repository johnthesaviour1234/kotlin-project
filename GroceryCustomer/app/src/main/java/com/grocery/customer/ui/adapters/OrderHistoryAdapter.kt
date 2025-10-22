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
            
            // Comprehensive debug logging
            android.util.Log.d("OrderHistoryAdapter", """BIND ORDER DATA:
            |  ID: ${order.id}
            |  order_number: '${order.order_number}' (is null: ${order.order_number == null}, is empty: ${order.order_number?.isEmpty()})
            |  displayOrderNumber: '$displayOrderNumber'
            |  status: ${order.status}
            |  created_at: ${order.created_at}
            |  total_amount: ${order.total_amount}
            |  order_items count: ${order.order_items?.size}
            """.trimMargin())
            
            textViewOrderNumber.text = displayOrderNumber
            android.util.Log.d("OrderHistoryAdapter", "textViewOrderNumber.text set to: '${textViewOrderNumber.text}'")
            
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
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", // with millis and timezone
                "yyyy-MM-dd'T'HH:mm:ssXXX",     // with timezone
                "yyyy-MM-dd'T'HH:mm:ss.SSS",    // millis, no timezone (assume UTC)
                "yyyy-MM-dd'T'HH:mm:ss"         // no timezone (assume UTC)
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    if (!pattern.contains("X")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(dateString)
                    if (date != null) return outputFormat.format(date)
                } catch (_: Exception) { /* try next */ }
            }
            return dateString
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
            textViewOrderStatus.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.on_primary)
            )
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