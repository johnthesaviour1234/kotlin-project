package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.R
import com.grocery.customer.ui.adapters.OrderItemsAdapter
import com.grocery.customer.domain.repository.OrderRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Order Confirmation Fragment - Displays order confirmation details
 * Shows order number, date, total, delivery address, and items
 * Allows user to view detailed order or continue shopping
 */
@AndroidEntryPoint
class OrderConfirmationFragment : Fragment() {

    private val args: OrderConfirmationFragmentArgs by navArgs()
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    
    @Inject
    lateinit var orderRepository: OrderRepository

    // View references
    private lateinit var textViewOrderNumber: TextView
    private lateinit var textViewOrderDate: TextView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var textViewDeliveryAddress: TextView
    private lateinit var recyclerViewOrderItems: RecyclerView
    private lateinit var buttonViewOrder: Button
    private lateinit var buttonContinueShopping: Button
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "OrderConfirmationFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_order_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        displayOrderDetails()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        textViewOrderNumber = view.findViewById(R.id.textViewOrderNumber)
        textViewOrderDate = view.findViewById(R.id.textViewOrderDate)
        textViewTotalAmount = view.findViewById(R.id.textViewTotalAmount)
        textViewDeliveryAddress = view.findViewById(R.id.textViewDeliveryAddress)
        recyclerViewOrderItems = view.findViewById(R.id.recyclerViewOrderItems)
        buttonViewOrder = view.findViewById(R.id.buttonViewOrder)
        buttonContinueShopping = view.findViewById(R.id.buttonContinueShopping)
        progressBar = ProgressBar(view.context).apply {
            isIndeterminate = true
        }
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        recyclerViewOrderItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderItemsAdapter
        }
    }

    private fun displayOrderDetails() {
        val orderId = args.orderId
        Log.d(TAG, "Fetching order: $orderId")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = orderRepository.getOrder(orderId)
                result.fold(
                    onSuccess = { order ->
                        Log.d(TAG, "Successfully fetched order: ${order.id}")
                        updateUIWithOrder(order)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to fetch order: ${exception.message}", exception)
                        Toast.makeText(
                            context,
                            "Failed to load order details: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading order details: ${e.message}", e)
                Toast.makeText(context, "Error loading order details", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUIWithOrder(order: com.grocery.customer.data.remote.dto.OrderDTO) {
        // Order number with prefix
        textViewOrderNumber.text = "Order #${order.order_number}"
        
        // Order date
        textViewOrderDate.text = formatDate(order.created_at)
        
        // Total amount
        textViewTotalAmount.text = getString(R.string.price_format, order.total_amount)
        
        // Delivery address
        val addressParts = mutableListOf<String>()
        
        order.delivery_address?.let { address ->
            addressParts.add(address.street)
            
            address.apartment?.let {
                if (it.isNotBlank()) {
                    addressParts.add(it)
                }
            }
            
            addressParts.add("${address.city}, ${address.state} ${address.postal_code}")
            
            address.landmark?.let {
                if (it.isNotBlank()) {
                    addressParts.add("Landmark: $it")
                }
            }
        } ?: run {
            addressParts.add("Address not available")
        }
        
        textViewDeliveryAddress.text = addressParts.joinToString("\n")
        
        // Order items
        order.order_items?.let { items ->
            Log.d(TAG, "Setting up ${items.size} order items")
            orderItemsAdapter.submitList(items)
        }
    }

    private fun formatDate(dateString: String): String {
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
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

    private fun setupClickListeners() {
        // View detailed order - also clear backstack to prevent navigation issues
        buttonViewOrder.setOnClickListener {
            try {
                // Clear backstack up to home before navigating to order details
                // This prevents the Checkout and OrderConfirmation from remaining in backstack
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false) // Keep home, remove everything above it
                    .build()
                
                findNavController().navigate(
                    OrderConfirmationFragmentDirections.actionOrderConfirmationToOrderDetail(
                        orderId = args.orderId
                    ),
                    navOptions
                )
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
            }
        }
        
        // Continue shopping - navigate to home and clear entire backstack
        buttonContinueShopping.setOnClickListener {
            try {
                // Clear the entire backstack and go to home
                // This ensures no Checkout or OrderConfirmation remains in backstack
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true) // Pop everything including home
                    .setLaunchSingleTop(true) // Reuse existing home instance if available
                    .build()
                    
                findNavController().navigate(R.id.homeFragment, null, navOptions)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
                // Fallback: Just pop to home
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
