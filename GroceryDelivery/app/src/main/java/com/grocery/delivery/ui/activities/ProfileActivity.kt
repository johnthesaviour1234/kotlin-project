package com.grocery.delivery.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.grocery.delivery.databinding.ActivityProfileBinding
import com.grocery.delivery.ui.viewmodels.ProfileViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Profile Activity - View and edit driver profile
 */
@AndroidEntryPoint
class ProfileActivity : BaseActivity<ActivityProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()
    private var originalName: String = ""
    private var originalPhone: String = ""

    override fun inflateViewBinding(): ActivityProfileBinding {
        return ActivityProfileBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbar()
        setupTextWatchers()
        setupClickListeners()
    }

    override fun setupObservers() {
        // Observe profile data
        viewModel.profileState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showProfileLoading()
                }
                is Resource.Success -> {
                    hideProfileLoading()
                    resource.data?.let { profileResponse ->
                        displayProfileData(profileResponse)
                    }
                }
                is Resource.Error -> {
                    hideProfileLoading()
                    showProfileError(resource.message ?: "Failed to load profile")
                }
                else -> {
                    hideProfileLoading()
                }
            }
        }

        // Observe update state
        viewModel.updateState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showUpdating()
                }
                is Resource.Success -> {
                    hideUpdating()
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    viewModel.clearUpdateState()
                }
                is Resource.Error -> {
                    hideUpdating()
                    Toast.makeText(this, resource.message ?: "Failed to update profile", Toast.LENGTH_LONG).show()
                    viewModel.clearUpdateState()
                }
                else -> {
                    hideUpdating()
                }
            }
        }

        // Observe validation errors
        viewModel.validationErrors.observe(this) { errors ->
            // Clear previous errors
            binding.textInputLayoutName.error = null
            binding.textInputLayoutPhone.error = null

            // Show new errors if any
            errors.fullNameError?.let {
                binding.textInputLayoutName.error = it
            }
            errors.phoneError?.let {
                binding.textInputLayoutPhone.error = it
            }
        }

        // Observe dirty state
        viewModel.isDirty.observe(this) { isDirty ->
            binding.buttonSave.isEnabled = isDirty
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "My Profile"
        }
        binding.toolbar.setNavigationOnClickListener {
            handleBackPress()
        }
    }

    private fun setupTextWatchers() {
        binding.editTextName.doAfterTextChanged {
            if (it.toString() != originalName) {
                viewModel.markDirty()
            }
        }

        binding.editTextPhone.doAfterTextChanged {
            if (it.toString() != originalPhone) {
                viewModel.markDirty()
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            saveProfile()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshProfile()
        }
    }

    private fun displayProfileData(profileResponse: com.grocery.delivery.data.dto.ProfileResponse) {
        binding.swipeRefreshLayout.isRefreshing = false

        val profile = profileResponse.profile
        val stats = profileResponse.stats

        // Profile information
        binding.textViewEmail.text = profile.email
        binding.editTextName.setText(profile.fullName)
        binding.editTextPhone.setText(profile.phone)

        // Save original values
        originalName = profile.fullName
        originalPhone = profile.phone

        // Member since date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(profile.createdAt.substringBefore("."))
            binding.textViewMemberSince.text = "Member since ${outputFormat.format(date ?: Date())}"
        } catch (e: Exception) {
            binding.textViewMemberSince.text = "Member since ${profile.createdAt.substringBefore("T")}"
        }

        // Statistics
        binding.textViewTotalDeliveries.text = stats.totalDeliveries.toString()
        binding.textViewCompletedDeliveries.text = stats.completedDeliveries.toString()
        binding.textViewActiveDeliveries.text = stats.activeDeliveries.toString()

        // Status badge
        if (profile.isActive) {
            binding.textViewStatus.text = "Active"
            binding.textViewStatus.setBackgroundResource(android.R.color.holo_green_light)
        } else {
            binding.textViewStatus.text = "Inactive"
            binding.textViewStatus.setBackgroundResource(android.R.color.holo_red_light)
        }

        // Clear dirty flag after loading
        viewModel.clearDirty()
    }

    private fun saveProfile() {
        val name = binding.editTextName.text.toString()
        val phone = binding.editTextPhone.text.toString()

        viewModel.updateProfile(name, phone)
    }

    private fun showProfileLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun hideProfileLoading() {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showProfileError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.textViewError.text = message
        binding.buttonRetry.setOnClickListener {
            viewModel.refreshProfile()
        }
    }

    private fun showUpdating() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Saving..."
    }

    private fun hideUpdating() {
        binding.buttonSave.isEnabled = viewModel.isDirty.value == true
        binding.buttonSave.text = "Save Changes"
    }

    private fun handleBackPress() {
        if (viewModel.isDirty.value == true) {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Leave") { _, _ ->
                    finish()
                }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        handleBackPress()
    }
}
