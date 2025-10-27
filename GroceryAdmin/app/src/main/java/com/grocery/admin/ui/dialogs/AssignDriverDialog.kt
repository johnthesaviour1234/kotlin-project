package com.grocery.admin.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.databinding.DialogAssignDriverBinding
import com.grocery.admin.ui.adapters.DriversAdapter
import com.grocery.admin.data.remote.dto.DeliveryPersonnelDto
import com.grocery.admin.ui.viewmodels.OrderDetailViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dialog for assigning delivery drivers to orders
 */
@AndroidEntryPoint
class AssignDriverDialog : DialogFragment() {

    private var _binding: DialogAssignDriverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels({ requireParentFragment() })
    private lateinit var driversAdapter: DriversAdapter

    private lateinit var orderId: String
    private var selectedDriverId: String? = null
    private var allDrivers: List<DeliveryPersonnelDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString(ARG_ORDER_ID) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAssignDriverBinding.inflate(layoutInflater)
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        // Load delivery personnel from API
        viewModel.loadDeliveryPersonnel()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }

    private fun setupUI() {
        // Dialog title
        binding.tvTitle.text = "Assign Delivery Driver"
        binding.tvOrderId.text = "Order: ORD-${orderId.take(8)}"

        // Search functionality
        binding.etSearchDriver.addTextChangedListener { text ->
            filterDrivers(text?.toString() ?: "")
        }

        // Estimated time
        binding.etEstimatedTime.setText("30") // Default 30 minutes

        // Buttons
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnAssign.setOnClickListener {
            assignDriver()
        }

        // Initially disable assign button
        binding.btnAssign.isEnabled = false
    }

    private fun setupRecyclerView() {
        driversAdapter = DriversAdapter { driver ->
            selectedDriverId = driver.id
            binding.btnAssign.isEnabled = driver.isActive
            
            if (!driver.isActive) {
                showError("This driver is not currently available")
            }
        }

        binding.rvDrivers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = driversAdapter
        }
    }

    private fun filterDrivers(query: String) {
        val filteredDrivers = if (query.isBlank()) {
            allDrivers
        } else {
            allDrivers.filter { driver ->
                driver.fullName.contains(query, ignoreCase = true) ||
                driver.email.contains(query, ignoreCase = true)
            }
        }
        
        driversAdapter.submitList(filteredDrivers)
        updateDriversCount(filteredDrivers.size)
    }

    private fun updateDriversCount(count: Int) {
        val availableCount = allDrivers.count { it.isActive }
        binding.tvDriversCount.text = "Showing $count drivers ($availableCount available)"
    }

    private fun assignDriver() {
        val driverId = selectedDriverId
        if (driverId == null) {
            showError("Please select a driver")
            return
        }

        val estimatedTimeText = binding.etEstimatedTime.text?.toString()?.trim()
        if (estimatedTimeText.isNullOrBlank()) {
            showError("Please enter estimated delivery time")
            return
        }

        val estimatedMinutes = estimatedTimeText.toIntOrNull()
        if (estimatedMinutes == null || estimatedMinutes <= 0) {
            showError("Please enter a valid time in minutes")
            return
        }

        // Show confirmation
        val selectedDriver = allDrivers.find { it.id == driverId }
        selectedDriver?.let { driver ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Assignment")
                .setMessage("Assign order ORD-${orderId.take(8)} to ${driver.fullName}?\n\nEstimated delivery time: $estimatedMinutes minutes")
                .setPositiveButton("Assign") { _, _ ->
                    performDriverAssignment(driverId, estimatedMinutes)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performDriverAssignment(driverId: String, estimatedMinutes: Int) {
        binding.btnAssign.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        viewModel.assignDriverToOrder(orderId, driverId, estimatedMinutes)
    }

    private fun observeViewModel() {
        // Observe delivery personnel
        viewModel.deliveryPersonnel.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvDrivers.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvDrivers.visibility = View.VISIBLE
                    
                    allDrivers = resource.data ?: emptyList()
                    driversAdapter.submitList(allDrivers)
                    updateDriversCount(allDrivers.size)
                    
                    if (allDrivers.isEmpty()) {
                        showError("No delivery drivers available")
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvDrivers.visibility = View.VISIBLE
                    showError(resource.message ?: "Failed to load drivers")
                }
            }
        }
        
        // Observe assign driver result
        viewModel.assignDriverResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnAssign.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Driver assigned successfully")
                    viewModel.resetAssignDriverResult()
                    // Reload order details after successful assignment
                    viewModel.loadOrderDetails(orderId)
                    dismiss()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAssign.isEnabled = true
                    showError(resource.message ?: "Failed to assign driver")
                    viewModel.resetAssignDriverResult()
                }
            }
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

    companion object {
        private const val ARG_ORDER_ID = "order_id"

        fun newInstance(orderId: String): AssignDriverDialog {
            return AssignDriverDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}