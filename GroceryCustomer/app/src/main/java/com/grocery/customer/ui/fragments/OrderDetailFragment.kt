package com.grocery.customer.ui.fragments

import android.content.Intent
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.OrderDTO
import com.grocery.customer.ui.activities.TrackDeliveryActivity
import com.grocery.customer.ui.adapters.OrderItemsAdapter
import com.grocery.customer.ui.viewmodels.OrderDetailViewModel
import com.grocery.customer.ui.viewmodels.OrderDetailUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for displaying detailed information about a specific order.
 * Shows order details, items, delivery address, and payment information.
 */
@AndroidEntryPoint
class OrderDetailFragment : Fragment() {

    private val viewModel: OrderDetailViewModel by viewModels()
    private val args: OrderDetailFragmentArgs by navArgs()
    
    // Views
    private lateinit var toolbarTitle: TextView
    private lateinit var buttonBack: ImageButton
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var scrollViewContent: ScrollView
    private lateinit var layoutError: LinearLayout
    private lateinit var textViewErrorMessage: TextView
    private lateinit var buttonRetry: Button
    
    // Order Info Views
    private lateinit var textViewOrderNumber: TextView
    private lateinit var textViewOrderDate: TextView
    private lateinit var textViewOrderStatus: TextView
    private lateinit var textViewEstimatedDelivery: TextView
    
    // Items Views
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textViewSubtotal: TextView
    private lateinit var textViewTax: TextView
    private lateinit var textViewDeliveryFee: TextView
    private lateinit var textViewTotal: TextView
    
    // Address Views
    private lateinit var textViewDeliveryAddress: TextView
    
    // Payment Views
    private lateinit var textViewPaymentMethod: TextView
    private lateinit var textViewPaymentStatus: TextView
    
    // Notes Views
    private lateinit var textViewNotes: TextView
    private lateinit var layoutNotes: MaterialCardView
    
    // Track Delivery Button
    private lateinit var buttonTrackDelivery: Button
    
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private var currentOrder: OrderDTO? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
        
        // Load order details
        viewModel.loadOrderDetails(args.orderId)
    }

    private fun initializeViews(view: View) {
        toolbarTitle = view.findViewById(R.id.toolbarTitle)
        buttonBack = view.findViewById(R.id.buttonBack)
        progressBarLoading = view.findViewById(R.id.progressBarLoading)
        scrollViewContent = view.findViewById(R.id.scrollViewContent)
        layoutError = view.findViewById(R.id.layoutError)
        textViewErrorMessage = view.findViewById(R.id.textViewErrorMessage)
        buttonRetry = view.findViewById(R.id.buttonRetry)
        
        // Order info
        textViewOrderNumber = view.findViewById(R.id.textViewOrderNumber)
        textViewOrderDate = view.findViewById(R.id.textViewOrderDate)
        textViewOrderStatus = view.findViewById(R.id.textViewOrderStatus)
        textViewEstimatedDelivery = view.findViewById(R.id.textViewEstimatedDelivery)
        
        // Items
        recyclerViewItems = view.findViewById(R.id.recyclerViewItems)
        textViewSubtotal = view.findViewById(R.id.textViewSubtotal)
        textViewTax = view.findViewById(R.id.textViewTax)
        textViewDeliveryFee = view.findViewById(R.id.textViewDeliveryFee)
        textViewTotal = view.findViewById(R.id.textViewTotal)
        
        // Address
        textViewDeliveryAddress = view.findViewById(R.id.textViewDeliveryAddress)
        
        // Payment
        textViewPaymentMethod = view.findViewById(R.id.textViewPaymentMethod)
        textViewPaymentStatus = view.findViewById(R.id.textViewPaymentStatus)
        
        // Notes
        textViewNotes = view.findViewById(R.id.textViewNotes)
        layoutNotes = view.findViewById(R.id.layoutNotes)
        
        // Track Delivery Button
        buttonTrackDelivery = view.findViewById(R.id.buttonTrackDelivery)
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        
        recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderItemsAdapter
            // Disable nested scrolling to work properly inside ScrollView
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        buttonRetry.setOnClickListener {
            viewModel.loadOrderDetails(args.orderId)
        }
        
        buttonTrackDelivery.setOnClickListener {
            currentOrder?.let { order ->
                val intent = Intent(requireContext(), TrackDeliveryActivity::class.java).apply {
                    putExtra(TrackDeliveryActivity.EXTRA_ORDER_ID, order.id)
                    putExtra(TrackDeliveryActivity.EXTRA_ORDER_NUMBER, order.order_number)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: OrderDetailUiState) {
        // Handle loading state
        progressBarLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Handle error state
        if (state.error != null) {
            layoutError.visibility = View.VISIBLE
            scrollViewContent.visibility = View.GONE
            textViewErrorMessage.text = state.error
        } else if (state.order != null) {
            layoutError.visibility = View.GONE
            scrollViewContent.visibility = View.VISIBLE
            
            // Populate order details
            populateOrderDetails(state.order)
        } else {
            // Loading or initial state
            layoutError.visibility = View.GONE
            scrollViewContent.visibility = View.GONE
        }
    }

    private fun populateOrderDetails(order: OrderDTO) {
        currentOrder = order
        
        // Update toolbar title
        toolbarTitle.text = "Order #${order.order_number}"
        
        // Order info
        textViewOrderNumber.text = "Order #${order.order_number}"
        textViewOrderDate.text = "Placed on ${formatDate(order.created_at)}"
        textViewOrderStatus.text = order.status.replaceFirstChar { it.uppercase() }
        setStatusColor(order.status)
        
        // Show Track Delivery button for out_for_delivery status
        if (order.status == "out_for_delivery") {
            buttonTrackDelivery.visibility = View.VISIBLE
        } else {
            buttonTrackDelivery.visibility = View.GONE
        }
        
        // Estimated delivery
        if (order.estimated_delivery_time != null) {
            textViewEstimatedDelivery.text = "Estimated delivery: ${formatDate(order.estimated_delivery_time)}"
            textViewEstimatedDelivery.visibility = View.VISIBLE
        } else {
            textViewEstimatedDelivery.visibility = View.GONE
        }
        
        // Order items
        order.order_items?.let { items ->
            orderItemsAdapter.submitList(items)
        }
        
        // Price breakdown
        textViewSubtotal.text = getString(R.string.price_format, order.subtotal ?: order.total_amount)
        textViewTax.text = getString(R.string.price_format, order.tax_amount ?: 0.0)
        textViewDeliveryFee.text = getString(R.string.price_format, order.delivery_fee ?: 0.0)
        textViewTotal.text = getString(R.string.price_format, order.total_amount)
        
        // Delivery address
        textViewDeliveryAddress.text = order.delivery_address?.let { formatAddress(it) } ?: "Address not available"
        
        // Payment info
        textViewPaymentMethod.text = (order.payment_method ?: "Cash").replaceFirstChar { it.uppercase() }
        textViewPaymentStatus.text = (order.payment_status ?: "Pending").replaceFirstChar { it.uppercase() }
        setPaymentStatusColor(order.payment_status ?: "pending")
        
        // Notes
        if (!order.notes.isNullOrEmpty()) {
            layoutNotes.visibility = View.VISIBLE
            textViewNotes.text = order.notes
        } else {
            layoutNotes.visibility = View.GONE
        }
    }

    private fun formatDate(dateString: String): String {
        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (!pattern.contains("X")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(dateString)
                if (date != null) return outputFormat.format(date)
            } catch (_: Exception) { /* try next */ }
        }
        return dateString
    }

    private fun formatAddress(address: com.grocery.customer.data.remote.dto.DeliveryAddressDTO): String {
        return buildString {
            if (!address.apartment.isNullOrEmpty()) {
                append("${address.apartment}, ")
            }
            append("${address.street}, ")
            if (!address.landmark.isNullOrEmpty()) {
                append("${address.landmark}, ")
            }
            append("${address.city}, ${address.state} ${address.postal_code}")
        }
    }

    private fun setStatusColor(status: String) {
        val color = when (status.lowercase()) {
            "pending" -> R.color.status_pending
            "confirmed" -> R.color.status_confirmed
            "preparing" -> R.color.status_preparing
            "ready" -> R.color.status_ready
            "delivered" -> R.color.status_delivered
            "cancelled" -> R.color.status_cancelled
            else -> R.color.text_secondary
        }
        
        textViewOrderStatus.setTextColor(
            resources.getColor(color, requireContext().theme)
        )
    }

    private fun setPaymentStatusColor(status: String) {
        val color = when (status.lowercase()) {
            "paid" -> R.color.status_delivered
            "pending" -> R.color.status_pending
            "failed" -> R.color.status_cancelled
            "refunded" -> R.color.status_preparing
            else -> R.color.text_secondary
        }
        
        textViewPaymentStatus.setTextColor(
            resources.getColor(color, requireContext().theme)
        )
    }
}