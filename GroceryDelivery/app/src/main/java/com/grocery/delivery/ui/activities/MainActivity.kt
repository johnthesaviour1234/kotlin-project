package com.grocery.delivery.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.grocery.delivery.R
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.data.local.PreferencesManager
import com.grocery.delivery.databinding.ActivityMainBinding
import com.grocery.delivery.ui.adapters.AvailableOrdersAdapter
import com.grocery.delivery.services.ConnectionState
import com.grocery.delivery.services.RealtimeManager
import com.grocery.delivery.ui.dialogs.OrderDetailDialog
import com.grocery.delivery.ui.viewmodels.AvailableOrdersViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
    
    @Inject
    lateinit var realtimeManager: RealtimeManager

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNavigation()
        setupRealtimeConnection()
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
                        // Sort orders: accepted/in_transit at top (newest first), then pending (newest first)
                        val sortedOrders = orders.sortedWith(
                            compareByDescending<DeliveryAssignment> { 
                                // Priority: accepted > in_transit > arrived > pending
                                when(it.status) {
                                    "accepted" -> 4
                                    "in_transit" -> 3
                                    "arrived" -> 2
                                    else -> 1
                                }
                            }.thenByDescending { 
                                // Within same status, newest first (using assigned_at or created_at)
                                it.assignedAt ?: it.createdAt ?: ""
                            }
                        )
                        adapter.submitList(sortedOrders)
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
                    // Update status to "accepted" before navigating so ActiveDeliveryActivity shows the correct button
                    acceptedAssignment?.let { assignment ->
                        val updatedAssignment = assignment.copy(status = "accepted")
                        navigateToActiveDelivery(updatedAssignment)
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
    
    /**
     * Setup real-time connection for order updates.
     */
    private fun setupRealtimeConnection() {
        lifecycleScope.launch {
            realtimeManager.connect()
        }
        
        // Observe connection state
        lifecycleScope.launch {
            realtimeManager.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        android.util.Log.d("MainActivity", "Realtime connected")
                    }
                    is ConnectionState.Connecting -> {
                        android.util.Log.d("MainActivity", "Realtime connecting...")
                    }
                    is ConnectionState.Disconnected -> {
                        android.util.Log.d("MainActivity", "Realtime disconnected")
                    }
                    is ConnectionState.Error -> {
                        android.util.Log.e("MainActivity", "Realtime error: ${state.message}")
                    }
                }
            }
        }
        
        // Observe order refresh triggers from realtime
        lifecycleScope.launch {
            realtimeManager.orderRefreshTrigger.collect {
                android.util.Log.d("MainActivity", "🔔 Realtime order update received - auto-refreshing list")
                viewModel.refreshOrders()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reset toolbar title when returning to this activity
        supportActionBar?.title = "Available Orders"
        // Reconnect realtime if needed
        if (!realtimeManager.isConnected()) {
            lifecycleScope.launch {
                realtimeManager.connect()
            }
        }
        // Refresh orders when returning to screen
        viewModel.refreshOrders()
    }
    
    override fun onPause() {
        super.onPause()
        // Keep realtime connected even when app is in background
        // Only disconnect on logout or when explicitly needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Disconnect realtime when activity is destroyed
        lifecycleScope.launch {
            realtimeManager.disconnect()
        }
    }
}
