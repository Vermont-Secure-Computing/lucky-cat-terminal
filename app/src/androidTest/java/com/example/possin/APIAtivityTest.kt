package com.example.possin

import android.content.Context
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Properties

@RunWith(AndroidJUnit4::class)
class APIActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(APIActivity::class.java)

    private lateinit var context: Context
    private lateinit var apiPropertiesFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        apiPropertiesFile = File(context.filesDir, "api.properties")
        // Clear the properties file before each test
        if (apiPropertiesFile.exists()) {
            apiPropertiesFile.delete()
        }
    }

    @After
    fun tearDown() {
        // Clean up after tests
        if (apiPropertiesFile.exists()) {
            apiPropertiesFile.delete()
        }
    }

    @Test
    fun testSaveApiKey() {
        val testApiKey = "test_api_key"

        // Launch the activity
        ActivityScenario.launch(APIActivity::class.java)

        // Enter the API key
        onView(withId(R.id.api_key_input)).perform(replaceText(testApiKey))

        // Click the submit button
        onView(withId(R.id.submit_text)).perform(click())

        // Re-launch the activity to see if the API key is saved and loaded correctly
        ActivityScenario.launch(APIActivity::class.java)

        // Verify the API key is loaded correctly
        onView(withId(R.id.api_key_input)).check(matches(withText(testApiKey)))
    }

    @Test
    fun testLoadApiKey() {
        val testApiKey = "test_api_key"

        // Create a properties file with the test API key
        val properties = Properties()
        properties.setProperty("api_key", testApiKey)
        apiPropertiesFile.outputStream().use {
            properties.store(it, null)
        }

        // Launch the activity
        ActivityScenario.launch(APIActivity::class.java)

        // Verify the API key is loaded correctly
        onView(withId(R.id.api_key_input)).check(matches(withText(testApiKey)))
    }



    fun testEmptyApiKey() {
        // Launch the activity
        ActivityScenario.launch(APIActivity::class.java)

        // Verify the API key input is empty
        onView(withId(R.id.api_key_input)).perform(replaceText(""))
        println("Checking if EditText is empty after first replaceText(\"\")")
        onView(withId(R.id.api_key_input)).check(matches(CustomMatchers.withEmptyText()))

        // Enter an API key
        val testApiKey = "test_api_key"
        onView(withId(R.id.api_key_input)).perform(replaceText(testApiKey))
        println("Entered API key: $testApiKey")

        // Click the submit button
        onView(withId(R.id.submit_text)).perform(click())

        // Verify if the API key is still in the EditText after submission
        println("Checking if EditText still contains API key after submission")
        onView(withId(R.id.api_key_input)).check { view, _ ->
            val editText = view as EditText
            println("EditText content after submission: '${editText.text}'")
        }

        // Clear the input field
        onView(withId(R.id.api_key_input)).perform(replaceText(""))
        println("Checking if EditText is empty after second replaceText(\"\")")

        // Intermediate check to ensure the field is empty after clearing it
        onView(withId(R.id.api_key_input)).check(matches(CustomMatchers.withEmptyText()))

        // Re-launch the activity to see if the empty input is handled correctly
        ActivityScenario.launch(APIActivity::class.java)

        // Final check to verify the API key input is still empty
        println("Checking if EditText is empty after re-launching the activity")
        onView(withId(R.id.api_key_input)).check(matches(CustomMatchers.withEmptyText()))
    }
}

