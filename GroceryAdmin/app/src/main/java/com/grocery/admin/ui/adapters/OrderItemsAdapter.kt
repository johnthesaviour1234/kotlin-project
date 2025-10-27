package com.grocery.admin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.OrderItemDto
import com.grocery.admin.databinding.ItemOrderItemBinding
import java.text.NumberFormat
import java.util.*

/**
 * Adapter for displaying individual order items
 */
class OrderItemsAdapter : ListAdapter<OrderItemDto, OrderItemsAdapter.OrderItemViewHolder>(OrderItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val binding = ItemOrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderItemViewHolder(
        private val binding: ItemOrderItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(orderItem: OrderItemDto) {
            // Product details
            binding.tvProductName.text = orderItem.product?.name ?: "Unknown Product"
            binding.tvQuantity.text = "Qty: ${orderItem.quantity}"
            binding.tvUnitPrice.text = formatCurrency(orderItem.price)
            binding.tvTotalPrice.text = formatCurrency(orderItem.price * orderItem.quantity)

            // Product image
            orderItem.product?.imageUrl?.let { imageUrl ->
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_product)
                    .error(R.drawable.ic_placeholder_product)
                    .into(binding.ivProductImage)
            } ?: run {
                binding.ivProductImage.setImageResource(R.drawable.ic_placeholder_product)
            }

            // Product description
            binding.tvProductDescription.text = orderItem.product?.description ?: ""
            
            // Category info if available
            orderItem.product?.category?.let { category ->
                binding.tvCategory.text = category.name
                binding.tvCategory.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvCategory.visibility = android.view.View.GONE
            }
        }

        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 2
            return format.format(amount)
        }
    }

    private class OrderItemDiffCallback : DiffUtil.ItemCallback<OrderItemDto>() {
        override fun areItemsTheSame(oldItem: OrderItemDto, newItem: OrderItemDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderItemDto, newItem: OrderItemDto): Boolean {
            return oldItem == newItem
        }
    }
}