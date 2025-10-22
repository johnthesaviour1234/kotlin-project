package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.grocery.customer.R
import com.grocery.customer.databinding.FragmentProductListBinding
import com.grocery.customer.ui.adapters.ProductsAdapter
import com.grocery.customer.ui.viewmodels.ProductListViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

/**
 * Product List Fragment - Displays products in a specific category
 */
@AndroidEntryPoint
class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductListViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter
    
    private var currentSortOption = SortOption.DEFAULT
    
    enum class SortOption(val displayName: String) {
        DEFAULT("Default"),
        PRICE_LOW_HIGH("Price: Low to High"),
        PRICE_HIGH_LOW("Price: High to Low"),
        NAME_A_Z("Name: A-Z"),
        NAME_Z_A("Name: Z-A")
    }
    
    companion object {
        private const val TAG = "ProductListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupRefreshListener()
        setupToolbar()
        setupSearch()
        setupSortButton()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { product ->
            // Navigate to product detail
            val action = ProductListFragmentDirections.actionProductListToDetail(
                productId = product.id
            )
            findNavController().navigate(action)
        }

        binding.recyclerViewProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productsAdapter
        }
    }

    private fun setupRefreshListener() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupToolbar() {
        val categoryName = arguments?.getString("categoryName") ?: "Products"
        binding.textViewCategoryTitle.text = categoryName
    }
    
    private fun setupSearch() {
        // Setup search functionality
        binding.editTextSearch.doAfterTextChanged { text ->
            val query = text.toString().trim()
            if (query.length >= 2 || query.isEmpty()) {
                Log.d(TAG, "Search query: $query")
                performSearch(query)
            }
        }
        
        // Handle search action
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearch.text.toString().trim()
                Log.d(TAG, "Search action triggered with query: $query")
                performSearch(query)
                true
            } else {
                false
            }
        }
    }
    
    private fun setupSortButton() {
        binding.buttonSort.setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // If search is cleared, reload category products or all products
            loadData()
        } else {
            viewModel.searchProducts(query)
        }
    }
    
    private fun showSortDialog() {
        val sortOptions = SortOption.values().map { it.displayName }.toTypedArray()
        val currentIndex = currentSortOption.ordinal
        
        AlertDialog.Builder(requireContext())
            .setTitle("Sort by")
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                currentSortOption = SortOption.values()[which]
                applySorting()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun applySorting() {
        val currentProducts = (viewModel.products.value as? Resource.Success)?.data
        if (currentProducts != null) {
            val sortedProducts = when (currentSortOption) {
                SortOption.DEFAULT -> currentProducts
                SortOption.PRICE_LOW_HIGH -> currentProducts.sortedBy { it.price }
                SortOption.PRICE_HIGH_LOW -> currentProducts.sortedByDescending { it.price }
                SortOption.NAME_A_Z -> currentProducts.sortedBy { it.name }
                SortOption.NAME_Z_A -> currentProducts.sortedByDescending { it.name }
            }
            productsAdapter.submitList(sortedProducts)
            Log.d(TAG, "Applied sorting: ${currentSortOption.displayName}")
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Only show progress bar if not already refreshing via swipe
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.textViewEmptyProducts.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    
                    // Apply current sorting to the products
                    val sortedProducts = when (currentSortOption) {
                        SortOption.DEFAULT -> resource.data
                        SortOption.PRICE_LOW_HIGH -> resource.data?.sortedBy { it.price }
                        SortOption.PRICE_HIGH_LOW -> resource.data?.sortedByDescending { it.price }
                        SortOption.NAME_A_Z -> resource.data?.sortedBy { it.name }
                        SortOption.NAME_Z_A -> resource.data?.sortedByDescending { it.name }
                    }
                    
                    productsAdapter.submitList(sortedProducts)
                    
                    // Show empty state if no products
                    binding.textViewEmptyProducts.visibility = 
                        if (resource.data?.isEmpty() == true) View.VISIBLE else View.GONE
                        
                    Log.d(TAG, "Loaded ${resource.data?.size ?: 0} products with sorting: ${currentSortOption.displayName}")
                }
                is Resource.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    binding.textViewEmptyProducts.visibility = View.VISIBLE
                    // TODO: Show error message to user
                }
            }
        }
    }

    private fun loadData() {
        val categoryId = arguments?.getString("categoryId")
        if (categoryId != null) {
            viewModel.loadProductsByCategory(categoryId)
        } else {
            viewModel.loadAllProducts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
