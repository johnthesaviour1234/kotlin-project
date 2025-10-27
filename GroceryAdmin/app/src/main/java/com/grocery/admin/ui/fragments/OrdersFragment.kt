package com.grocery.admin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.OrderDto
import com.grocery.admin.databinding.FragmentOrdersBinding
import com.grocery.admin.ui.adapters.OrdersAdapter
import com.grocery.admin.ui.dialogs.UpdateOrderStatusDialog
import com.grocery.admin.ui.dialogs.AssignDriverDialog
import com.grocery.admin.ui.viewmodels.OrdersViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for managing orders in the admin panel.
 * Displays a list of orders with filtering, search, and management capabilities.
 */
@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load orders
        viewModel.loadOrders()
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(
            onOrderClick = { order: OrderDto ->
                // TODO: Navigate to OrderDetailFragment
                showOrderDetails(order)
            },
            onUpdateStatusClick = { order: OrderDto ->
                showUpdateStatusDialog(order)
            },
            onAssignDriverClick = { order: OrderDto ->
                showAssignDriverDialog(order)
            }
        )
        
        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
        }
    }

    private fun setupSearchAndFilters() {
        // Search functionality
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchOrders(text?.toString() ?: "")
        }

        // Status filter chips
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding.chipAll.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            when (checkedIds.first()) {
                R.id.chipAll -> viewModel.filterByStatus(null)
                R.id.chipPending -> viewModel.filterByStatus("pending")
                R.id.chipConfirmed -> viewModel.filterByStatus("confirmed")
                R.id.chipPreparing -> viewModel.filterByStatus("preparing")
                R.id.chipReady -> viewModel.filterByStatus("ready")
                R.id.chipOutForDelivery -> viewModel.filterByStatus("out_for_delivery")
                R.id.chipDelivered -> viewModel.filterByStatus("delivered")
                R.id.chipCancelled -> viewModel.filterByStatus("cancelled")
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshOrders()
        }
    }

    private fun observeViewModel() {
        // Observe orders list
        viewModel.orders.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    
                    val orders = resource.data ?: emptyList()
                    
                    if (orders.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvOrders.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvOrders.visibility = View.VISIBLE
                        ordersAdapter.submitList(orders)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    showError(resource.message ?: "Failed to load orders")
                }
                null -> {
                    // Initial state
                }
            }
        }

        // Observe update status result
        viewModel.updateStatusResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Order status updated successfully")
                    viewModel.resetUpdateStatusResult()
                    viewModel.refreshOrders()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Failed to update order status")
                    viewModel.resetUpdateStatusResult()
                }
                null -> {
                    // Initial state
                }
            }
        }

        // Observe assign driver result
        viewModel.assignDriverResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Order assigned to driver successfully")
                    viewModel.resetAssignDriverResult()
                    viewModel.refreshOrders()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Failed to assign driver")
                    viewModel.resetAssignDriverResult()
                }
                null -> {
                    // Initial state
                }
            }
        }
    }

    private fun showOrderDetails(order: OrderDto) {
        // Navigate to OrderDetailFragment using Navigation Component
        val bundle = Bundle().apply {
            putString("orderId", order.id)
        }
        findNavController().navigate(R.id.action_ordersFragment_to_orderDetailFragment, bundle)
    }

    private fun showUpdateStatusDialog(order: OrderDto) {
        val dialog = UpdateOrderStatusDialog.newInstance(
            orderId = order.id,
            currentStatus = order.status
        )
        dialog.show(childFragmentManager, "UpdateOrderStatusDialog")
    }

    private fun showAssignDriverDialog(order: OrderDto) {
        val dialog = AssignDriverDialog.newInstance(
            orderId = order.id
        )
        dialog.show(childFragmentManager, "AssignDriverDialog")
    }

    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setAction("Retry") {
            viewModel.loadOrders()
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