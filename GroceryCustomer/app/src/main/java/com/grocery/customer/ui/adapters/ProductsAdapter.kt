package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.data.remote.dto.Product
import com.grocery.customer.databinding.ItemProductBinding

/**
 * RecyclerView adapter for displaying products in a grid layout
 */
class ProductsAdapter(
    private val onProductClick: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                textViewProductName.text = product.name
                textViewProductPrice.text = "$${product.price}"
                textViewProductDescription.text = product.category_id ?: ""
                
                // TODO: Load image using Glide when image loading is implemented
                // Glide.with(imageViewProduct.context)
                //     .load(product.imageUrl)
                //     .placeholder(R.drawable.placeholder_product)
                //     .error(R.drawable.error_product)
                //     .into(imageViewProduct)

                root.setOnClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}