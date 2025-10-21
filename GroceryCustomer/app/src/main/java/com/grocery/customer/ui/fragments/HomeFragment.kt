package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.grocery.customer.R
import com.grocery.customer.databinding.FragmentHomeBinding
import com.grocery.customer.ui.adapters.ProductsAdapter
import com.grocery.customer.ui.adapters.CategoriesAdapter
import com.grocery.customer.ui.viewmodels.HomeViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Home Fragment - Displays featured products and categories
 * This is the main landing screen showing personalized content
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var categoriesAdapter: CategoriesAdapter

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupRefreshListener()
    }

    private fun setupRecyclerView() {
        // Setup Categories RecyclerView
        categoriesAdapter = CategoriesAdapter { category ->
            Log.d(TAG, "Category clicked: ${category.name} (ID: ${category.id})")
            try {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToProductsByCategory(
                        categoryId = category.id,
                        categoryName = category.name
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}", e)
            }
        }

        binding.recyclerViewCategories.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = categoriesAdapter
        }

        // Setup Products RecyclerView
        productsAdapter = ProductsAdapter { product ->
            Log.d(TAG, "Product clicked: ${product.name} (ID: ${product.id})")
            try {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToProductDetail(product.id)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}", e)
            }
        }

        binding.recyclerViewFeaturedProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productsAdapter
        }
    }

    private fun setupRefreshListener() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        // Observe Categories
        viewModel.categories.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG, "Categories resource state: ${resource::class.simpleName}")
            when (resource) {
                is Resource.Loading -> {
                    Log.d(TAG, "Loading categories...")
                    // Categories loading handled by overall progress
                }
                is Resource.Success -> {
                    Log.d(TAG, "Successfully loaded ${resource.data?.size ?: 0} categories")
                    categoriesAdapter.submitList(resource.data)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading categories: ${resource.message}")
                    // Show error but don't fail entire page
                }
            }
        }

        // Observe Featured Products
        viewModel.featuredProducts.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG, "Featured products resource state: ${resource::class.simpleName}")
            when (resource) {
                is Resource.Loading -> {
                    Log.d(TAG, "Loading featured products...")
                    binding.swipeRefresh.isRefreshing = true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.textViewEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    Log.d(TAG, "Successfully loaded ${resource.data?.size ?: 0} featured products")
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    productsAdapter.submitList(resource.data)
                    
                    // Show empty state if no products
                    binding.textViewEmptyState.visibility = 
                        if (resource.data?.isEmpty() == true) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading featured products: ${resource.message}")
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    binding.textViewEmptyState.visibility = View.VISIBLE
                    // TODO: Show error message to user
                }
            }
        }
    }

    private fun loadData() {
        Log.d(TAG, "Loading home screen data...")
        viewModel.loadFeaturedProducts()
        viewModel.loadCategories()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchProducts(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    // Reset to featured products when search is cleared
                    viewModel.loadFeaturedProducts()
                }
                return true
            }
        })
    }
    
    private fun searchProducts(query: String) {
        Log.d(TAG, "Searching for products: $query")
        viewModel.searchProducts(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
