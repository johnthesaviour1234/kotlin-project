package com.grocery.customer.ui.activities

import android.widget.Toast
import com.grocery.customer.databinding.ActivityForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>() {

    override fun inflateViewBinding(): ActivityForgotPasswordBinding = ActivityForgotPasswordBinding.inflate(layoutInflater)

    override fun setupUI() {
        binding.buttonSubmit.setOnClickListener {
            // TODO: Implement forgot-password use case when backend endpoint is ready in app repository
            Toast.makeText(this, "Password reset coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun setupObservers() { /* no-op for now */ }
}