package com.grocery.delivery.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.grocery.delivery.R
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.databinding.DialogOrderDetailBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Bottom sheet dialog showing order details with accept/decline options
 */
class OrderDetailDialog(
    private val assignment: DeliveryAssignment,
    private val onAccept: ((DeliveryAssignment) -> Unit)?,
    private val onDecline: ((String) -> Unit)?
) : BottomSheetDialogFragment() {

    private var _binding: DialogOrderDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        val order = assignment.orders
        
        if (order != null) {
            // Order info
            binding.tvOrderNumber.text = order.orderNumber
            
            // Customer info
            binding.tvCustomerName.text = order.getCustomerName() ?: "N/A"
            binding.tvCustomerPhone.text = order.getCustomerPhone() ?: "N/A"
            
            // Address
            binding.tvAddress.text = order.getFormattedAddress()
            
            // Total amount
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvTotal.text = currencyFormatter.format(order.totalAmount)
            
            // Estimated delivery time
            val estimatedMinutes = assignment.estimatedDeliveryMinutes ?: 30
            binding.tvEstimatedTime.text = "$estimatedMinutes mins"
            
            // Notes
            if (!order.notes.isNullOrBlank()) {
                binding.tvNotesLabel.visibility = View.VISIBLE
                binding.tvNotes.visibility = View.VISIBLE
                binding.tvNotes.text = order.notes
            } else {
                binding.tvNotesLabel.visibility = View.GONE
                binding.tvNotes.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        // Hide action buttons if callbacks are null (e.g., for completed orders)
        if (onAccept == null || onDecline == null) {
            binding.btnAccept.visibility = View.GONE
            binding.btnDecline.visibility = View.GONE
        } else {
            binding.btnAccept.setOnClickListener {
                onAccept.invoke(assignment)
                dismiss()
            }

            binding.btnDecline.setOnClickListener {
                onDecline.invoke(assignment.id)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
