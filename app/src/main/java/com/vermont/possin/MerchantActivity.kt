package com.vermont.possin

import android.app.Dialog
import android.content.Context
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

class MerchantActivity : AppCompatActivity() {

    private lateinit var editTextBusinessName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextState: EditText
    private lateinit var editTextZipCode: EditText
    private lateinit var editTextCountry: EditText
    private lateinit var editTextMerchantPhone: EditText
    private lateinit var editTextMerchantEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        // Initialize the EditTexts
        editTextBusinessName = findViewById(R.id.edittext_business_name)
        editTextAddress = findViewById(R.id.edittext_address)
        editTextCity = findViewById(R.id.edittext_city)
        editTextState = findViewById(R.id.edittext_state)
        editTextZipCode = findViewById(R.id.edittext_zip_code)
        editTextCountry = findViewById(R.id.edittext_country)
        editTextMerchantPhone = findViewById(R.id.edittext_merchant_phone)
        editTextMerchantEmail = findViewById(R.id.edittext_merchant_email)

        val setPinTextView = findViewById<TextView>(R.id.setPinTextView)
        // Check if a PIN is already set
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val userPin = sharedPreferences.getString("USER_PIN", null)

        // If a PIN is already set, change the text to "Update Pin"
        if (!userPin.isNullOrEmpty()) {
            setPinTextView.text = getString(R.string.update_pin)
        }

        setPinTextView.setOnClickListener {
            val intent = Intent(this, com.vermont.possin.SetPinActivity::class.java)
            startActivity(intent)
        }

        // Load the properties if they exist
        loadMerchantProperties()

        // Set up the back arrow click listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        // Set up the submit button click listener
        val submitText = findViewById<TextView>(R.id.submit_text)
        submitText.setOnClickListener {
            if (editTextBusinessName.text.toString().isEmpty()) {
                Toast.makeText(this, R.string.business_name_is_required, Toast.LENGTH_SHORT).show()
            } else {
                saveMerchantProperties()
                showSuccessDialog()
            }
        }
    }

    private fun loadMerchantProperties() {
        val file = File(filesDir, "merchant.properties")
        if (file.exists()) {
            val properties = Properties()
            val inputStream = FileInputStream(file)
            properties.load(inputStream)
            inputStream.close()

            editTextBusinessName.setText(properties.getProperty("merchant_name", ""))
            editTextAddress.setText(properties.getProperty("address", ""))
            editTextCity.setText(properties.getProperty("city", ""))
            editTextState.setText(properties.getProperty("state", ""))
            editTextZipCode.setText(properties.getProperty("zip_code", ""))
            editTextCountry.setText(properties.getProperty("country", ""))
            editTextMerchantPhone.setText(properties.getProperty("phone", ""))
            editTextMerchantEmail.setText(properties.getProperty("email", ""))
        }
    }

    private fun saveMerchantProperties() {
        val properties = Properties()
        properties.setProperty("merchant_name", editTextBusinessName.text.toString())
        properties.setProperty("address", editTextAddress.text.toString())
        properties.setProperty("city", editTextCity.text.toString())
        properties.setProperty("state", editTextState.text.toString())
        properties.setProperty("zip_code", editTextZipCode.text.toString())
        properties.setProperty("country", editTextCountry.text.toString())
        properties.setProperty("phone", editTextMerchantPhone.text.toString())
        properties.setProperty("email", editTextMerchantEmail.text.toString())

        val file = File(filesDir, "merchant.properties")
        val fileOutputStream = FileOutputStream(file)
        properties.store(fileOutputStream, "Merchant Properties")
        fileOutputStream.close()
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
