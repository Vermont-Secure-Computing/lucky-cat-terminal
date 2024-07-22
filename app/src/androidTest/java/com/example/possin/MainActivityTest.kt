package com.example.possin

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private lateinit var propertiesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        propertiesFile = File(context.filesDir, "config.properties")
        propertiesFile.delete() // Ensure file is deleted before each test
    }

    @Test
    fun testOnboardingViewShownWhenPropertiesFileDoesNotExist() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.checkbox_bitcoin)).check(matches(isDisplayed()))
            onView(withId(R.id.checkbox_ethereum)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testHomeActivityStartedWhenPropertiesFileExists() {
        // Create a non-empty properties file
        propertiesFile.writeText("cryptocurrencies=Bitcoin,Ethereum")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val expectedIntent = Intent(activity, HomeActivity::class.java)
                val actualIntent = activity.intent
                assertThat(actualIntent.component).isEqualTo(expectedIntent.component)
            }
        }
    }
}