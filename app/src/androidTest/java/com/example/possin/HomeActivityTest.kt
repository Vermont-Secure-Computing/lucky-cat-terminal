package com.example.possin

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Properties

@RunWith(AndroidJUnit4::class)
class HomeActivityTest {

    private lateinit var merchantPropertiesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        merchantPropertiesFile = File(context.filesDir, "merchant.properties")
        merchantPropertiesFile.delete() // Ensure file is deleted before each test
    }

    @Test
    fun testMerchantNameDisplayedWhenPropertiesFileExists() {
        val properties = Properties().apply {
            setProperty("merchant_name", "Test Merchant")
        }
        merchantPropertiesFile.outputStream().use {
            properties.store(it, null)
        }

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.header_name)).check(matches(withText("Good morning, Test Merchant!")))
        }
    }

    @Test
    fun testDefaultGreetingWhenPropertiesFileDoesNotExist() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.header_name)).check(matches(withText("Good morning!")))
        }
    }

    @Test
    fun testPOSActivityStartedOnButtonClick() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.button1)).perform(click())
            val expectedIntent = Intent(ApplicationProvider.getApplicationContext(), POSActivity::class.java)
            assertThat(it.result.resultData.component).isEqualTo(expectedIntent.component)
        }
    }
}