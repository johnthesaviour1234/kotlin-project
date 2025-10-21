package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grocery.customer.databinding.FragmentCartBinding
import com.grocery.customer.ui.adapters.CartAdapter
import com.grocery.customer.ui.viewmodels.CartViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Cart Fragment - Displays items in the shopping cart
 */
@AndroidEntryPoint
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh cart data when fragment becomes visible
        // This ensures the cart is up to date if it was cleared from another screen
        viewModel.refreshCart()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupCheckoutButton()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { cartItemId, quantity ->
                viewModel.updateCartItemQuantity(cartItemId, quantity)
            },
            onRemoveItem = { cartItemId ->
                viewModel.removeCartItem(cartItemId)
            }
        )
        
        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
        }
    }

    private fun setupCheckoutButton() {
        binding.buttonCheckout.setOnClickListener {
            val cart = viewModel.cart.value
            if (cart != null && !cart.isEmpty()) {
                // Navigate to checkout with cart data
                // TODO: Pass cart items as navigation arguments using Safe Args
                navigateToCheckout()
            } else {
                Toast.makeText(context, "Your cart is empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToCheckout() {
        try {
            // For now, navigate without arguments. 
            // TODO: Implement Safe Args to pass cart data
            findNavController().navigate(com.grocery.customer.R.id.action_cartFragment_to_checkoutFragment)
        } catch (e: Exception) {
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe cart data
        viewModel.cart.observe(viewLifecycleOwner) { cart ->
            android.util.Log.d("CartFragment", "Cart updated: ${cart.totalItems} items, ${cart.items.size} unique items")
            if (cart.isEmpty()) {
                android.util.Log.d("CartFragment", "Showing empty cart state")
                showEmptyCart()
            } else {
                android.util.Log.d("CartFragment", "Showing cart items: ${cart.totalItems} total items")
                showCartItems(cart)
            }
        }

        // Observe cart update operations
        viewModel.updateCartState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    viewModel.resetUpdateCartState()
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdateCartState()
                }
                null -> {
                    // Reset state
                }
            }
        }

        // Observe remove item operations
        viewModel.removeItemState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show()
                    viewModel.resetRemoveItemState()
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetRemoveItemState()
                }
                null -> {
                    // Reset state
                }
            }
        }
    }

    private fun showEmptyCart() {
        binding.apply {
            textViewEmptyCart.visibility = View.VISIBLE
            recyclerViewCart.visibility = View.GONE
            cardViewCheckout.visibility = View.GONE
        }
    }

    private fun showCartItems(cart: com.grocery.customer.data.remote.dto.Cart) {
        binding.apply {
            textViewEmptyCart.visibility = View.GONE
            recyclerViewCart.visibility = View.VISIBLE
            cardViewCheckout.visibility = View.VISIBLE
            
            // Update cart summary
            textViewTotalItems.text = "${cart.totalItems} items"
            textViewTotalPrice.text = "₹${String.format("%.2f", cart.totalPrice)}"
        }
        
        // Update adapter
        cartAdapter.submitList(cart.items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
