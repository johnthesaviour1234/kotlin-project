package com.grocery.admin.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.ProductDto
import com.grocery.admin.databinding.ItemProductBinding

/**
 * RecyclerView adapter for displaying a list of products in the admin panel.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class ProductsAdapter(
    private val onEditClick: (ProductDto) -> Unit,
    private val onDeleteClick: (ProductDto) -> Unit
) : ListAdapter<ProductDto, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for product items.
     */
    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onEditClick: (ProductDto) -> Unit,
        private val onDeleteClick: (ProductDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductDto) {
            // Set product name
            binding.tvProductName.text = product.name

            // Set product category
            binding.tvProductCategory.text = product.category?.name ?: "No category"

            // Set product price with currency symbol
            binding.tvProductPrice.text = itemView.context.getString(
                R.string.price_format,
                product.price
            )

            // Set stock level and badge color
            val stockQuantity = product.inventory?.quantity ?: 0
            binding.tvStockLevel.text = itemView.context.getString(
                R.string.stock_format,
                stockQuantity
            )

            // Update stock badge color based on stock level
            val stockThreshold = product.inventory?.reorderLevel ?: 10
            when {
                stockQuantity == 0 -> {
                    binding.cardStockBadge.setCardBackgroundColor(
                        itemView.context.getColor(R.color.error_container)
                    )
                    binding.tvStockLevel.setTextColor(
                        itemView.context.getColor(R.color.on_error_container)
                    )
                }
                stockQuantity <= stockThreshold -> {
                    binding.cardStockBadge.setCardBackgroundColor(
                        itemView.context.getColor(R.color.warning_container)
                    )
                    binding.tvStockLevel.setTextColor(
                        itemView.context.getColor(R.color.on_warning_container)
                    )
                }
                else -> {
                    binding.cardStockBadge.setCardBackgroundColor(
                        itemView.context.getColor(R.color.success_container)
                    )
                    binding.tvStockLevel.setTextColor(
                        itemView.context.getColor(R.color.on_success_container)
                    )
                }
            }

            // Show/hide featured badge
            binding.cardFeaturedBadge.visibility = if (product.featured) View.VISIBLE else View.GONE

            // Show/hide inactive badge
            binding.cardStatusBadge.visibility = if (!product.isActive) View.VISIBLE else View.GONE

            // Load product image using Glide
            Glide.with(binding.root.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_placeholder_product)
                .error(R.drawable.ic_placeholder_product)
                .centerCrop()
                .into(binding.ivProductImage)

            // Set click listeners
            binding.btnEdit.setOnClickListener {
                onEditClick(product)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(product)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    private class ProductDiffCallback : DiffUtil.ItemCallback<ProductDto>() {
        override fun areItemsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductDto, newItem: ProductDto): Boolean {
            return oldItem == newItem
        }
    }
}
