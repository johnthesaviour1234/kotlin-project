package com.grocery.delivery.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.grocery.delivery.R
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.data.local.PreferencesManager
import com.grocery.delivery.databinding.ActivityMainBinding
import com.grocery.delivery.ui.adapters.AvailableOrdersAdapter
import com.grocery.delivery.ui.dialogs.OrderDetailDialog
import com.grocery.delivery.ui.viewmodels.AvailableOrdersViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity - Displays available orders for delivery
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: AvailableOrdersViewModel by viewModels()
    private lateinit var adapter: AvailableOrdersAdapter
    private var acceptedAssignment: DeliveryAssignment? = null
    
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNavigation()
    }

    override fun setupObservers() {
        viewModel.ordersState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    setLoadingState(true)
                    hideErrorMessage()
                    hideEmptyState()
                }
                is Resource.Success -> {
                    setLoadingState(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    val orders = resource.data ?: emptyList()
                    if (orders.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        adapter.submitList(orders)
                    }
                }
                is Resource.Error -> {
                    setLoadingState(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    showErrorMessage(resource.message ?: "Failed to load orders")
                }
            }
        }
        
        // Observe action state (accept/decline)
        viewModel.actionState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    hideLoading()
                    Toast.makeText(this, resource.data ?: "Action completed", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to active delivery screen after accepting
                    acceptedAssignment?.let { assignment ->
                        navigateToActiveDelivery(assignment)
                        acceptedAssignment = null
                    }
                    
                    viewModel.resetActionState()
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(resource.message ?: "Action failed")
                    Toast.makeText(this, resource.message ?: "Action failed", Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                null -> {
                    // Reset state
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Available Orders"
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun logout() {
        // Clear all stored data
        preferencesManager.clearAll()
        
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        adapter = AvailableOrdersAdapter { assignment ->
            // If order is pending, show detail dialog for accept/decline
            // If order is already accepted/in_transit/arrived, navigate to active delivery
            when (assignment.status) {
                "pending" -> showOrderDetailDialog(assignment)
                "accepted", "in_transit", "arrived" -> navigateToActiveDelivery(assignment)
                else -> showOrderDetailDialog(assignment)
            }
        }
        binding.recyclerViewOrders.adapter = adapter
    }
    
    private fun showOrderDetailDialog(assignment: DeliveryAssignment) {
        val dialog = OrderDetailDialog(
            assignment = assignment,
            onAccept = { acceptedAssignment ->
                this.acceptedAssignment = acceptedAssignment
                viewModel.acceptOrder(acceptedAssignment.id)
            },
            onDecline = { assignmentId ->
                viewModel.declineOrder(assignmentId)
            }
        )
        dialog.show(supportFragmentManager, "OrderDetailDialog")
    }
    
    private fun navigateToActiveDelivery(assignment: DeliveryAssignment) {
        val intent = Intent(this, ActiveDeliveryActivity::class.java).apply {
            putExtra(ActiveDeliveryActivity.EXTRA_ASSIGNMENT, assignment)
        }
        startActivity(intent)
    }
    
    private fun navigateToDeliveryHistory() {
        val intent = Intent(this, DeliveryHistoryActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_available_orders
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_available_orders -> {
                    // Already on this screen
                    true
                }
                R.id.nav_active_delivery -> {
                    // Check for active delivery and navigate
                    checkAndNavigateToActiveDelivery()
                    true
                }
                R.id.nav_history -> {
                    navigateToDeliveryHistory()
                    true
                }
                R.id.nav_profile -> {
                    navigateToProfile()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun checkAndNavigateToActiveDelivery() {
        // Get the first accepted or in_transit order from current list
        val activeOrders = viewModel.ordersState.value?.data?.filter { 
            it.status == "accepted" || it.status == "in_transit" 
        }
        
        if (!activeOrders.isNullOrEmpty()) {
            navigateToActiveDelivery(activeOrders.first())
        } else {
            Toast.makeText(this, "No active delivery found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshOrders()
        }
    }

    private fun setLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewOrders.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewOrders.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewOrders.visibility = View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        binding.textViewError.text = message
        binding.textViewError.visibility = View.VISIBLE
        binding.recyclerViewOrders.visibility = View.GONE
    }

    private fun hideErrorMessage() {
        binding.textViewError.visibility = View.GONE
    }
}
