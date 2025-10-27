package com.grocery.admin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.databinding.FragmentInventoryBinding
import com.grocery.admin.ui.adapters.InventoryAdapter
import com.grocery.admin.ui.dialogs.UpdateStockDialog
import com.grocery.admin.ui.viewmodels.InventoryViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for inventory management.
 * Shows products with their stock levels and provides stock update functionality.
 */
@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var inventoryAdapter: InventoryAdapter
    private var isFilteringLowStock = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter { inventoryItem ->
            // Show update stock dialog
            val dialog = UpdateStockDialog(
                inventoryItem = inventoryItem,
                onUpdateStock = { productId, stock, adjustmentType ->
                    viewModel.updateStock(productId, stock, adjustmentType)
                }
            )
            dialog.show(parentFragmentManager, "UpdateStockDialog")
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
        }
    }
    
    private fun setupUI() {
        binding.tvTitle.text = "Inventory Management"
        binding.tvSubtitle.text = "Product stock levels and inventory tracking"
        
        // Setup swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshInventory()
        }
        
        // Setup filter chip
        binding.chipLowStock.setOnCheckedChangeListener { _, isChecked ->
            isFilteringLowStock = isChecked
            if (isChecked) {
                viewModel.filterLowStock()
            } else {
                viewModel.clearFilter()
            }
        }
    }

    private fun observeViewModel() {
        // Observe inventory data
        viewModel.inventory.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.emptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    
                    val data = resource.data
                    if (data != null) {
                        val items = data.items
                        
                        if (items.isEmpty()) {
                            binding.recyclerView.visibility = View.GONE
                            binding.layoutStats.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        } else {
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.layoutStats.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            
                            // Update RecyclerView
                            inventoryAdapter.submitList(items)
                            
                            // Update statistics
                            updateInventoryStats(items, data.lowStockCount)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.emptyState.visibility = View.VISIBLE
                    
                    showError(resource.message ?: "Failed to load inventory")
                }
            }
        }
        
        // Observe stock update result
        viewModel.updateResult.observe(viewLifecycleOwner) { resource ->
            resource?.let {
                when (it) {
                    is Resource.Loading -> {
                        // Show progress
                    }
                    is Resource.Success -> {
                        showSuccess("Stock updated successfully")
                        // Clear the result after showing
                        viewModel.clearUpdateResult()
                    }
                    is Resource.Error -> {
                        showError(it.message ?: "Failed to update stock")
                        // Clear the result after showing
                        viewModel.clearUpdateResult()
                    }
                }
            }
        }
    }

    private fun updateInventoryStats(
        items: List<com.grocery.admin.data.remote.dto.InventoryItemDto>,
        lowStockCount: Int
    ) {
        val totalProducts = items.size
        
        // Calculate out of stock items
        val outOfStockCount = items.count { it.stock == 0 }
        
        // Update UI
        binding.tvTotalProducts.text = totalProducts.toString()
        binding.tvLowStock.text = lowStockCount.toString()
        binding.tvOutOfStock.text = outOfStockCount.toString()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}