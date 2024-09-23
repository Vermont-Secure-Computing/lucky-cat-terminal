package com.vermont.possin

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vermont.possin.model.ApiResponse
import com.vermont.possin.model.Details
import com.vermont.possin.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties

class APIActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var submitButton: Button
    private lateinit var backArrow: ImageView
    private lateinit var apiDetailsSection: View // Parent View for the API details section
    private lateinit var apiKeyTextView: TextView
    private lateinit var subscriptionLevelTextView: TextView
    private lateinit var activeStatusTextView: TextView
    private lateinit var expiresAtTextView: TextView
    private lateinit var hourlyCallsTextView: TextView
    private lateinit var dailyCallsTextView: TextView
    private lateinit var priceTextView: TextView

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        // Initialize UI elements
        apiKeyInput = findViewById(R.id.api_key_input)
        submitButton = findViewById(R.id.submit_button)
        backArrow = findViewById(R.id.back_arrow)
        apiDetailsSection = findViewById(R.id.api_details_section) // Parent view for all details
        apiKeyTextView = findViewById(R.id.apiKeyTextView)
        subscriptionLevelTextView = findViewById(R.id.subscriptionLevelTextView)
        activeStatusTextView = findViewById(R.id.activeStatusTextView)
        expiresAtTextView = findViewById(R.id.expiresAtTextView)
        hourlyCallsTextView = findViewById(R.id.hourlyCallsTextView)
        dailyCallsTextView = findViewById(R.id.dailyCallsTextView)
        priceTextView = findViewById(R.id.priceTextView)

        // Initially hide the API details section
        hideApiDetails()

        // Initialize Retrofit for API calls
        val retrofit = Retrofit.Builder()
            .baseUrl("https://dogpay.mom/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Load existing API key if it exists and call the API
        loadApiKey()

        submitButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString()
            if (apiKey.isNotEmpty()) {
                saveApiKey(apiKey)
                showSuccessDialog()
            } else {
                Toast.makeText(this, R.string.API_key_cannot_be_empty, Toast.LENGTH_SHORT).show()
            }
        }

        backArrow.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadApiKey() {
        val apiPropertiesFile = File(filesDir, "api.properties")
        if (apiPropertiesFile.exists()) {
            val properties = Properties()
            FileInputStream(apiPropertiesFile).use { input ->
                properties.load(input)
            }
            val apiKey = properties.getProperty("api_key")
            if (!apiKey.isNullOrEmpty()) {
                apiKeyInput.setText(apiKey)
                // Call the API to get details
                callApiDetails(apiKey)
            } else {
                // Hide the API details if API key is empty
                hideApiDetails()
            }
        } else {
            // Hide the API details if the file does not exist
            hideApiDetails()
        }
    }

    private fun callApiDetails(apiKey: String) {
        val call = apiService.getApiDetails(apiKey)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    response.body()?.let {
                        displayApiDetails(it.details) // Show the details only on success
                        showApiDetails()  // Show the details section when successful
                    }
                } else {
                    // If the response is not successful, hide the details and show error
                    hideApiDetails()
                    Toast.makeText(this@APIActivity, R.string.failed_to_get_API_details, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                // On failure, hide the details and show error
                hideApiDetails()
                Toast.makeText(this@APIActivity, "${R.string.error_colon} ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayApiDetails(details: Details) {
        // Update the TextViews with API response
        val apiKeyText = getString(R.string.api_key_colon, details.apiKey)
        apiKeyTextView.text = apiKeyText
        val subscriptionLevel = getString(R.string.subscription_level, details.subscriptionLevel)
        subscriptionLevelTextView.text = subscriptionLevel
        val isActiveEnabled = details.active
        val featureStatusText = getString(
            R.string.active_colon,
            if (isActiveEnabled) "true" else "false"
        )
        activeStatusTextView.text = featureStatusText

        if (details.expiresAt.isNullOrEmpty()) {
            val expiresAt = getString(R.string.expires_at, "No expiration")
            expiresAtTextView.text = expiresAt
        } else {
            // Convert expiresAt to a human-readable format
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputDateFormat.parse(details.expiresAt)
            val expiresAt = getString(R.string.expires_at, outputDateFormat.format(date))
            expiresAtTextView.text = expiresAt
        }
        val hourlyCalls = getString(R.string.hourly_calls, details.hourlyCalls)
        hourlyCallsTextView.text = hourlyCalls
        val dailyCalls = getString(R.string.daily_calls, details.dailyCalls)
        dailyCallsTextView.text = dailyCalls
        val price = getString(R.string.price, details.price)
        priceTextView.text = price
    }

    private fun hideApiDetails() {
        // Hide the entire API details section
        apiDetailsSection.visibility = View.GONE
    }

    private fun showApiDetails() {
        // Show the entire API details section
        apiDetailsSection.visibility = View.VISIBLE
    }

    private fun saveApiKey(apiKey: String) {
        val apiPropertiesFile = File(filesDir, "api.properties")
        val properties = Properties()
        properties.setProperty("api_key", apiKey)

        FileOutputStream(apiPropertiesFile).use { output ->
            properties.store(output, null)
        }
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
            navigateToHome()
        }
        dialog.show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}

