package com.example.possin

import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@Config(sdk = [34]) // Specify the Android SDK version
class XpubAddressTest {

    private lateinit var activity: XpubAddress
    private lateinit var searchField: AutoCompleteTextView
    private lateinit var cryptocurrencyContainer: LinearLayout

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(XpubAddress::class.java)
            .create()
            .resume()
            .get()

        searchField = activity.findViewById(R.id.search_field)
        cryptocurrencyContainer = activity.findViewById(R.id.cryptocurrency_container)
    }

    @Test
    fun testLoadCryptocurrencies() {
        // Check if cryptocurrencies are loaded
        assertTrue(activity.getCryptocurrencies().isNotEmpty())
    }

    @Test
    fun testSearchFunctionality() {
        // Simulate typing "Bitcoin" in the search field
        searchField.setText("Bitcoin")

        // Verify that the filtered list contains only the matching cryptocurrency
        activity.filterCryptocurrencies("Bitcoin")
        activity.populateCryptocurrencyContainer()

        val filteredList = activity.getFilteredCryptocurrencies()
        assertEquals(1, filteredList.size)
        assertEquals("Bitcoin", filteredList[0].name)
    }

    @Test
    fun testDynamicPlaceholder() {
        // Simulate selecting "xpub" from the dropdown for Bitcoin
        searchField.setText("Bitcoin")
        activity.filterCryptocurrencies("Bitcoin")
        activity.populateCryptocurrencyContainer()

        // Find the input field and spinner for Bitcoin
        val itemView = cryptocurrencyContainer.getChildAt(0)
        val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
        val inputField = itemView.findViewById<EditText>(R.id.input_field)

        // Select "xpub" from the spinner
        typeSpinner.setSelection(0)

        // Verify the placeholder text
        assertEquals("Enter xpub Bitcoin name", inputField.hint.toString())
    }

    @Test
    fun testInputFieldVisibility() {
        // Simulate typing in the search field to filter out cryptocurrencies
        searchField.setText("Litecoin")
        activity.filterCryptocurrencies("Litecoin")
        activity.populateCryptocurrencyContainer()

        // Verify that the container is updated with the filtered cryptocurrency
        assertEquals(1, cryptocurrencyContainer.childCount)
        val itemView = cryptocurrencyContainer.getChildAt(0)
        val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
        assertEquals("Litecoin", nameTextView.text.toString())
    }

    @Test
    fun testSaveCryptocurrencyValues() {
        // Simulate input and save
        searchField.setText("Bitcoin")
        activity.filterCryptocurrencies("Bitcoin")
        activity.populateCryptocurrencyContainer()

        val itemView = cryptocurrencyContainer.getChildAt(0)
        val inputField = itemView.findViewById<EditText>(R.id.input_field)
        val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)

        inputField.setText("xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKPVgUks5B9E3v2G96fuk8QoD5mB4xUBmx9Mi2PQf2jBN2ad2dFNEvDJLR8jNg5P7NXw1RhDQieZZ7")
        typeSpinner.setSelection(0) // Assume 0 is "xpub"

        activity.saveCryptocurrencyValues()

        // Verify saved values in properties
        val properties = activity.properties
        val savedType = properties.getProperty("Bitcoin_type")
        val savedValue = properties.getProperty("Bitcoin_value")

        assertEquals("xpub", savedType)
        assertEquals("xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKPVgUks5B9E3v2G96fuk8QoD5mB4xUBmx9Mi2PQf2jBN2ad2dFNEvDJLR8jNg5P7NXw1RhDQieZZ7", savedValue)
    }
}
