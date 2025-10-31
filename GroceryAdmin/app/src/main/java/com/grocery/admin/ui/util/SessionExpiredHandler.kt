package com.grocery.admin.ui.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grocery.admin.ui.activities.LoginActivity

/**
 * Utility class to handle session expiration across the app
 * Shows a dialog and redirects to login screen
 */
object SessionExpiredHandler {
    
    /**
     * Show session expired dialog and navigate to login
     * @param context The current context (Activity)
     */
    fun showSessionExpiredDialog(context: Context) {
        if (context !is AppCompatActivity) return
        
        // Don't show dialog if activity is finishing
        if (context.isFinishing) return
        
        AlertDialog.Builder(context)
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please login again.")
            .setPositiveButton("Login") { _, _ ->
                navigateToLogin(context)
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Navigate to login activity and clear activity stack
     */
    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        
        if (context is AppCompatActivity) {
            context.finish()
        }
    }
}
