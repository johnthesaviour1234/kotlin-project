package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.grocery.customer.R
import com.grocery.customer.ui.adapters.OrderHistoryAdapter
import com.grocery.customer.ui.viewmodels.OrderHistoryViewModel
import com.grocery.customer.ui.viewmodels.OrderHistoryUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for displaying user's order history.
 * Shows list of past orders with filtering and pagination support.
 */
@AndroidEntryPoint
class OrderHistoryFragment : Fragment() {

    private val viewModel: OrderHistoryViewModel by viewModels()
    private var isPollingActive = false
    
    // Views
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerViewOrders: RecyclerView
    private lateinit var spinnerStatusFilter: Spinner
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var layoutError: LinearLayout
    private lateinit var textViewErrorMessage: TextView
    private lateinit var buttonRetry: Button
    
    private lateinit var orderHistoryAdapter: OrderHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupStatusFilter()
        setupObservers()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerViewOrders = view.findViewById(R.id.recyclerViewOrders)
        spinnerStatusFilter = view.findViewById(R.id.spinnerStatusFilter)
        layoutEmptyState = view.findViewById(R.id.textViewEmptyState)
        progressBarLoading = view.findViewById(R.id.progressBarLoading)
        layoutError = view.findViewById(R.id.layoutError)
        textViewErrorMessage = view.findViewById(R.id.textViewErrorMessage)
        buttonRetry = view.findViewById(R.id.buttonRetry)
    }

    private fun setupRecyclerView() {
        orderHistoryAdapter = OrderHistoryAdapter { orderId ->
            // Navigate to order details screen
            val action = OrderHistoryFragmentDirections.actionOrderHistoryToOrderDetail(orderId)
            findNavController().navigate(action)
        }
        
        recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderHistoryAdapter
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when close to end
                    if (visibleItemCount + pastVisibleItems >= totalItemCount - 3) {
                        viewModel.loadMoreOrders()
                    }
                }
            })
        }
    }

    private fun setupStatusFilter() {
        val statusOptions = arrayOf("All", "Pending", "Confirmed", "Out for Delivery", "Arrived", "Delivered", "Cancelled")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_address, statusOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item_address)
        
        spinnerStatusFilter.adapter = adapter
        spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = when (position) {
                    0 -> "all"
                    1 -> "pending"
                    2 -> "confirmed"
                    3 -> "out_for_delivery"
                    4 -> "arrived"
                    5 -> "delivered"
                    6 -> "cancelled"
                    else -> "all"
                }
                viewModel.filterByStatus(selectedStatus)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
                
                // Observe selected order state
                launch {
                    viewModel.selectedOrderState.collect { state ->
                        // Handle order details state if needed
                        // For now, you could show a bottom sheet or navigate to details screen
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadOrders(refresh = true)
        }
        
        buttonRetry.setOnClickListener {
            viewModel.retryLoadOrders()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Start polling for order updates
        startOrderPolling()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop polling when not visible
        isPollingActive = false
    }
    
    /**
     * Polls for order updates every 10 seconds to catch driver status changes.
     * Works in conjunction with Supabase Realtime for redundancy.
     */
    private fun startOrderPolling() {
        isPollingActive = true
        
        viewLifecycleOwner.lifecycleScope.launch {
            while (isPollingActive) {
                kotlinx.coroutines.delay(10_000) // Poll every 10 seconds
                
                if (isPollingActive) {
                    android.util.Log.d("OrderHistoryFragment", "Polling for order updates")
                    viewModel.refreshOrdersSilently()
                }
            }
        }
    }

    private fun updateUI(state: OrderHistoryUiState) {
        // Update loading states
        swipeRefreshLayout.isRefreshing = false
        progressBarLoading.visibility = if (state.isLoading && state.orders.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Handle error state
        if (state.error != null) {
            layoutError.visibility = View.VISIBLE
            textViewErrorMessage.text = state.error
            recyclerViewOrders.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        } else {
            layoutError.visibility = View.GONE
            
            // Handle empty state
            if (state.orders.isEmpty() && !state.isLoading) {
                layoutEmptyState.visibility = View.VISIBLE
                recyclerViewOrders.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                recyclerViewOrders.visibility = View.VISIBLE
                
                // Debug logging
                android.util.Log.d("OrderHistoryFragment", """UPDATE UI - ORDERS LIST:
                |  Total orders: ${state.orders.size}
                |  Orders data:
                """.trimMargin())
                state.orders.forEach { order ->
                    android.util.Log.d("OrderHistoryFragment", "    - ID: ${order.id}, order_number: '${order.order_number}', status: ${order.status}, items: ${order.order_items?.size}")
                }
                
                // Update orders list
                orderHistoryAdapter.submitList(state.orders)
            }
        }
        
        // Update status filter selection if needed
        updateStatusFilterSelection(state.selectedStatusFilter)
    }

    private fun updateStatusFilterSelection(selectedStatus: String) {
        val position = when (selectedStatus) {
            "all" -> 0
            "pending" -> 1
            "confirmed" -> 2
            "out_for_delivery" -> 3
            "arrived" -> 4
            "delivered" -> 5
            "cancelled" -> 6
            else -> 0
        }
        
        if (spinnerStatusFilter.selectedItemPosition != position) {
            spinnerStatusFilter.setSelection(position)
        }
    }
}