package com.grocery.admin.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.InventoryItemDto
import com.grocery.admin.databinding.DialogUpdateStockBinding

class UpdateStockDialog(
    private val inventoryItem: InventoryItemDto,
    private val onUpdateStock: (productId: String, stock: Int, adjustmentType: String) -> Unit
) : DialogFragment() {
    
    private var _binding: DialogUpdateStockBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUpdateStockBinding.inflate(layoutInflater)
        
        setupUI()
        setupListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Update Stock")
            .setPositiveButton("Update") { _, _ ->
                updateStock()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupUI() {
        binding.apply {
            // Product info
            tvProductName.text = inventoryItem.products?.name ?: "Unknown Product"
            tvCurrentStock.text = "Current Stock: ${inventoryItem.stock} units"
            
            // Load product image
            Glide.with(requireContext())
                .load(inventoryItem.products?.imageUrl)
                .placeholder(R.drawable.ic_placeholder_product)
                .error(R.drawable.ic_placeholder_product)
                .centerCrop()
                .into(ivProductImage)
            
            // Set default adjustment type to "set"
            rgAdjustmentType.check(R.id.rbSet)
            
            // Show hint based on selected adjustment type
            updateHintText()
        }
    }
    
    private fun setupListeners() {
        binding.rgAdjustmentType.setOnCheckedChangeListener { _, _ ->
            updateHintText()
        }
    }
    
    private fun updateHintText() {
        val selectedType = getSelectedAdjustmentType()
        val hintText = when (selectedType) {
            "set" -> "Enter new stock quantity"
            "add" -> "Enter quantity to add"
            "subtract" -> "Enter quantity to subtract"
            else -> "Enter quantity"
        }
        binding.etQuantity.hint = hintText
    }
    
    private fun getSelectedAdjustmentType(): String {
        return when (binding.rgAdjustmentType.checkedRadioButtonId) {
            R.id.rbSet -> "set"
            R.id.rbAdd -> "add"
            R.id.rbSubtract -> "subtract"
            else -> "set"
        }
    }
    
    private fun updateStock() {
        val quantityText = binding.etQuantity.text.toString()
        
        if (quantityText.isEmpty()) {
            binding.tilQuantity.error = "Quantity is required"
            return
        }
        
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity < 0) {
            binding.tilQuantity.error = "Please enter a valid quantity"
            return
        }
        
        val adjustmentType = getSelectedAdjustmentType()
        onUpdateStock(inventoryItem.productId, quantity, adjustmentType)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
