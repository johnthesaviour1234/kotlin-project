package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.CartItem
import com.grocery.customer.databinding.ItemCartBinding

/**
 * RecyclerView adapter for displaying cart items
 */
class CartAdapter(
    private val onQuantityChanged: (cartItemId: String, quantity: Int) -> Unit,
    private val onRemoveItem: (cartItemId: String) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartItemViewHolder>(CartItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartItemViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.apply {
                // Product details
                textViewProductName.text = cartItem.product.name
                textViewProductPrice.text = "$${String.format("%.2f", cartItem.price)} each"
                textViewQuantity.text = cartItem.quantity.toString()
                textViewItemTotal.text = "$${String.format("%.2f", cartItem.totalPrice)}"

                // Load product image
                Glide.with(imageViewProduct.context)
                    .load(cartItem.product.image_url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(imageViewProduct)

                // Quantity controls
                buttonDecreaseQuantity.setOnClickListener {
                    val newQuantity = cartItem.quantity - 1
                    if (newQuantity > 0) {
                        onQuantityChanged(cartItem.id, newQuantity)
                    } else {
                        onRemoveItem(cartItem.id)
                    }
                }

                buttonIncreaseQuantity.setOnClickListener {
                    val newQuantity = cartItem.quantity + 1
                    onQuantityChanged(cartItem.id, newQuantity)
                }

                // Remove button
                buttonRemove.setOnClickListener {
                    onRemoveItem(cartItem.id)
                }

                // Enable/disable quantity controls
                buttonDecreaseQuantity.isEnabled = cartItem.quantity > 1
            }
        }
    }

    private class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}