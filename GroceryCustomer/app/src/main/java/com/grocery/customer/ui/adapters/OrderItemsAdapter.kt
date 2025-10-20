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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.OrderItemDTO

/**
 * Adapter for displaying order items in order detail view.
 * Shows product image, name, quantity, price details.
 */
class OrderItemsAdapter : ListAdapter<OrderItemDTO, OrderItemsAdapter.OrderItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_item, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderItemViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val imageViewProduct: ImageView = itemView.findViewById(R.id.imageViewProduct)
        private val textViewProductName: TextView = itemView.findViewById(R.id.textViewProductName)
        private val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        private val textViewUnitPrice: TextView = itemView.findViewById(R.id.textViewUnitPrice)
        private val textViewTotalPrice: TextView = itemView.findViewById(R.id.textViewTotalPrice)

        fun bind(item: OrderItemDTO) {
            // Product name
            textViewProductName.text = item.product_name
            
            // Quantity
            textViewQuantity.text = "Qty: ${item.quantity}"
            
            // Unit price
            textViewUnitPrice.text = itemView.context.getString(R.string.price_format, item.unit_price)
            
            // Total price
            textViewTotalPrice.text = itemView.context.getString(R.string.price_format, item.total_price)
            
            // Load product image
            Glide.with(itemView.context)
                .load(item.product_image_url)
                .placeholder(R.drawable.ic_placeholder_product)
                .error(R.drawable.ic_placeholder_product)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageViewProduct)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderItemDTO>() {
        override fun areItemsTheSame(oldItem: OrderItemDTO, newItem: OrderItemDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderItemDTO, newItem: OrderItemDTO): Boolean {
            return oldItem == newItem
        }
    }
}