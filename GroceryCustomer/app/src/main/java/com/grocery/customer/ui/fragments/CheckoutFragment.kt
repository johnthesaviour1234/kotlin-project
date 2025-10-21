package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.UserRepository
import com.grocery.customer.ui.adapters.CheckoutItemAdapter
import com.grocery.customer.ui.viewmodels.CheckoutViewModel
import com.grocery.customer.ui.viewmodels.CartViewModel
import com.grocery.customer.ui.viewmodels.OrderPlacementState
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fragment for checkout process.
 * Handles order summary, delivery address, payment method, and order placement.
 */
@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private val viewModel: CheckoutViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()
    
    @Inject
    lateinit var cartRepository: CartRepository
    
    @Inject
    lateinit var userRepository: UserRepository
    
    // Views
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textSubtotal: TextView
    private lateinit var textTax: TextView
    private lateinit var textDeliveryFee: TextView
    private lateinit var textTotal: TextView
    
    // Address selection views
    private lateinit var buttonSelectSavedAddress: Button
    private lateinit var spinnerSavedAddresses: Spinner
    
    // Address form views
    private lateinit var editTextStreet: EditText
    private lateinit var editTextApartment: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextState: EditText
    private lateinit var editTextPostalCode: EditText
    private lateinit var editTextLandmark: EditText
    
    // Payment and notes views
    private lateinit var radioGroupPayment: RadioGroup
    private lateinit var editTextNotes: EditText
    private lateinit var buttonPlaceOrder: Button
    private lateinit var progressBarOrder: ProgressBar
    
    private lateinit var checkoutItemAdapter: CheckoutItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_checkout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupTextWatchers()
        
        // Set up authentication and load cart data
        setupAndLoadData()
    }

    private fun initializeViews(view: View) {
        recyclerViewItems = view.findViewById(R.id.recyclerViewItems)
        textSubtotal = view.findViewById(R.id.textSubtotal)
        textTax = view.findViewById(R.id.textTax)
        textDeliveryFee = view.findViewById(R.id.textDeliveryFee)
        textTotal = view.findViewById(R.id.textTotal)
        
        buttonSelectSavedAddress = view.findViewById(R.id.buttonSelectSavedAddress)
        spinnerSavedAddresses = view.findViewById(R.id.spinnerSavedAddresses)
        
        editTextStreet = view.findViewById(R.id.editTextStreet)
        editTextApartment = view.findViewById(R.id.editTextApartment)
        editTextCity = view.findViewById(R.id.editTextCity)
        editTextState = view.findViewById(R.id.editTextState)
        editTextPostalCode = view.findViewById(R.id.editTextPostalCode)
        editTextLandmark = view.findViewById(R.id.editTextLandmark)
        
        radioGroupPayment = view.findViewById(R.id.radioGroupPayment)
        editTextNotes = view.findViewById(R.id.editTextNotes)
        buttonPlaceOrder = view.findViewById(R.id.buttonPlaceOrder)
        progressBarOrder = view.findViewById(R.id.progressBarOrder)
    }

    private fun setupRecyclerView() {
        checkoutItemAdapter = CheckoutItemAdapter()
        recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = checkoutItemAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
                
                // Observe order placement state
                launch {
                    viewModel.orderPlacementState.collect { state ->
                        handleOrderPlacementState(state)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        buttonPlaceOrder.setOnClickListener {
            viewModel.placeOrder()
        }
        
        buttonSelectSavedAddress.setOnClickListener {
            toggleSavedAddressSelection()
        }
        
        spinnerSavedAddresses.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Position 0 is "Select an address"
                    loadSavedAddresses() // We'll load the addresses and handle selection
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        radioGroupPayment.setOnCheckedChangeListener { _, checkedId ->
            val paymentMethod = when (checkedId) {
                R.id.radioButtonCash -> "cash"
                R.id.radioButtonCard -> "card"
                R.id.radioButtonUPI -> "upi"
                else -> "cash"
            }
            viewModel.updatePaymentMethod(paymentMethod)
        }
    }

    private fun setupTextWatchers() {
        // Address form text watchers
        val addressTextWatcher = {
            updateDeliveryAddress()
        }
        
        editTextStreet.addTextChangedListener { addressTextWatcher() }
        editTextApartment.addTextChangedListener { addressTextWatcher() }
        editTextCity.addTextChangedListener { addressTextWatcher() }
        editTextState.addTextChangedListener { addressTextWatcher() }
        editTextPostalCode.addTextChangedListener { addressTextWatcher() }
        editTextLandmark.addTextChangedListener { addressTextWatcher() }
        
        // Notes text watcher
        editTextNotes.addTextChangedListener { text ->
            viewModel.updateOrderNotes(text?.toString() ?: "")
        }
    }

    private fun updateDeliveryAddress() {
        val address = DeliveryAddressDTO(
            street = editTextStreet.text.toString().trim(),
            apartment = editTextApartment.text.toString().trim().takeIf { it.isNotEmpty() },
            city = editTextCity.text.toString().trim(),
            state = editTextState.text.toString().trim(),
            postal_code = editTextPostalCode.text.toString().trim(),
            landmark = editTextLandmark.text.toString().trim().takeIf { it.isNotEmpty() }
        )
        viewModel.updateDeliveryAddress(address)
    }

    private fun updateUI(state: com.grocery.customer.ui.viewmodels.CheckoutUiState) {
        // Update order items
        checkoutItemAdapter.submitList(state.cartItems)
        
        // Update totals
        textSubtotal.text = getString(R.string.price_format, state.subtotal)
        textTax.text = getString(R.string.price_format, state.taxAmount)
        textDeliveryFee.text = if (state.deliveryFee > 0) {
            getString(R.string.price_format, state.deliveryFee)
        } else {
            "Free"
        }
        textTotal.text = getString(R.string.price_format, state.totalAmount)
        
        // Update place order button state
        buttonPlaceOrder.isEnabled = state.isDeliveryAddressValid && 
                                      state.cartItems.isNotEmpty() &&
                                      state.selectedPaymentMethod.isNotBlank()
    }

    private fun handleOrderPlacementState(state: OrderPlacementState) {
        when (state) {
            is OrderPlacementState.Idle -> {
                progressBarOrder.visibility = View.GONE
                buttonPlaceOrder.isEnabled = true
            }
            
            is OrderPlacementState.Loading -> {
                progressBarOrder.visibility = View.VISIBLE
                buttonPlaceOrder.isEnabled = false
            }
            
            is OrderPlacementState.Success -> {
                progressBarOrder.visibility = View.GONE
                buttonPlaceOrder.isEnabled = true
                
                // Force refresh cart using CartViewModel for proper UI coordination
                android.util.Log.d("CheckoutFragment", "Order placed successfully. Refreshing cart state.")
                cartViewModel.refreshCart()
                android.util.Log.d("CheckoutFragment", "Cart refresh triggered after successful order")
                
                // Show success message
                Toast.makeText(
                    context, 
                    "Order placed successfully! Order #${state.order.order_number}", 
                    Toast.LENGTH_LONG
                ).show()
                
                // Navigate to order confirmation or home
                findNavController().popBackStack()
                
                // Reset state for next time
                viewModel.resetOrderPlacementState()
            }
            
            is OrderPlacementState.Error -> {
                progressBarOrder.visibility = View.GONE
                buttonPlaceOrder.isEnabled = true
                
                // Show error message
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                
                // Reset state
                viewModel.resetOrderPlacementState()
            }
        }
    }

    /**
     * Load cart data directly
     */
    private fun setupAndLoadData() {
        // Load cart data directly - authentication is handled by the repository layer
        loadCartData()
    }
    
    /**
     * Load real cart data from CartRepository
     */
    private fun loadCartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cart = cartRepository.getCart().first()
                if (!cart.isEmpty()) {
                    viewModel.initializeCheckout(cart.items)
                } else {
                    // Handle empty cart - maybe navigate back or show message
                    Toast.makeText(context, "Your cart is empty", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                // Handle error loading cart
                Toast.makeText(
                    context, 
                    "Error loading cart: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }
    }
    
    private fun toggleSavedAddressSelection() {
        if (spinnerSavedAddresses.visibility == View.VISIBLE) {
            // Hide spinner and show manual entry
            spinnerSavedAddresses.visibility = View.GONE
            showAddressForm(true)
            buttonSelectSavedAddress.text = "Use Saved"
        } else {
            // Show spinner and hide manual entry
            loadSavedAddresses()
        }
    }
    
    private fun showAddressForm(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        editTextStreet.visibility = visibility
        editTextApartment.visibility = visibility
        editTextCity.visibility = visibility
        editTextState.visibility = visibility
        editTextPostalCode.visibility = visibility
        editTextLandmark.visibility = visibility
        
        // Also hide/show the parent TextInputLayouts
        (editTextStreet.parent as View).visibility = visibility
        (editTextApartment.parent as View).visibility = visibility
        (editTextCity.parent.parent as View).visibility = visibility // For the horizontal LinearLayout
        (editTextPostalCode.parent.parent as View).visibility = visibility // For the horizontal LinearLayout
    }
    
    private fun loadSavedAddresses() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = userRepository.getUserAddresses()
                result.fold(
                    onSuccess = { addresses ->
                        if (addresses.isNotEmpty()) {
                            setupAddressSpinner(addresses)
                            spinnerSavedAddresses.visibility = View.VISIBLE
                            showAddressForm(false)
                            buttonSelectSavedAddress.text = "Manual Entry"
                        } else {
                            Toast.makeText(context, "No saved addresses found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { exception ->
                        Toast.makeText(context, "Error loading addresses: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading saved addresses", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupAddressSpinner(addresses: List<UserAddress>) {
        val addressOptions = mutableListOf<String>()
        addressOptions.add("Select an address")
        
        addresses.forEach { address ->
            val displayAddress = "${address.street_address}, ${address.city}, ${address.state} ${address.postal_code}"
            addressOptions.add(displayAddress)
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, addressOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSavedAddresses.adapter = adapter
        
        spinnerSavedAddresses.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedAddress = addresses[position - 1]
                    fillAddressForm(selectedAddress)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun fillAddressForm(address: UserAddress) {
        editTextStreet.setText(address.street_address)
        editTextApartment.setText(address.apartment ?: "")
        editTextCity.setText(address.city)
        editTextState.setText(address.state)
        editTextPostalCode.setText(address.postal_code)
        editTextLandmark.setText(address.landmark ?: "")
        
        // Update the address in view model
        updateDeliveryAddress()
    }
}
