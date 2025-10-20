package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
        private val textViewOrderStatus: TextView = itemView.findViewById(R.id.textViewOrderStatus)
        private val textViewOrderTotal: TextView = itemView.findViewById(R.id.textViewOrderTotal)
        private val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)

        fun bind(order: OrderDTO) {
            textViewOrderNumber.text = "Order #${order.order_number}"
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
            val color = when (status.lowercase()) {
                "pending" -> R.color.status_pending
                "confirmed" -> R.color.status_confirmed
                "preparing" -> R.color.status_preparing
                "ready" -> R.color.status_ready
                "delivered" -> R.color.status_delivered
                "cancelled" -> R.color.status_cancelled
                else -> R.color.text_secondary
            }
            
            textViewOrderStatus.setTextColor(
                itemView.context.resources.getColor(color, itemView.context.theme)
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