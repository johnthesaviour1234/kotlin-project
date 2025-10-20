package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.CartItem

/**
 * Adapter for displaying cart items in the checkout screen.
 * Shows product details, quantity, and total price for each item.
 */
class CheckoutItemAdapter : ListAdapter<CartItem, CheckoutItemAdapter.CheckoutItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkout, parent, false)
        return CheckoutItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckoutItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CheckoutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewProduct: ImageView = itemView.findViewById(R.id.imageViewProduct)
        private val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        private val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        private val textViewUnitPrice: TextView = itemView.findViewById(R.id.textViewUnitPrice)
        private val textViewTotalPrice: TextView = itemView.findViewById(R.id.textViewTotalPrice)

        fun bind(cartItem: CartItem) {
            textViewProductName.text = cartItem.product.name
            textViewQuantity.text = itemView.context.getString(R.string.quantity_format, cartItem.quantity)
            textViewUnitPrice.text = itemView.context.getString(R.string.price_format, cartItem.price)
            textViewTotalPrice.text = itemView.context.getString(R.string.price_format, cartItem.totalPrice)

            // Load product image
            if (!cartItem.product.image_url.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(cartItem.product.image_url)
                    .placeholder(R.drawable.ic_placeholder_product)
                    .error(R.drawable.ic_placeholder_product)
                    .into(imageViewProduct)
            } else {
                imageViewProduct.setImageResource(R.drawable.ic_placeholder_product)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}