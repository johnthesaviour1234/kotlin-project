package com.grocery.admin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.InventoryItemDto
import com.grocery.admin.databinding.ItemInventoryBinding

class InventoryAdapter(
    private val onUpdateStockClick: (InventoryItemDto) -> Unit
) : ListAdapter<InventoryItemDto, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryViewHolder(binding, onUpdateStockClick)
    }
    
    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class InventoryViewHolder(
        private val binding: ItemInventoryBinding,
        private val onUpdateStockClick: (InventoryItemDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: InventoryItemDto) {
            binding.apply {
                // Product name
                tvProductName.text = item.products?.name ?: "Unknown Product"
                
                // Product price
                tvPrice.text = "₹${String.format("%.2f", item.products?.price ?: 0.0)}"
                
                // Stock level
                tvStockLevel.text = "${item.stock} units"
                
                // Stock status color and text
                val context = root.context
                when {
                    item.stock == 0 -> {
                        tvStockStatus.text = "Out of Stock"
                        tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
                        cardView.strokeColor = ContextCompat.getColor(context, R.color.error)
                        cardView.strokeWidth = 2
                    }
                    item.stock < 10 -> {
                        tvStockStatus.text = "Low Stock"
                        tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                        cardView.strokeColor = ContextCompat.getColor(context, R.color.warning)
                        cardView.strokeWidth = 2
                    }
                    else -> {
                        tvStockStatus.text = "In Stock"
                        tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                        cardView.strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
                        cardView.strokeWidth = 0
                    }
                }
                
                // Product image
                Glide.with(root.context)
                    .load(item.products?.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_product)
                    .error(R.drawable.ic_placeholder_product)
                    .centerCrop()
                    .into(ivProductImage)
                
                // Active status
                val statusText = if (item.products?.isActive == true) "Active" else "Inactive"
                val statusColor = if (item.products?.isActive == true) {
                    ContextCompat.getColor(context, R.color.success)
                } else {
                    ContextCompat.getColor(context, R.color.text_secondary)
                }
                tvActiveStatus.text = statusText
                tvActiveStatus.setTextColor(statusColor)
                
                // Update stock button
                btnUpdateStock.setOnClickListener {
                    onUpdateStockClick(item)
                }
            }
        }
    }
    
    class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryItemDto>() {
        override fun areItemsTheSame(oldItem: InventoryItemDto, newItem: InventoryItemDto): Boolean {
            return oldItem.productId == newItem.productId
        }
        
        override fun areContentsTheSame(oldItem: InventoryItemDto, newItem: InventoryItemDto): Boolean {
            return oldItem == newItem
        }
    }
}
