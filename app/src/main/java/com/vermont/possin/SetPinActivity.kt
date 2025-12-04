/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vermont.possin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SetPinActivity : AppCompatActivity() {

    private lateinit var pinCircles: List<View>
    private lateinit var instructionTextView: TextView

    private var enteredPin: StringBuilder = StringBuilder()
    private var firstPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        pinCircles = listOf(
            findViewById(R.id.pinCircle1),
            findViewById(R.id.pinCircle2),
            findViewById(R.id.pinCircle3),
            findViewById(R.id.pinCircle4)
        )

        instructionTextView = findViewById(com.vermont.possin.R.id.instructionTextView)
        val backArrow = findViewById<ImageView>(com.vermont.possin.R.id.back_arrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updatePinCircles() {
        for (i in pinCircles.indices) {
            if (i < enteredPin.length) {
                pinCircles[i].setBackgroundColor(ContextCompat.getColor(this,
                    R.color.tapeRed
                ))
            } else {
                pinCircles[i].setBackgroundColor(ContextCompat.getColor(this,
                    R.color.grey
                ))
            }
        }
    }

    fun onNumberClick(view: View) {
        if (enteredPin.length < 4) {
            val button = view as Button
            enteredPin.append(button.text)
            updatePinCircles()

            if (enteredPin.length == 4) {
                if (firstPin == null) {
                    // First pin entry complete, save it and prompt for confirmation
                    firstPin = enteredPin.toString()
                    enteredPin.clear()
                    updatePinCircles()
                    instructionTextView.text = "Confirm Your Pin"
                } else {
                    // Confirm pin entry
                    if (enteredPin.toString() == firstPin) {
                        savePin(firstPin!!)
                    } else {
                        Toast.makeText(this, "Pins do not match. Please try again.", Toast.LENGTH_SHORT).show()
                        enteredPin.clear()
                        updatePinCircles()
                        instructionTextView.text = "Enter Your Pin"
                        firstPin = null
                    }
                }
            }
        }
    }

    fun onBackspaceClick(view: View) {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updatePinCircles()
        }
    }

    private fun savePin(pin: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("USER_PIN", pin)
            apply()
        }
        Toast.makeText(this, "Pin Saved", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, com.vermont.possin.HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
