package com.grocery.customer.ui.activities

import android.content.Intent
import com.grocery.customer.BuildConfig
import com.grocery.customer.databinding.ActivitySplashBinding
import com.grocery.customer.ui.viewmodels.AuthViewModel
import com.grocery.customer.ui.viewmodels.Destination
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateViewBinding(): ActivitySplashBinding = ActivitySplashBinding.inflate(layoutInflater)

    override fun setupUI() {
        // Always use proper authentication flow
        viewModel.checkAuth()
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateTo.collect { dest ->
                    when (dest) {
                        Destination.Main -> startActivity(Intent(this@SplashActivity, MainActivity::class.java)).also { finish() }
                        Destination.Login -> startActivity(Intent(this@SplashActivity, LoginActivity::class.java)).also { finish() }
                        else -> Unit
                    }
                }
            }
        }
    }
}