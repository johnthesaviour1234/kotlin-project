package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.grocery.customer.databinding.FragmentHomeBinding
import com.grocery.customer.ui.adapters.ProductsAdapter
import com.grocery.customer.ui.viewmodels.HomeViewModel
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
        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupRefreshListener()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { product ->
            // TODO: Navigate to product details
            // findNavController().navigate(
            //     HomeFragmentDirections.actionHomeToProductDetail(product.id)
            // )
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
        // TODO: Observe ViewModel data when implemented
        /*
        viewModel.featuredProducts.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    productsAdapter.submitList(resource.data)
                    
                    // Show empty state if no products
                    binding.textViewEmptyState.visibility = 
                        if (resource.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    // TODO: Show error state
                }
            }
        }
        */
    }

    private fun loadData() {
        // TODO: Load featured products from ViewModel
        // viewModel.loadFeaturedProducts()
        
        // For now, hide loading state
        binding.swipeRefresh.isRefreshing = false
        binding.progressBar.visibility = View.GONE
        binding.textViewEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}