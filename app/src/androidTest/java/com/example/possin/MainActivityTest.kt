package com.example.possin

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setUp() {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent)
    }

    @Test
    fun testHeaderIsDisplayed() {
        onView(withId(R.id.top_view)).check(matches(isDisplayed()))
        onView(withId(R.id.header)).check(matches(isDisplayed()))
        onView(withId(R.id.logo_image)).check(matches(isDisplayed()))
        onView(withId(R.id.submit_text)).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchBarIsDisplayed() {
        onView(withId(R.id.search_bar)).check(matches(isDisplayed()))
    }

    @Test
    fun testSelectedCryptocurrenciesContainerIsDisplayed() {
        onView(withId(R.id.selected_cryptocurrencies_container)).check(matches(isDisplayed()))
    }

    @Test
    fun testRecyclerViewIsDisplayed() {
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun testAddCryptocurrency() {
        onView(withId(R.id.search_bar)).perform(typeText("Bitcoin"), closeSoftKeyboard())
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.selected_cryptocurrencies_container)).check(matches(hasDescendant(withText("Bitcoin"))))
    }

    @Test
    fun testSubmitButtonWithoutSelection() {
        onView(withId(R.id.submit_text)).perform(click())
        onView(withText("Please select at least one cryptocurrency.")).check(matches(isDisplayed()))
    }

    @Test
    fun testSubmitButtonWithSelection() {
        onView(withId(R.id.search_bar)).perform(typeText("Bitcoin"), closeSoftKeyboard())
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.submit_text)).perform(click())
        onView(withId(R.id.xpub_input_container)).check(matches(isDisplayed()))
    }
}
