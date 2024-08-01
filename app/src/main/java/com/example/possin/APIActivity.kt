package com.example.possin

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
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

        submitTextView.setOnClickListener {
            val apiKey = apiKeyInput.text.toString()
            if (apiKey.isNotEmpty()) {
                saveApiKey(apiKey)
            }
        }

        backArrow.setOnClickListener {
            onBackPressed()
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
}
