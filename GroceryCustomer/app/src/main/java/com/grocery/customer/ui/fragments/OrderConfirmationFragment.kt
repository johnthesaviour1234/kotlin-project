package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.R
import com.grocery.customer.ui.adapters.OrderItemsAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Order Confirmation Fragment - Displays order confirmation details
 * Shows order number, date, total, delivery address, and items
 * Allows user to view detailed order or continue shopping
 */
@AndroidEntryPoint
class OrderConfirmationFragment : Fragment() {

    private val args: OrderConfirmationFragmentArgs by navArgs()
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    // View references
    private lateinit var textViewOrderNumber: TextView
    private lateinit var textViewOrderDate: TextView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var textViewDeliveryAddress: TextView
    private lateinit var recyclerViewOrderItems: RecyclerView
    private lateinit var buttonViewOrder: Button
    private lateinit var buttonContinueShopping: Button

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
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        recyclerViewOrderItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderItemsAdapter
        }
    }

    private fun displayOrderDetails() {
        try {
            val order = args.order
            
            Log.d(TAG, "Displaying order confirmation for: ${order.id}")
            
            // Order number with prefix
            textViewOrderNumber.text = "Order #${order.order_number}"
            
            // Order date
            textViewOrderDate.text = formatDate(order.created_at)
            
            // Total amount
            textViewTotalAmount.text = getString(R.string.price_format, order.total_amount)
            
            // Delivery address
            val addressParts = mutableListOf<String>()
            addressParts.add(order.delivery_address.street)
            
            order.delivery_address.apartment?.let {
                if (it.isNotBlank()) {
                    addressParts.add(it)
                }
            }
            
            addressParts.add("${order.delivery_address.city}, ${order.delivery_address.state} ${order.delivery_address.postal_code}")
            
            order.delivery_address.landmark?.let {
                if (it.isNotBlank()) {
                    addressParts.add("Landmark: $it")
                }
            }
            
            textViewDeliveryAddress.text = addressParts.joinToString("\n")
            
            // Order items
            order.order_items?.let { items ->
                Log.d(TAG, "Setting up ${items.size} order items")
                orderItemsAdapter.submitList(items)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying order details: ${e.message}", e)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun setupClickListeners() {
        // View detailed order
        buttonViewOrder.setOnClickListener {
            try {
                findNavController().navigate(
                    OrderConfirmationFragmentDirections.actionOrderConfirmationToOrderDetail(
                        orderId = args.order.id
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
            }
        }
        
        // Continue shopping - navigate to home
        buttonContinueShopping.setOnClickListener {
            try {
                // Pop back stack to go home
                findNavController().popBackStack(R.id.homeFragment, false)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
                // Fallback: navigate home
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
