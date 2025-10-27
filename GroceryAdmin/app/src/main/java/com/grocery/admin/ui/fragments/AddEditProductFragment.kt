package com.grocery.admin.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.grocery.admin.R
import com.grocery.admin.data.remote.dto.ProductDto
import com.grocery.admin.databinding.FragmentAddEditProductBinding
import com.grocery.admin.ui.viewmodels.AddEditProductViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for adding new products or editing existing ones.
 * Provides comprehensive form validation, image upload, and category management.
 */
@AndroidEntryPoint
class AddEditProductFragment : Fragment() {

    private var _binding: FragmentAddEditProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditProductViewModel by viewModels()
    // private val args: AddEditProductFragmentArgs by navArgs()
    private var productId: String? = null

    private var selectedImageUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                binding.ivProductImage.setImageURI(uri)
                binding.tvAddImage.text = getString(R.string.change_image)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupImagePicker()
        setupFormValidation()
        setupClickListeners()
        observeViewModel()
        
        // Get product ID from arguments if available
        productId = arguments?.getString("productId")
        
        // Load product data if editing
        productId?.let { id ->
            viewModel.loadProduct(id)
            binding.btnSaveProduct.text = getString(R.string.update_product)
        }
        
        // Load categories
        viewModel.loadCategories()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            // Handle navigation - check if NavController is available
            try {
                findNavController().navigateUp()
            } catch (e: IllegalStateException) {
                // Fallback to fragment manager pop
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        
        val title = if (productId != null) {
            getString(R.string.edit_product)
        } else {
            getString(R.string.add_product)
        }
        binding.toolbar.title = title
    }

    private fun setupImagePicker() {
        binding.layoutAddImage.setOnClickListener {
            openImagePicker()
        }
        
        binding.ivProductImage.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun setupFormValidation() {
        binding.etProductName.addTextChangedListener { validateForm() }
        binding.etProductDescription.addTextChangedListener { validateForm() }
        binding.etProductPrice.addTextChangedListener { validateForm() }
        binding.etProductStock.addTextChangedListener { validateForm() }
        binding.spnCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                validateForm()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveProduct.setOnClickListener {
            if (validateForm()) {
                saveProduct()
            }
        }
        
        binding.switchFeatured.setOnCheckedChangeListener { _, _ ->
            // Optional: Show info about featured products
        }
        
        binding.switchActive.setOnCheckedChangeListener { _, _ ->
            // Optional: Show info about active/inactive products
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val categories = resource.data ?: emptyList()
                    val categoryNames = categories.map { it.name }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spnCategory.adapter = adapter
                }
                is Resource.Error -> {
                    showError(resource.message ?: "Failed to load categories")
                }
                is Resource.Loading -> {
                    // Show loading state if needed
                }
                null -> {}
            }
        }

        viewModel.product.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val product = resource.data
                    product?.let { populateForm(it) }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Failed to load product")
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                null -> {}
            }
        }

        viewModel.saveProductResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSaveProduct.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveProduct.isEnabled = true
                    showSuccess(
                        if (productId != null) "Product updated successfully"
                        else "Product created successfully"
                    )
                    // Navigate back - check if NavController is available
                    try {
                        findNavController().navigateUp()
                    } catch (e: IllegalStateException) {
                        // Fallback to fragment manager pop
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveProduct.isEnabled = true
                    showError(resource.message ?: "Failed to save product")
                    viewModel.resetSaveProductResult()
                }
                null -> {}
            }
        }
    }

    private fun populateForm(product: ProductDto) {
        binding.etProductName.setText(product.name)
        binding.etProductDescription.setText(product.description)
        binding.etProductPrice.setText(product.price.toString())
        binding.etProductStock.setText(product.inventory?.quantity?.toString() ?: "0")
        binding.switchFeatured.isChecked = product.featured
        binding.switchActive.isChecked = product.isActive
        
        // Load product image
        product.imageUrl?.let { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(binding.ivProductImage)
                binding.tvAddImage.text = getString(R.string.change_image)
            }
        }
        
        // Set category selection
        viewModel.categories.value?.let { resource ->
            if (resource is Resource.Success) {
                val categories = resource.data ?: emptyList()
                val categoryIndex = categories.indexOfFirst { it.id == product.categoryId }
                if (categoryIndex >= 0) {
                    binding.spnCategory.setSelection(categoryIndex)
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        
        // Product name validation
        val productName = binding.etProductName.text.toString().trim()
        if (productName.isEmpty()) {
            binding.tilProductName.error = getString(R.string.product_name_required)
            isValid = false
        } else if (productName.length < 3) {
            binding.tilProductName.error = getString(R.string.product_name_min_length)
            isValid = false
        } else {
            binding.tilProductName.error = null
        }
        
        // Product description validation
        val description = binding.etProductDescription.text.toString().trim()
        if (description.isEmpty()) {
            binding.tilProductDescription.error = getString(R.string.product_description_required)
            isValid = false
        } else if (description.length < 10) {
            binding.tilProductDescription.error = getString(R.string.product_description_min_length)
            isValid = false
        } else {
            binding.tilProductDescription.error = null
        }
        
        // Price validation
        val priceText = binding.etProductPrice.text.toString().trim()
        if (priceText.isEmpty()) {
            binding.tilProductPrice.error = getString(R.string.product_price_required)
            isValid = false
        } else {
            try {
                val price = priceText.toDouble()
                if (price <= 0) {
                    binding.tilProductPrice.error = getString(R.string.product_price_positive)
                    isValid = false
                } else {
                    binding.tilProductPrice.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilProductPrice.error = getString(R.string.product_price_invalid)
                isValid = false
            }
        }
        
        // Stock validation
        val stockText = binding.etProductStock.text.toString().trim()
        if (stockText.isEmpty()) {
            binding.tilProductStock.error = getString(R.string.product_stock_required)
            isValid = false
        } else {
            try {
                val stock = stockText.toInt()
                if (stock < 0) {
                    binding.tilProductStock.error = getString(R.string.product_stock_negative)
                    isValid = false
                } else {
                    binding.tilProductStock.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilProductStock.error = getString(R.string.product_stock_invalid)
                isValid = false
            }
        }
        
        // Category validation
        if (binding.spnCategory.selectedItemPosition == -1) {
            showError(getString(R.string.product_category_required))
            isValid = false
        }
        
        binding.btnSaveProduct.isEnabled = isValid
        return isValid
    }

    private fun saveProduct() {
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etProductDescription.text.toString().trim()
        val price = binding.etProductPrice.text.toString().toDouble()
        val stock = binding.etProductStock.text.toString().toInt()
        val isFeatured = binding.switchFeatured.isChecked
        val isActive = binding.switchActive.isChecked
        
        viewModel.categories.value?.let { resource ->
            if (resource is Resource.Success) {
                val categories = resource.data ?: emptyList()
                val selectedCategory = categories.getOrNull(binding.spnCategory.selectedItemPosition)
                selectedCategory?.let { category ->
                    if (productId != null) {
                        // Update existing product
                        viewModel.updateProduct(
                            productId = productId!!,
                            name = name,
                            description = description,
                            price = price,
                            categoryId = category.id,
                            stockQuantity = stock,
                            isFeatured = isFeatured,
                            isActive = isActive,
                            imageUri = selectedImageUri
                        )
                    } else {
                        // Create new product
                        viewModel.createProduct(
                            name = name,
                            description = description,
                            price = price,
                            categoryId = category.id,
                            stockQuantity = stock,
                            isFeatured = isFeatured,
                            isActive = isActive,
                            imageUri = selectedImageUri
                        )
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}