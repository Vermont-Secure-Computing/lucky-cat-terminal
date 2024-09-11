package com.example.possin

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
import com.example.possin.model.ApiResponse
import com.example.possin.model.Details
import com.example.possin.network.ApiService
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
        apiKeyTextView = findViewById(R.id.apiKeyTextView)
        subscriptionLevelTextView = findViewById(R.id.subscriptionLevelTextView)
        activeStatusTextView = findViewById(R.id.activeStatusTextView)
        expiresAtTextView = findViewById(R.id.expiresAtTextView)
        hourlyCallsTextView = findViewById(R.id.hourlyCallsTextView)
        dailyCallsTextView = findViewById(R.id.dailyCallsTextView)
        priceTextView = findViewById(R.id.priceTextView)

        // Initially hide the API details views
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
                Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
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
                // Show API details section since the API key is not null or empty
                showApiDetails()
                // Call the API to get details
                callApiDetails(apiKey)
            }
        }
    }

    private fun callApiDetails(apiKey: String) {
        val call = apiService.getApiDetails(apiKey)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                println(response.body())
                if (response.isSuccessful) {
                    response.body()?.let {
                        displayApiDetails(it.details)
                    }
                } else {
                    Toast.makeText(this@APIActivity, "Failed to get API details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@APIActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayApiDetails(details: Details) {
        // Update the TextViews with API response
        apiKeyTextView.setText("API key: ${details.apiKey}")
        subscriptionLevelTextView.setText("Subscription Level: ${details.subscriptionLevel}")
        activeStatusTextView.setText("Active: ${details.active}")

        if (details.expiresAt.isNullOrEmpty()) {
            expiresAtTextView.setText("Expires At: No expiration")
        } else {
            // Convert expiresAt to a human-readable format
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputDateFormat.parse(details.expiresAt)
            expiresAtTextView.setText("Expires At: ${outputDateFormat.format(date)}")
        }

        hourlyCallsTextView.setText("Hourly Calls: ${details.hourlyCalls}")
        dailyCallsTextView.setText("Daily Calls: ${details.dailyCalls}")
        priceTextView.setText("Price: ${details.price}")
    }

    private fun hideApiDetails() {
        // Initially hide the API details section
        apiKeyTextView.visibility = View.GONE
        subscriptionLevelTextView.visibility = View.GONE
        activeStatusTextView.visibility = View.GONE
        expiresAtTextView.visibility = View.GONE
        hourlyCallsTextView.visibility = View.GONE
        dailyCallsTextView.visibility = View.GONE
        priceTextView.visibility = View.GONE
    }

    private fun showApiDetails() {
        // Show the API details section when the API key is loaded
        apiKeyTextView.visibility = View.VISIBLE
        subscriptionLevelTextView.visibility = View.VISIBLE
        activeStatusTextView.visibility = View.VISIBLE
        expiresAtTextView.visibility = View.VISIBLE
        hourlyCallsTextView.visibility = View.VISIBLE
        dailyCallsTextView.visibility = View.VISIBLE
        priceTextView.visibility = View.VISIBLE
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
