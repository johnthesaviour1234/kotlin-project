package com.grocery.delivery.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.delivery.R
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.databinding.ItemAvailableOrderBinding
import java.text.NumberFormat
import java.util.*

/**
 * Adapter for displaying available orders in RecyclerView
 */
class AvailableOrdersAdapter(
    private val onOrderClick: (DeliveryAssignment) -> Unit
) : ListAdapter<DeliveryAssignment, AvailableOrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemAvailableOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding, onOrderClick)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class OrderViewHolder(
        private val binding: ItemAvailableOrderBinding,
        private val onOrderClick: (DeliveryAssignment) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(assignment: DeliveryAssignment) {
            val order = assignment.orders
            
            // Order number
            binding.textViewOrderNumber.text = order?.orderNumber ?: "N/A"
            
            // Status with color coding
            when (assignment.status) {
                "pending" -> {
                    binding.textViewStatus.text = "Pending"
                    binding.textViewStatus.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.status_pending)
                    )
                }
                "accepted" -> {
                    binding.textViewStatus.text = "Accepted"
                    binding.textViewStatus.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.status_accepted)
                    )
                }
                "in_transit" -> {
                    binding.textViewStatus.text = "On the Way"
                    binding.textViewStatus.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.status_in_transit)
                    )
                }
                "arrived" -> {
                    binding.textViewStatus.text = "Arrived"
                    binding.textViewStatus.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.status_arrived)
                    )
                }
                else -> {
                    binding.textViewStatus.text = assignment.status.replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                }
            }
            
            // Customer name
            binding.textViewCustomerName.text = order?.getCustomerName() ?: "Customer"
            
            // Delivery address
            binding.textViewAddress.text = order?.getFormattedAddress() ?: "Address not available"
            
            // Total amount with currency format
            val amount = order?.totalAmount ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.textViewTotalAmount.text = formatter.format(amount)
            
            // Estimated delivery time
            val estimatedMinutes = assignment.estimatedDeliveryMinutes
            if (estimatedMinutes != null && estimatedMinutes > 0) {
                binding.textViewEstimatedTime.text = "Est. $estimatedMinutes mins"
            } else {
                binding.textViewEstimatedTime.text = "Est. 30 mins"
            }
            
            // Click listener
            binding.root.setOnClickListener {
                onOrderClick(assignment)
            }
        }
    }
    
    class OrderDiffCallback : DiffUtil.ItemCallback<DeliveryAssignment>() {
        override fun areItemsTheSame(
            oldItem: DeliveryAssignment,
            newItem: DeliveryAssignment
        ): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(
            oldItem: DeliveryAssignment,
            newItem: DeliveryAssignment
        ): Boolean {
            return oldItem == newItem
        }
    }
}
