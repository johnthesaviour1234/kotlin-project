package com.grocery.admin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.databinding.FragmentProductsBinding
import com.grocery.admin.ui.adapters.ProductsAdapter
import com.grocery.admin.ui.viewmodels.ProductsViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for managing products in the admin panel.
 * Displays a list of products with search, filter, and CRUD operations.
 */
@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductsViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupFab()
        observeViewModel()
        
        // Load products
        viewModel.loadProducts()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(
            onEditClick = { product ->
                // TODO: Navigate to AddEditProductFragment with product
                Snackbar.make(
                    binding.root,
                    "Edit Product: ${product.name} - To be implemented",
                    Snackbar.LENGTH_SHORT
                ).show()
            },
            onDeleteClick = { product ->
                showDeleteConfirmationDialog(product.id, product.name)
            }
        )
        
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
        }
    }

    private fun setupSearchAndFilters() {
        // Search functionality
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchProducts(text?.toString() ?: "")
        }

        // Filter chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding.chipAll.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            when (checkedIds.first()) {
                R.id.chipAll -> viewModel.filterByCategory(null)
                R.id.chipActive -> {}
                R.id.chipInactive -> {}
                R.id.chipFeatured -> {}
                R.id.chipLowStock -> {}
            }
        }
    }

    private fun setupFab() {
        binding.fabAddProduct.setOnClickListener {
            // TODO: Navigate to AddEditProductFragment without product
            Snackbar.make(
                binding.root,
                "Add Product - To be implemented",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val products = resource.data ?: emptyList()
                    
                    if (products.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvProducts.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvProducts.visibility = View.VISIBLE
                        productsAdapter.submitList(products)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Failed to load products")
                }
                null -> {
                    // Initial state
                }
            }
        }

        viewModel.deleteProductResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Product deleted successfully")
                    viewModel.resetDeleteProductResult()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Failed to delete product")
                    viewModel.resetDeleteProductResult()
                }
                null -> {
                    // Initial state
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(productId: String, productName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_product))
            .setMessage(getString(R.string.delete_product_confirm, productName))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteProduct(productId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.retry)) {
            viewModel.loadProducts()
        }.show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
