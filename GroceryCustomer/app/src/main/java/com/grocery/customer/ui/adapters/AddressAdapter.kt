package com.grocery.customer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.data.remote.dto.UserAddress
import com.grocery.customer.databinding.ItemAddressBinding

/**
 * Adapter for displaying user addresses in a RecyclerView
 */
class AddressAdapter(
    private val onEditClick: (UserAddress) -> Unit,
    private val onDeleteClick: (UserAddress) -> Unit,
    private val onAddressClick: (UserAddress) -> Unit
) : ListAdapter<UserAddress, AddressAdapter.AddressViewHolder>(AddressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AddressViewHolder(
        private val binding: ItemAddressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: UserAddress) {
            // Set address name
            binding.textViewAddressName.text = address.name

            // Build full address string
            val fullAddress = buildString {
                append(address.street_address)
                if (!address.apartment.isNullOrBlank()) {
                    append("\n${address.apartment}")
                }
                append("\n${address.city}, ${address.state} ${address.postal_code}")
                if (!address.landmark.isNullOrBlank()) {
                    append("\nNear ${address.landmark}")
                }
            }
            binding.textViewFullAddress.text = fullAddress

            // Set address type
            binding.textViewAddressType.text = address.address_type.uppercase()

            // Show/hide default chip
            binding.chipDefault.visibility = if (address.is_default) View.VISIBLE else View.GONE

            // Set click listeners
            binding.root.setOnClickListener {
                onAddressClick(address)
            }

            binding.buttonEditAddress.setOnClickListener {
                onEditClick(address)
            }

            binding.buttonDeleteAddress.setOnClickListener {
                onDeleteClick(address)
            }
        }
    }

    class AddressDiffCallback : DiffUtil.ItemCallback<UserAddress>() {
        override fun areItemsTheSame(oldItem: UserAddress, newItem: UserAddress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserAddress, newItem: UserAddress): Boolean {
            return oldItem == newItem
        }
    }
}