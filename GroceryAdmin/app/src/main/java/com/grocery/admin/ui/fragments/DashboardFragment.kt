package com.grocery.admin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.DashboardMetrics
import com.grocery.admin.databinding.FragmentDashboardBinding
import com.grocery.admin.ui.util.SessionExpiredHandler
import com.grocery.admin.ui.viewmodels.DashboardViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshDashboardMetrics()
        }
        
        // Setup retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadDashboardMetrics()
        }
    }
    
    private fun observeViewModel() {
        // Observe session expiration
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionExpired.collect {
                    requireActivity().let { activity ->
                        SessionExpiredHandler.showSessionExpiredDialog(activity)
                    }
                }
            }
        }
        
        // Observe dashboard metrics
        viewModel.dashboardMetrics.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    resource.data?.let { metrics ->
                        showMetrics(metrics)
                        updateLastUpdatedTime()
                    }
                }
                is Resource.Error -> {
                    showError(resource.message ?: getString(R.string.error_generic))
                }
            }
        }
        
        // Observe refresh state
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
    }
    
    private fun showLoading() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.metricsContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }
    
    private fun showMetrics(metrics: DashboardMetrics) {
        binding.loadingContainer.visibility = View.GONE
        binding.metricsContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
        
        // Update Total Orders card
        updateMetricCard(
            binding.cardTotalOrders.root,
            R.drawable.ic_receipt,
            getString(R.string.total_orders),
            metrics.totalOrders.toString()
        )
        
        // Update Total Revenue card
        updateMetricCard(
            binding.cardTotalRevenue.root,
            R.drawable.ic_receipt,
            getString(R.string.total_revenue),
            formatCurrency(metrics.totalRevenue)
        )
        
        // Update Active Customers card
        updateMetricCard(
            binding.cardActiveCustomers.root,
            R.drawable.ic_profile,
            getString(R.string.active_customers),
            metrics.activeCustomers.toString()
        )
        
        // Update Pending Orders card
        updateMetricCard(
            binding.cardPendingOrders.root,
            R.drawable.ic_notifications,
            getString(R.string.pending_orders),
            metrics.pendingOrders.toString()
        )
        
        // Update Low Stock Items card
        updateMetricCard(
            binding.cardLowStock.root,
            R.drawable.ic_inventory,
            getString(R.string.low_stock_items),
            (metrics.lowStockItems ?: 0).toString()
        )
        
        // Update Average Order Value card
        updateMetricCard(
            binding.cardAverageOrder.root,
            R.drawable.ic_receipt,
            getString(R.string.average_order_value),
            formatCurrency(metrics.averageOrderValue)
        )
    }
    
    private fun showError(message: String) {
        binding.loadingContainer.visibility = View.GONE
        binding.metricsContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        
        binding.tvErrorMessage.text = message
    }
    
    private fun updateMetricCard(cardView: View, iconRes: Int, title: String, value: String) {
        cardView.findViewById<ImageView>(R.id.metricIcon).setImageResource(iconRes)
        cardView.findViewById<TextView>(R.id.metricTitle).text = title
        cardView.findViewById<TextView>(R.id.metricValue).text = value
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount)
    }
    
    private fun updateLastUpdatedTime() {
        binding.tvLastUpdated.text = getString(R.string.last_updated, "Just now")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
