package com.grocery.admin.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.databinding.DialogUpdateOrderStatusBinding
import com.grocery.admin.ui.viewmodels.OrderDetailViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dialog for updating order status
 */
@AndroidEntryPoint
class UpdateOrderStatusDialog : DialogFragment() {

    private var _binding: DialogUpdateOrderStatusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels({ requireParentFragment() })

    private lateinit var orderId: String
    private lateinit var currentStatus: String

    private val statusOptions = listOf(
        StatusOption("pending", "Pending", "Order placed, awaiting confirmation"),
        StatusOption("confirmed", "Confirmed", "Order confirmed by admin"),
        StatusOption("preparing", "Preparing", "Order is being prepared"),
        StatusOption("ready", "Ready", "Order ready for delivery"),
        StatusOption("out_for_delivery", "Out for Delivery", "Order is on the way"),
        StatusOption("delivered", "Delivered", "Order successfully delivered"),
        StatusOption("cancelled", "Cancelled", "Order has been cancelled")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString(ARG_ORDER_ID) ?: ""
        currentStatus = arguments?.getString(ARG_CURRENT_STATUS) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUpdateOrderStatusBinding.inflate(layoutInflater)
        
        setupUI()
        observeViewModel()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }

    private fun setupUI() {
        // Dialog title
        binding.tvTitle.text = "Update Order Status"
        binding.tvOrderId.text = "Order: ORD-${orderId.take(8)}"

        // Current status
        val currentStatusOption = statusOptions.find { it.value == currentStatus }
        binding.tvCurrentStatus.text = "Current: ${currentStatusOption?.label ?: currentStatus}"
        binding.tvCurrentStatus.setTextColor(ContextCompat.getColor(requireContext(), getStatusColor(currentStatus)))

        // Status dropdown
        setupStatusSpinner()

        // Buttons
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpdate.setOnClickListener {
            updateOrderStatus()
        }
    }

    private fun setupStatusSpinner() {
        // Filter out invalid status transitions
        val availableStatuses = getAvailableStatusTransitions(currentStatus)
        
        val adapter = object : ArrayAdapter<StatusOption>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableStatuses
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val statusOption = getItem(position)
                (view as? android.widget.TextView)?.text = statusOption?.label
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = layoutInflater.inflate(R.layout.item_status_dropdown, parent, false)
                val statusOption = getItem(position)
                
                statusOption?.let { option ->
                    view.findViewById<android.widget.TextView>(R.id.tvStatusLabel).text = option.label
                    view.findViewById<android.widget.TextView>(R.id.tvStatusDescription).text = option.description
                    view.findViewById<View>(R.id.statusIndicator).setBackgroundColor(
                        ContextCompat.getColor(requireContext(), getStatusColor(option.value))
                    )
                }
                
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter

        // Pre-select current status or first available option
        val currentIndex = availableStatuses.indexOfFirst { it.value == currentStatus }
        if (currentIndex >= 0) {
            binding.spinnerStatus.setSelection(currentIndex)
        }
    }

    private fun getAvailableStatusTransitions(currentStatus: String): List<StatusOption> {
        // Define valid status transitions
        val validTransitions = when (currentStatus.lowercase()) {
            "pending" -> listOf("confirmed", "cancelled")
            "confirmed" -> listOf("preparing", "cancelled")
            "preparing" -> listOf("ready", "cancelled")
            "ready" -> listOf("out_for_delivery", "cancelled")
            "out_for_delivery" -> listOf("delivered", "cancelled")
            "delivered" -> emptyList() // Final state
            "cancelled" -> emptyList() // Final state
            else -> statusOptions.map { it.value }
        }

        return statusOptions.filter { it.value in validTransitions }
    }

    private fun updateOrderStatus() {
        val selectedStatusOption = binding.spinnerStatus.selectedItem as? StatusOption
        val notes = binding.etNotes.text?.toString()?.trim() ?: ""

        if (selectedStatusOption == null) {
            showError("Please select a status")
            return
        }

        if (selectedStatusOption.value == currentStatus) {
            showError("Please select a different status")
            return
        }

        // Show confirmation for critical actions
        if (selectedStatusOption.value == "cancelled") {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? This action cannot be undone.")
                .setPositiveButton("Cancel Order") { _, _ ->
                    performStatusUpdate(selectedStatusOption.value, notes)
                }
                .setNegativeButton("Keep Order", null)
                .show()
        } else {
            performStatusUpdate(selectedStatusOption.value, notes)
        }
    }

    private fun performStatusUpdate(newStatus: String, notes: String) {
        binding.btnUpdate.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        viewModel.updateOrderStatus(orderId, newStatus, notes)
    }

    private fun observeViewModel() {
        viewModel.updateStatusResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnUpdate.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Order status updated successfully")
                    viewModel.resetUpdateStatusResult()
                    // Reload order details after successful status update
                    viewModel.loadOrderDetails(orderId)
                    dismiss()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUpdate.isEnabled = true
                    showError(resource.message ?: "Failed to update status")
                    viewModel.resetUpdateStatusResult()
                }
            }
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "preparing" -> R.color.status_preparing
            "ready" -> R.color.status_ready
            "out_for_delivery" -> R.color.primary
            "delivered" -> R.color.status_delivered
            "cancelled" -> R.color.status_cancelled
            else -> R.color.text_secondary
        }
    }

    private fun showSuccess(message: String) {
        view?.let { v ->
            Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        view?.let { v ->
            Snackbar.make(v, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class StatusOption(
        val value: String,
        val label: String,
        val description: String
    )

    companion object {
        private const val ARG_ORDER_ID = "order_id"
        private const val ARG_CURRENT_STATUS = "current_status"

        fun newInstance(orderId: String, currentStatus: String): UpdateOrderStatusDialog {
            return UpdateOrderStatusDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_CURRENT_STATUS, currentStatus)
                }
            }
        }
    }
}