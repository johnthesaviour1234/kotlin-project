package com.grocery.delivery.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.grocery.delivery.databinding.ActivityDeliveryHistoryBinding
import com.grocery.delivery.ui.adapters.DeliveryHistoryAdapter
import com.grocery.delivery.ui.dialogs.OrderDetailDialog
import com.grocery.delivery.ui.viewmodels.DeliveryHistoryViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity for displaying delivery history
 */
@AndroidEntryPoint
class DeliveryHistoryActivity : BaseActivity<ActivityDeliveryHistoryBinding>() {

    private val viewModel: DeliveryHistoryViewModel by viewModels()
    private lateinit var adapter: DeliveryHistoryAdapter

    override fun inflateViewBinding(): ActivityDeliveryHistoryBinding {
        return ActivityDeliveryHistoryBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    override fun setupObservers() {
        // Observe history state
        viewModel.historyState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    setLoadingState(true)
                    hideErrorMessage()
                    hideEmptyState()
                }
                is Resource.Success -> {
                    setLoadingState(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    val deliveries = resource.data ?: emptyList()
                    if (deliveries.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        adapter.submitList(deliveries)
                    }
                }
                is Resource.Error -> {
                    setLoadingState(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    showErrorMessage(resource.message ?: "Failed to load history")
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Observe stats state
        viewModel.statsState.observe(this) { stats ->
            updateStatsUI(stats)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Delivery History"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = DeliveryHistoryAdapter { assignment ->
            showOrderDetailDialog(assignment)
        }
        binding.recyclerViewHistory.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshHistory()
        }
    }

    private fun showOrderDetailDialog(assignment: com.grocery.delivery.data.dto.DeliveryAssignment) {
        val dialog = OrderDetailDialog(
            assignment = assignment,
            onAccept = null, // No actions needed for completed orders
            onDecline = null
        )
        dialog.show(supportFragmentManager, "OrderDetailDialog")
    }

    private fun updateStatsUI(stats: DeliveryHistoryViewModel.DeliveryStats) {
        binding.tvTotalDeliveries.text = stats.totalDeliveries.toString()
        
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        binding.tvTotalEarnings.text = currencyFormatter.format(stats.totalEarnings)
        
        binding.tvAverageTime.text = "${stats.averageDeliveryTime} min"
    }

    private fun setLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewHistory.visibility = if (show) View.GONE else View.VISIBLE
        binding.statsCard.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.statsCard.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.VISIBLE
        binding.statsCard.visibility = View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        binding.textViewError.text = message
        binding.textViewError.visibility = View.VISIBLE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.statsCard.visibility = View.GONE
    }

    private fun hideErrorMessage() {
        binding.textViewError.visibility = View.GONE
    }
}
