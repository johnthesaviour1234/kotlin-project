package com.grocery.customer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.grocery.customer.databinding.FragmentProductDetailBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Product Detail Fragment - Displays detailed information about a product
 */
@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupUI() {
        // TODO: Implement product detail functionality
        val productId = arguments?.getString("productId") ?: "unknown"
        binding.textViewProductName.text = "Product Details"
        binding.textViewProductDescription.text = "Loading product $productId..."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}