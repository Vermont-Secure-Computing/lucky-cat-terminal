package com.example.possin

import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.FileInputStream
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34]) // Specify the Android SDK version
class MerchantActivityTest {

    private lateinit var activity: MerchantActivity
    private lateinit var editTextBusinessName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextState: EditText
    private lateinit var editTextZipCode: EditText
    private lateinit var editTextCountry: EditText
    private lateinit var editTextMerchantPhone: EditText
    private lateinit var editTextMerchantEmail: EditText

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MerchantActivity::class.java)
            .create()
            .resume()
            .get()

        editTextBusinessName = activity.findViewById(R.id.edittext_business_name)
        editTextAddress = activity.findViewById(R.id.edittext_address)
        editTextCity = activity.findViewById(R.id.edittext_city)
        editTextState = activity.findViewById(R.id.edittext_state)
        editTextZipCode = activity.findViewById(R.id.edittext_zip_code)
        editTextCountry = activity.findViewById(R.id.edittext_country)
        editTextMerchantPhone = activity.findViewById(R.id.edittext_merchant_phone)
        editTextMerchantEmail = activity.findViewById(R.id.edittext_merchant_email)
    }

    @Test
    fun testLoadMerchantProperties() {
        // Create a properties file for testing
        val properties = Properties()
        properties.setProperty("merchant_name", "Test Business")
        properties.setProperty("address", "123 Test Street")
        properties.setProperty("city", "Test City")
        properties.setProperty("state", "Test State")
        properties.setProperty("zip_code", "12345")
        properties.setProperty("country", "Test Country")
        properties.setProperty("phone", "1234567890")
        properties.setProperty("email", "test@example.com")

        val file = File(activity.filesDir, "merchant.properties")
        val outputStream = FileOutputStream(file)
        properties.store(outputStream, null)
        outputStream.close()

        // Reload the activity to load properties
        activity.recreate()

        assertEquals("Test Business", editTextBusinessName.text.toString())
        assertEquals("123 Test Street", editTextAddress.text.toString())
        assertEquals("Test City", editTextCity.text.toString())
        assertEquals("Test State", editTextState.text.toString())
        assertEquals("12345", editTextZipCode.text.toString())
        assertEquals("Test Country", editTextCountry.text.toString())
        assertEquals("1234567890", editTextMerchantPhone.text.toString())
        assertEquals("test@example.com", editTextMerchantEmail.text.toString())
    }

    @Test
    fun testSaveMerchantProperties() {
        editTextBusinessName.setText("Test Business")
        editTextAddress.setText("123 Test Street")
        editTextCity.setText("Test City")
        editTextState.setText("Test State")
        editTextZipCode.setText("12345")
        editTextCountry.setText("Test Country")
        editTextMerchantPhone.setText("1234567890")
        editTextMerchantEmail.setText("test@example.com")

        activity.saveMerchantProperties()

        val file = File(activity.filesDir, "merchant.properties")
        assertTrue(file.exists())

        val properties = Properties()
        val inputStream = FileInputStream(file)
        properties.load(inputStream)
        inputStream.close()

        assertEquals("Test Business", properties.getProperty("merchant_name"))
        assertEquals("123 Test Street", properties.getProperty("address"))
        assertEquals("Test City", properties.getProperty("city"))
        assertEquals("Test State", properties.getProperty("state"))
        assertEquals("12345", properties.getProperty("zip_code"))
        assertEquals("Test Country", properties.getProperty("country"))
        assertEquals("1234567890", properties.getProperty("phone"))
        assertEquals("test@example.com", properties.getProperty("email"))
    }

    @Test
    fun testRequiredFieldValidation() {
        editTextBusinessName.setText("")

        // Simulate click on submit button
        val submitText = activity.findViewById<TextView>(R.id.submit_text)
        submitText.performClick()

        // Check if the appropriate toast message is shown
        val toastMessage = ShadowToast.getTextOfLatestToast()
        assertEquals("Business name is required", toastMessage)
    }
}
