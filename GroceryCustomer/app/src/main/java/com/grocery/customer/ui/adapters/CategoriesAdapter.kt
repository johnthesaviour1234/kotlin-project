package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.data.remote.dto.ProductCategory
import com.grocery.customer.databinding.ItemCategoryBinding

/**
 * RecyclerView adapter for displaying product categories
 */
class CategoriesAdapter(
    private val onCategoryClick: (ProductCategory) -> Unit
) : ListAdapter<ProductCategory, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val onCategoryClick: (ProductCategory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ProductCategory) {
            binding.apply {
                textViewCategoryName.text = category.name
                textViewCategoryDescription.text = category.description ?: ""
                
                // TODO: Load category image when available
                // Glide.with(imageViewCategory.context)
                //     .load(category.imageUrl)
                //     .placeholder(R.drawable.placeholder_category)
                //     .error(R.drawable.error_category)
                //     .into(imageViewCategory)

                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<ProductCategory>() {
        override fun areItemsTheSame(oldItem: ProductCategory, newItem: ProductCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductCategory, newItem: ProductCategory): Boolean {
            return oldItem == newItem
        }
    }
}