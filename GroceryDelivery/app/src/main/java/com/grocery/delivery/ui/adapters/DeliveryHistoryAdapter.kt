package com.grocery.delivery.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.delivery.R
import com.grocery.delivery.data.dto.DeliveryAssignment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying delivery history in RecyclerView
 */
class DeliveryHistoryAdapter(
    private val onItemClick: (DeliveryAssignment) -> Unit
) : ListAdapter<DeliveryAssignment, DeliveryHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_history, parent, false)
        return HistoryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: (DeliveryAssignment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderNumber: TextView = itemView.findViewById(R.id.tv_order_number)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tv_customer_name)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tv_delivery_address)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvDeliveryDate: TextView = itemView.findViewById(R.id.tv_delivery_date)
        private val tvDeliveryTime: TextView = itemView.findViewById(R.id.tv_delivery_time)

        fun bind(assignment: DeliveryAssignment) {
            val order = assignment.orders ?: return

            tvOrderNumber.text = order.orderNumber
            tvCustomerName.text = order.getCustomerName() ?: "Customer"
            tvDeliveryAddress.text = order.getFormattedAddress()

            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
            tvAmount.text = currencyFormatter.format(order.totalAmount)

            // Format delivery date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            try {
                val completedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                    .parse(assignment.updatedAt ?: "")
                
                if (completedDate != null) {
                    tvDeliveryDate.text = dateFormat.format(completedDate)
                    tvDeliveryTime.text = timeFormat.format(completedDate)
                } else {
                    tvDeliveryDate.text = "Completed"
                    tvDeliveryTime.text = ""
                }
            } catch (e: Exception) {
                tvDeliveryDate.text = "Completed"
                tvDeliveryTime.text = ""
            }

            itemView.setOnClickListener {
                onItemClick(assignment)
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<DeliveryAssignment>() {
        override fun areItemsTheSame(oldItem: DeliveryAssignment, newItem: DeliveryAssignment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DeliveryAssignment, newItem: DeliveryAssignment): Boolean {
            return oldItem == newItem
        }
    }
}
