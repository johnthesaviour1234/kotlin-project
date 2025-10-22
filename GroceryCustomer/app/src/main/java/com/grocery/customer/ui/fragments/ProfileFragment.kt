package com.grocery.customer.ui.fragments

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import com.grocery.customer.R
import com.grocery.customer.data.remote.dto.ChangePasswordRequest
import com.grocery.customer.data.remote.dto.CreateAddressRequest
import com.grocery.customer.data.remote.dto.ProfileUpdateRequest
import com.grocery.customer.data.remote.dto.UpdateAddressRequest
import com.grocery.customer.data.remote.dto.UserAddress
import com.grocery.customer.databinding.FragmentProfileBinding
import com.grocery.customer.ui.activities.LoginActivity
import com.grocery.customer.ui.adapters.AddressAdapter
import com.grocery.customer.ui.viewmodels.ProfileViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Profile Fragment - Displays user profile and settings
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var addressAdapter: AddressAdapter

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(
            onEditClick = { address -> showEditAddressDialog(address) },
            onDeleteClick = { address -> showDeleteConfirmation(address) },
            onAddressClick = { address -> Log.d(TAG, "Address clicked: ${address.name}") }
        )

        binding.recyclerViewAddresses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addressAdapter
        }
    }

    private fun setupClickListeners() {
        // Edit profile button
        binding.buttonEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Add address button
        binding.buttonAddAddress.setOnClickListener {
            showAddAddressDialog()
        }

        // Change password
        binding.layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Notifications toggle
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference(isChecked)
        }

        // Theme settings
        binding.layoutTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        // Logout
        binding.layoutLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupObservers() {
        // Observe user profile
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG, "User profile resource state: ${resource::class.simpleName}")
            when (resource) {
                is Resource.Loading -> {
                    Log.d(TAG, "Loading user profile...")
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    Log.d(TAG, "Successfully loaded user profile")
                    binding.progressBar.visibility = View.GONE
                    val profile = resource.data
                    
                    // Update UI with profile data
                    binding.textViewUserName.text = profile?.full_name ?: "User"
                    binding.textViewUserEmail.text = profile?.email ?: "No email"
                    
                    if (!profile?.phone.isNullOrBlank()) {
                        binding.textViewUserPhone.text = profile?.phone
                        binding.textViewUserPhone.visibility = View.VISIBLE
                    } else {
                        binding.textViewUserPhone.visibility = View.GONE
                    }

                    // Load avatar if available
                    if (!profile?.avatar_url.isNullOrBlank()) {
                        Glide.with(this)
                            .load(profile?.avatar_url)
                            .placeholder(R.drawable.ic_profile)
                            .circleCrop()
                            .into(binding.imageViewAvatar)
                    }

                    // Update notification preference
                    val preferences = profile?.preferences ?: emptyMap()
                    binding.switchNotifications.isChecked = preferences["notifications"] as? Boolean ?: true
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading user profile: ${resource.message}")
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error loading profile: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe addresses
        viewModel.userAddresses.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG, "User addresses resource state: ${resource::class.simpleName}")
            when (resource) {
                is Resource.Loading -> {
                    Log.d(TAG, "Loading user addresses...")
                }
                is Resource.Success -> {
                    Log.d(TAG, "Successfully loaded ${resource.data?.size ?: 0} addresses")
                    val addresses = resource.data ?: emptyList()
                    addressAdapter.submitList(addresses)
                    
                    if (addresses.isEmpty()) {
                        binding.recyclerViewAddresses.visibility = View.GONE
                        binding.textViewNoAddresses.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewAddresses.visibility = View.VISIBLE
                        binding.textViewNoAddresses.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading addresses: ${resource.message}")
                    Toast.makeText(context, "Error loading addresses: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe profile update result
        viewModel.profileUpdateResult.observe(viewLifecycleOwner) { resource ->
            resource?.let {
                when (it) {
                    is Resource.Loading -> {
                        // Show loading if needed
                    }
                    is Resource.Success -> {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                    is Resource.Error -> {
                        Toast.makeText(context, "Error updating profile: ${it.message}", Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                }
            }
        }

        // Observe password change result
        viewModel.passwordChangeResult.observe(viewLifecycleOwner) { resource ->
            resource?.let {
                when (it) {
                    is Resource.Loading -> {
                        // Show loading if needed
                    }
                    is Resource.Success -> {
                        Toast.makeText(context, it.data, Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                    is Resource.Error -> {
                        Toast.makeText(context, "Error changing password: ${it.message}", Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                }
            }
        }

        // Observe address operation result
        viewModel.addressOperationResult.observe(viewLifecycleOwner) { resource ->
            resource?.let {
                when (it) {
                    is Resource.Loading -> {
                        // Show loading if needed
                    }
                    is Resource.Success -> {
                        Toast.makeText(context, "Address updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                    is Resource.Error -> {
                        Toast.makeText(context, "Error with address: ${it.message}", Toast.LENGTH_SHORT).show()
                        viewModel.clearResults()
                    }
                }
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .create()

        val currentProfile = (viewModel.userProfile.value as? Resource.Success)?.data
        
        // Initialize fields with current data
        dialogView.findViewById<TextInputEditText>(R.id.editTextFullName).setText(currentProfile?.full_name)
        dialogView.findViewById<TextInputEditText>(R.id.editTextPhone).setText(currentProfile?.phone)
        dialogView.findViewById<TextInputEditText>(R.id.editTextDateOfBirth).setText(currentProfile?.date_of_birth)
        dialogView.findViewById<TextInputEditText>(R.id.editTextAvatarUrl).setText(currentProfile?.avatar_url)
        
        // Setup date picker for date of birth
        val dateField = dialogView.findViewById<TextInputEditText>(R.id.editTextDateOfBirth)
        dateField.setOnClickListener {
            showDatePicker { selectedDate ->
                dateField.setText(selectedDate)
            }
        }
        
        // Cancel button
        dialogView.findViewById<MaterialButton>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        // Save button
        dialogView.findViewById<MaterialButton>(R.id.buttonSave).setOnClickListener {
            val fullName = dialogView.findViewById<TextInputEditText>(R.id.editTextFullName).text.toString().trim()
            val phone = dialogView.findViewById<TextInputEditText>(R.id.editTextPhone).text.toString().trim()
            val dateOfBirth = dialogView.findViewById<TextInputEditText>(R.id.editTextDateOfBirth).text.toString().trim()
            val avatarUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextAvatarUrl).text.toString().trim()
            
            // Validate required fields
            if (fullName.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutFullName).error = "Full name is required"
                return@setOnClickListener
            }
            
            // Clear errors
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutFullName).error = null
            
            val updateRequest = ProfileUpdateRequest(
                full_name = fullName.ifEmpty { null },
                phone = phone.ifEmpty { null },
                date_of_birth = dateOfBirth.ifEmpty { null },
                avatar_url = avatarUrl.ifEmpty { null }
            )
            
            viewModel.updateProfile(updateRequest)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showAddAddressDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_address, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Address")
            .setView(dialogView)
            .create()

        setupAddressDialog(dialogView, dialog, null)
        dialog.show()
    }

    private fun showEditAddressDialog(address: UserAddress) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_address, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Address")
            .setView(dialogView)
            .create()

        setupAddressDialog(dialogView, dialog, address)
        dialog.show()
    }

    private fun showDeleteConfirmation(address: UserAddress) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Address")
        builder.setMessage("Are you sure you want to delete '${address.name}'?")
        builder.setPositiveButton("Delete") { dialog, _ ->
            viewModel.deleteAddress(address.id)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .create()

        // Cancel button
        dialogView.findViewById<MaterialButton>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        // Change Password button
        dialogView.findViewById<MaterialButton>(R.id.buttonChangePassword).setOnClickListener {
            val currentPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextCurrentPassword).text.toString().trim()
            val newPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextNewPassword).text.toString().trim()
            val confirmPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextConfirmPassword).text.toString().trim()
            
            // Clear previous errors
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCurrentPassword).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutNewPassword).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword).error = null
            
            var hasError = false
            
            // Validate current password
            if (currentPassword.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCurrentPassword).error = "Current password is required"
                hasError = true
            }
            
            // Validate new password
            if (newPassword.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutNewPassword).error = "New password is required"
                hasError = true
            } else if (newPassword.length < 8) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutNewPassword).error = "Password must be at least 8 characters"
                hasError = true
            }
            
            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword).error = "Please confirm your new password"
                hasError = true
            } else if (newPassword != confirmPassword) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword).error = "Passwords do not match"
                hasError = true
            }
            
            if (!hasError) {
                viewModel.changePassword(currentPassword, newPassword)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Light", "Dark", "System Default")
        val currentTheme = 0 // TODO: Get from preferences
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(themes, currentTheme) { dialog, which ->
            // TODO: Update theme preference
            Toast.makeText(context, "Theme changed to: ${themes[which]}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun updateNotificationPreference(enabled: Boolean) {
        val currentProfile = (viewModel.userProfile.value as? Resource.Success)?.data
        currentProfile?.let {
            val updatedPreferences = (it.preferences ?: emptyMap()).toMutableMap()
            updatedPreferences["notifications"] = enabled
            
            val updateRequest = ProfileUpdateRequest(
                preferences = updatedPreferences
            )
            viewModel.updateProfile(updateRequest)
        }
    }

    private fun showLogoutConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Logout") { dialog, _ ->
            viewModel.logout()
            Log.d(TAG, "User logged out")
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            
            // Navigate to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun setupAddressDialog(dialogView: View, dialog: AlertDialog, existingAddress: UserAddress?) {
        // Pre-fill fields if editing existing address
        existingAddress?.let { address ->
            dialogView.findViewById<TextInputEditText>(R.id.editTextAddressName).setText(address.name)
            dialogView.findViewById<TextInputEditText>(R.id.editTextStreetAddress).setText(address.street_address)
            dialogView.findViewById<TextInputEditText>(R.id.editTextApartment).setText(address.apartment)
            dialogView.findViewById<TextInputEditText>(R.id.editTextCity).setText(address.city)
            dialogView.findViewById<TextInputEditText>(R.id.editTextState).setText(address.state)
            dialogView.findViewById<TextInputEditText>(R.id.editTextPostalCode).setText(address.postal_code)
            dialogView.findViewById<TextInputEditText>(R.id.editTextLandmark).setText(address.landmark)
            dialogView.findViewById<MaterialSwitch>(R.id.switchDefaultAddress).isChecked = address.is_default
            
            // Set address type
            when (address.address_type) {
                "home" -> dialogView.findViewById<Chip>(R.id.chipHome).isChecked = true
                "work" -> dialogView.findViewById<Chip>(R.id.chipWork).isChecked = true
                "other" -> dialogView.findViewById<Chip>(R.id.chipOther).isChecked = true
            }
        }
        
        // Cancel button
        dialogView.findViewById<MaterialButton>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        // Save button
        dialogView.findViewById<MaterialButton>(R.id.buttonSave).setOnClickListener {
            val name = dialogView.findViewById<TextInputEditText>(R.id.editTextAddressName).text.toString().trim()
            val streetAddress = dialogView.findViewById<TextInputEditText>(R.id.editTextStreetAddress).text.toString().trim()
            val apartment = dialogView.findViewById<TextInputEditText>(R.id.editTextApartment).text.toString().trim()
            val city = dialogView.findViewById<TextInputEditText>(R.id.editTextCity).text.toString().trim()
            val state = dialogView.findViewById<TextInputEditText>(R.id.editTextState).text.toString().trim()
            val postalCode = dialogView.findViewById<TextInputEditText>(R.id.editTextPostalCode).text.toString().trim()
            val landmark = dialogView.findViewById<TextInputEditText>(R.id.editTextLandmark).text.toString().trim()
            val isDefault = dialogView.findViewById<MaterialSwitch>(R.id.switchDefaultAddress).isChecked
            
            // Get selected address type
            val addressType = when {
                dialogView.findViewById<Chip>(R.id.chipHome).isChecked -> "home"
                dialogView.findViewById<Chip>(R.id.chipWork).isChecked -> "work"
                dialogView.findViewById<Chip>(R.id.chipOther).isChecked -> "other"
                else -> "home"
            }
            
            // Clear previous errors
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutAddressName).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutStreetAddress).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCity).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutState).error = null
            dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutPostalCode).error = null
            
            var hasError = false
            
            // Validate required fields
            if (name.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutAddressName).error = "Address name is required"
                hasError = true
            }
            
            if (streetAddress.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutStreetAddress).error = "Street address is required"
                hasError = true
            }
            
            if (city.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCity).error = "City is required"
                hasError = true
            }
            
            if (state.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutState).error = "State is required"
                hasError = true
            }
            
            if (postalCode.isEmpty()) {
                dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutPostalCode).error = "Postal code is required"
                hasError = true
            }
            
            if (!hasError) {
                if (existingAddress != null) {
                    // Update existing address
                    val updateRequest = UpdateAddressRequest(
                        name = name,
                        street_address = streetAddress,
                        apartment = apartment.ifEmpty { null },
                        city = city,
                        state = state,
                        postal_code = postalCode,
                        landmark = landmark.ifEmpty { null },
                        address_type = addressType,
                        is_default = isDefault
                    )
                    viewModel.updateAddress(existingAddress.id, updateRequest)
                } else {
                    // Create new address
                    val createRequest = CreateAddressRequest(
                        name = name,
                        street_address = streetAddress,
                        apartment = apartment.ifEmpty { null },
                        city = city,
                        state = state,
                        postal_code = postalCode,
                        landmark = landmark.ifEmpty { null },
                        address_type = addressType,
                        is_default = isDefault
                    )
                    viewModel.createAddress(createRequest)
                }
                dialog.dismiss()
            }
        }
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(selectedDate)
            },
            year,
            month,
            day
        )
        
        // Set max date to today (can't select future dates for birth date)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
