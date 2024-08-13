package com.example.possin

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class APIActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var submitTextView: TextView
    private lateinit var backArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        apiKeyInput = findViewById(R.id.api_key_input)
        submitTextView = findViewById(R.id.submit_text)
        backArrow = findViewById(R.id.back_arrow)

        // Load existing API key if it exists
        loadApiKey()

        submitTextView.setOnClickListener {
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
            }
        }
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
