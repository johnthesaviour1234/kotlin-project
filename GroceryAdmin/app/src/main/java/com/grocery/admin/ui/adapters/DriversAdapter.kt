package com.grocery.admin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.DeliveryPersonnelDto
import com.grocery.admin.databinding.ItemDriverBinding

/**
 * Adapter for displaying delivery drivers for assignment
 */
class DriversAdapter(
    private val onDriverSelected: (DeliveryPersonnelDto) -> Unit
) : ListAdapter<DeliveryPersonnelDto, DriversAdapter.DriverViewHolder>(DriverDiffCallback()) {

    private var selectedDriverId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val binding = ItemDriverBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DriverViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DriverViewHolder(
        private val binding: ItemDriverBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(driver: DeliveryPersonnelDto) {
            // Driver basic info
            binding.tvDriverName.text = driver.fullName
            binding.tvDriverEmail.text = driver.email
            binding.tvDriverPhone.text = driver.phone ?: "No phone"
            
            // Rating - Not available in API, hide or show placeholder
            binding.tvRating.text = "New Driver"
            
            // Location/ETA - Not available in API
            binding.tvLocation.text = "Location unknown"
            
            // Availability status
            if (driver.isActive) {
                binding.tvAvailabilityStatus.text = "Available"
                binding.tvAvailabilityStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_ready)
                )
                binding.availabilityIndicator.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_ready)
                )
            } else {
                binding.tvAvailabilityStatus.text = "Busy"
                binding.tvAvailabilityStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_cancelled)
                )
                binding.availabilityIndicator.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.status_cancelled)
                )
            }

            // Selection state
            val isSelected = selectedDriverId == driver.id
            if (isSelected) {
                binding.cardDriver.strokeColor = ContextCompat.getColor(binding.root.context, R.color.primary)
                binding.cardDriver.strokeWidth = 4
                binding.cardDriver.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface_variant)
                )
            } else {
                binding.cardDriver.strokeColor = ContextCompat.getColor(binding.root.context, R.color.divider_light)
                binding.cardDriver.strokeWidth = 1
                binding.cardDriver.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface)
                )
            }

            // Click handling
            binding.root.setOnClickListener {
                if (driver.isActive) {
                    val previousSelectedId = selectedDriverId
                    selectedDriverId = driver.id
                    
                    // Update UI for previously selected item
                    if (previousSelectedId != null) {
                        notifyItemChanged(currentList.indexOfFirst { it.id == previousSelectedId })
                    }
                    
                    // Update UI for newly selected item
                    notifyItemChanged(currentList.indexOfFirst { it.id == driver.id })
                    
                    // Notify parent
                    onDriverSelected(driver)
                }
            }

            // Disable visual feedback for unavailable drivers
            binding.root.alpha = if (driver.isActive) 1.0f else 0.6f
            binding.root.isEnabled = driver.isActive
        }
    }

    private class DriverDiffCallback : DiffUtil.ItemCallback<DeliveryPersonnelDto>() {
        override fun areItemsTheSame(
            oldItem: DeliveryPersonnelDto,
            newItem: DeliveryPersonnelDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DeliveryPersonnelDto,
            newItem: DeliveryPersonnelDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
