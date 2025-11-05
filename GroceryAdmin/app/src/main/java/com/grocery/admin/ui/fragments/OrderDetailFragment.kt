package com.grocery.admin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.OrderDto
import com.grocery.admin.databinding.FragmentOrderDetailBinding
import com.grocery.admin.ui.adapters.OrderItemsAdapter
import com.grocery.admin.ui.dialogs.UpdateOrderStatusDialog
import com.grocery.admin.ui.dialogs.AssignDriverDialog
import com.grocery.admin.ui.viewmodels.OrderDetailViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment displaying detailed order information with management capabilities
 */
@AndroidEntryPoint
class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels()
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    private var currentOrder: OrderDto? = null
    private var isPollingActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val orderId = arguments?.getString("orderId")
        if (orderId == null) {
            findNavController().navigateUp()
            return
        }

        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        // Load order details
        viewModel.loadOrderDetails(orderId)
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Action buttons
        binding.btnUpdateStatus.setOnClickListener {
            currentOrder?.let { order ->
                showUpdateStatusDialog(order)
            }
        }

        binding.btnAssignDriver.setOnClickListener {
            currentOrder?.let { order ->
                showAssignDriverDialog(order)
            }
        }

        binding.btnCallCustomer.setOnClickListener {
            currentOrder?.let { order ->
                // TODO: Implement call customer functionality
                Snackbar.make(binding.root, "Call customer feature coming soon", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Refresh functionality
        binding.swipeRefresh.setOnRefreshListener {
            currentOrder?.let { order ->
                viewModel.loadOrderDetails(order.id)
            }
        }
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderItemsAdapter
        }
    }

    private fun observeViewModel() {
        // Observe order details
        viewModel.orderDetails.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.contentLayout.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                    
                    resource.data?.let { order ->
                        currentOrder = order
                        displayOrderDetails(order)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.contentLayout.visibility = View.GONE
                    binding.errorLayout.visibility = View.VISIBLE
                    
                    binding.tvErrorMessage.text = resource.message
                    binding.btnRetry.setOnClickListener {
                        currentOrder?.let { order ->
                            viewModel.loadOrderDetails(order.id)
                        }
                    }
                }
            }
        }

        // Observe update status result
        viewModel.updateStatusResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    showSuccess("Order status updated successfully")
                    currentOrder?.let { viewModel.loadOrderDetails(it.id) }
                }
                is Resource.Error -> {
                    showError(resource.message ?: "Failed to update status")
                }
                is Resource.Loading -> {
                    // Show loading in dialog
                }
            }
        }

        // Observe assign driver result
        viewModel.assignDriverResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    showSuccess("Driver assigned successfully")
                    currentOrder?.let { viewModel.loadOrderDetails(it.id) }
                }
                is Resource.Error -> {
                    showError(resource.message ?: "Failed to assign driver")
                }
                is Resource.Loading -> {
                    // Show loading in dialog
                }
            }
        }
    }

    private fun displayOrderDetails(order: OrderDto) {
        // Order header - use actual order_number from backend if available
        binding.tvOrderNumber.text = order.orderNumber ?: "ORD-${order.id.take(8)}"
        binding.tvOrderDate.text = formatDate(order.createdAt)
        
        // Status
        binding.tvStatus.text = order.status.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        updateStatusColor(order.status)

        // Customer information from customer_info field (backend provides this)
        val customerName = when {
            !order.customerInfo?.name.isNullOrBlank() -> order.customerInfo?.name
            !order.customerInfo?.email.isNullOrBlank() -> order.customerInfo?.email?.substringBefore('@')
            else -> "Guest Customer (ID: ${order.customerId.take(8)})"
        }
        binding.tvCustomerName.text = customerName
        
        binding.tvCustomerEmail.text = order.customerInfo?.email?.ifBlank { "No email provided" } ?: "No email provided"
        binding.tvCustomerPhone.text = order.customerInfo?.phone?.ifBlank { "No phone" } ?: "No phone"

        // Delivery address
        displayDeliveryAddress(order)

        // Order items
        order.items?.let { items ->
            binding.tvItemsCount.text = "${items.size} items"
            orderItemsAdapter.submitList(items)
        } ?: run {
            binding.tvItemsCount.text = "0 items"
            orderItemsAdapter.submitList(emptyList())
        }

        // Order totals
        displayOrderTotals(order)

        // Action buttons visibility
        updateActionButtons(order)
    }

    private fun displayDeliveryAddress(order: OrderDto) {
        order.deliveryAddress?.let { address ->
            val fullAddress = listOfNotNull(
                address.street,
                address.apartment,
                address.city,
                address.state,
                address.postalCode
            ).joinToString(", ")
            
            binding.tvDeliveryAddress.text = fullAddress.ifEmpty { "No address provided" }
            address.landmark?.let {
                binding.tvLandmark.text = "Landmark: $it"
                binding.tvLandmark.visibility = View.VISIBLE
            } ?: run {
                binding.tvLandmark.visibility = View.GONE
            }
        } ?: run {
            binding.tvDeliveryAddress.text = "No address provided"
            binding.tvLandmark.visibility = View.GONE
        }
    }

    private fun displayOrderTotals(order: OrderDto) {
        // Use actual values from the backend (NOT recalculated)
        binding.tvSubtotal.text = formatCurrency(order.subtotal)
        binding.tvTax.text = formatCurrency(order.taxAmount)
        binding.tvDeliveryFee.text = formatCurrency(order.deliveryFee)
        binding.tvTotal.text = formatCurrency(order.totalAmount)
    }

    private fun updateStatusColor(status: String) {
        val statusColor = when (status.lowercase()) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "preparing" -> R.color.status_preparing
            "ready" -> R.color.status_ready
            "out_for_delivery" -> R.color.primary
            "delivered" -> R.color.status_delivered
            "cancelled" -> R.color.status_cancelled
            else -> R.color.text_secondary
        }
        
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
        binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), statusColor))
    }

    private fun updateActionButtons(order: OrderDto) {
        when (order.status.lowercase()) {
            "pending" -> {
                binding.btnAssignDriver.visibility = View.VISIBLE
                binding.btnUpdateStatus.visibility = View.VISIBLE
            }
            "delivered", "cancelled" -> {
                binding.btnAssignDriver.visibility = View.GONE
                binding.btnUpdateStatus.visibility = View.GONE
            }
            else -> {
                binding.btnAssignDriver.visibility = View.GONE
                binding.btnUpdateStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun showUpdateStatusDialog(order: OrderDto) {
        val dialog = UpdateOrderStatusDialog.newInstance(
            orderId = order.id, 
            currentStatus = order.status,
            hasDeliveryAssignment = order.hasDeliveryAssignment()
        )
        dialog.show(childFragmentManager, "UpdateStatusDialog")
    }

    private fun showAssignDriverDialog(order: OrderDto) {
        val dialog = AssignDriverDialog.newInstance(order.id)
        dialog.show(childFragmentManager, "AssignDriverDialog")
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    override fun onResume() {
        super.onResume()
        // Start polling for order detail updates
        startOrderDetailPolling()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop polling when not visible
        isPollingActive = false
    }
    
    /**
     * Polls for order detail updates every 15 seconds.
     * Ensures admin sees status changes made by drivers.
     */
    private fun startOrderDetailPolling() {
        isPollingActive = true
        
        viewLifecycleOwner.lifecycleScope.launch {
            while (isPollingActive) {
                delay(15_000) // Poll every 15 seconds
                
                if (isPollingActive) {
                    android.util.Log.d("OrderDetailFragment", "Polling for order detail updates (admin)")
                    currentOrder?.let { order ->
                        viewModel.loadOrderDetails(order.id)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPollingActive = false
        _binding = null
    }

    companion object {
        fun newInstance(orderId: String): OrderDetailFragment {
            return OrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("orderId", orderId)
                }
            }
        }
    }
}