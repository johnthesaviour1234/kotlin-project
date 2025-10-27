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
import com.grocery.admin.ui.viewmodels.ProductsViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for inventory management.
 * Shows products with their stock levels and provides basic management.
 */
@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductsViewModel by viewModels()

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
        
        setupUI()
        observeViewModel()
        
        // Load inventory data
        viewModel.loadProducts()
    }

    private fun setupUI() {
        // Show coming soon message for now
        binding.tvTitle.text = "Inventory Management"
        binding.tvSubtitle.text = "Product stock levels and inventory tracking"
        
        // Setup swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshProducts()
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    // Don't hide content during refresh, only on initial load
                    if (binding.layoutContent.visibility != View.VISIBLE) {
                        binding.layoutContent.visibility = View.GONE
                    }
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    
                    val products = resource.data ?: emptyList()
                    
                    // Always show content after successful load
                    binding.layoutContent.visibility = View.VISIBLE
                    
                    // Update inventory statistics
                    updateInventoryStats(products)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    
                    // Show content with error state
                    binding.layoutContent.visibility = View.VISIBLE
                    binding.tvMessage.text = "Error loading inventory: ${resource.message}\n\nPull down to try again"
                    
                    showError(resource.message ?: "Failed to load inventory")
                }
                null -> {
                    // Initial state - do nothing
                }
            }
        }
    }

    private fun updateInventoryStats(products: List<com.grocery.admin.data.remote.dto.ProductDto>) {
        val totalProducts = products.size
        
        // Calculate low stock items (stock < 10)
        val lowStockCount = products.count { product ->
            (product.inventory?.quantity ?: 0) > 0 && (product.inventory?.quantity ?: 0) < 10
        }
        
        // Calculate out of stock items
        val outOfStockCount = products.count { product ->
            (product.inventory?.quantity ?: 0) == 0
        }
        
        // Update UI
        binding.tvTotalProducts.text = totalProducts.toString()
        binding.tvLowStock.text = lowStockCount.toString()
        binding.tvOutOfStock.text = outOfStockCount.toString()
        
        // Show success message with details
        val statusMessage = buildString {
            append("Inventory loaded successfully!\n")
            append("Total Products: $totalProducts\n")
            if (lowStockCount > 0) {
                append("⚠️ $lowStockCount item(s) running low\n")
            }
            if (outOfStockCount > 0) {
                append("❌ $outOfStockCount item(s) out of stock\n")
            }
            if (lowStockCount == 0 && outOfStockCount == 0) {
                append("✅ All items are well stocked")
            }
        }
        binding.tvMessage.text = statusMessage
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}