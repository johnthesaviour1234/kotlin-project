package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.grocery.customer.R
import com.grocery.customer.databinding.FragmentProductDetailBinding
import com.grocery.customer.ui.viewmodels.ProductDetailViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Product Detail Fragment - Displays detailed information about a product
 */
@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()

    private val viewModel: ProductDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        setupQuantityControls()
        setupAddToCartButton()
    }

    private fun setupQuantityControls() {
        binding.buttonDecreaseQuantity.setOnClickListener {
            viewModel.decreaseQuantity()
        }

        binding.buttonIncreaseQuantity.setOnClickListener {
            viewModel.increaseQuantity()
        }
    }

    private fun setupAddToCartButton() {
        binding.buttonAddToCart.setOnClickListener {
            if (viewModel.isInStock() && viewModel.isQuantityValid()) {
                viewModel.addToCart()
            } else {
                Toast.makeText(context, "Product is out of stock", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.productDetail.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                    binding.textViewError.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                    binding.textViewError.visibility = View.GONE
                    
                    resource.data?.let { product ->
                        displayProductDetails(product)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.GONE
                    binding.textViewError.visibility = View.VISIBLE
                    binding.textViewError.text = resource.message
                }
            }
        }

        viewModel.quantity.observe(viewLifecycleOwner) { quantity ->
            binding.textViewQuantity.text = quantity.toString()
            updateAddToCartButton()
        }

        viewModel.addToCartState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonAddToCart.isEnabled = false
                    binding.buttonAddToCart.text = "Adding..."
                }
                is Resource.Success -> {
                    binding.buttonAddToCart.isEnabled = true
                    binding.buttonAddToCart.text = "Add to Cart"
                    Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAddToCartState()
                }
                is Resource.Error -> {
                    binding.buttonAddToCart.isEnabled = true
                    binding.buttonAddToCart.text = "Add to Cart"
                    Toast.makeText(context, "Failed to add to cart: ${resource.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetAddToCartState()
                }
                null -> {
                    // Reset state
                    updateAddToCartButton()
                }
            }
        }
    }

    private fun displayProductDetails(product: com.grocery.customer.data.remote.dto.ProductDetail) {
        binding.apply {
            textViewProductName.text = product.name
            textViewProductPrice.text = "₹${product.price}"
            textViewProductDescription.text = product.description ?: "No description available"
            
            // Display stock information
            val stock = product.inventory?.stock ?: 0
            textViewStockInfo.text = if (stock > 0) {
                "In stock: $stock items"
            } else {
                "Out of stock"
            }
            
            // Load product image using Glide
            Glide.with(imageViewProduct.context)
                .load(product.image_url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(imageViewProduct)
        }
        
        updateAddToCartButton()
    }

    private fun updateAddToCartButton() {
        val inStock = viewModel.isInStock()
        val quantityValid = viewModel.isQuantityValid()
        
        binding.buttonAddToCart.isEnabled = inStock && quantityValid
        binding.buttonAddToCart.text = if (inStock) {
            "Add to Cart"
        } else {
            "Out of Stock"
        }
        
        // Enable/disable quantity controls
        binding.buttonIncreaseQuantity.isEnabled = inStock
        binding.buttonDecreaseQuantity.isEnabled = inStock && (viewModel.quantity.value ?: 1) > 1
    }

    private fun loadData() {
        val productId = args.productId
        viewModel.loadProductDetail(productId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
