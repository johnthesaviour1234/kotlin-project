package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.grocery.customer.databinding.FragmentProductListBinding
import com.grocery.customer.ui.adapters.ProductsAdapter
import com.grocery.customer.ui.viewmodels.ProductListViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Product List Fragment - Displays products in a specific category
 */
@AndroidEntryPoint
class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductListViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter

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

    private fun setupObservers() {
        viewModel.products.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.textViewEmptyProducts.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    productsAdapter.submitList(resource.data)
                    
                    // Show empty state if no products
                    binding.textViewEmptyProducts.visibility = 
                        if (resource.data?.isEmpty() == true) View.VISIBLE else View.GONE
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
