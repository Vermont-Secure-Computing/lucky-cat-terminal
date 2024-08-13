package com.example.possin

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomeActivityTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testButtonsExist() {
        // Launch HomeActivity
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // Check if the buttons are displayed
        onView(withId(R.id.button1)).check(matches(isDisplayed()))
        onView(withId(R.id.button2)).check(matches(isDisplayed()))
//        onView(withId(R.id.button3)).check(matches(isDisplayed()))
        onView(withId(R.id.button4)).check(matches(isDisplayed()))
        onView(withId(R.id.button5)).check(matches(isDisplayed()))
        onView(withId(R.id.button6)).check(matches(isDisplayed()))
    }

    @Test
    fun testButton1Click_withoutPropertiesFiles() {
        // Simulate the absence of properties files
        deletePropertiesFiles()

        // Launch HomeActivity
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // Click button1
        onView(withId(R.id.button1)).perform(click())

        // Check if the dialog is displayed
        onView(withText("Incomplete Profile")).check(matches(isDisplayed()))

        // Check if the correct intent is fired (MerchantActivity or XpubAddress)
        onView(withText("OK")).perform(click())
        Intents.intended(hasComponent(MerchantActivity::class.java.name))
    }

    @Test
    fun testButton2Click() {
        // Launch HomeActivity
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // Click button2
        onView(withId(R.id.button2)).perform(click())

        // Check if the correct intent is fired (APIActivity)
        Intents.intended(hasComponent(APIActivity::class.java.name))
    }

    @Test
    fun testSeeAllClick() {
        // Launch HomeActivity
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // Click seeAllTextView
        onView(withId(R.id.seeAllTextView)).perform(click())

        // Check if the correct intent is fired (ViewAllActivity)
        Intents.intended(hasComponent(ViewAllActivity::class.java.name))
    }

    private fun deletePropertiesFiles() {
        // Delete the properties files to simulate their absence
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "merchant.properties").delete()
        File(context.filesDir, "config.properties").delete()
        File(context.filesDir, "api.properties").delete()
    }
}
