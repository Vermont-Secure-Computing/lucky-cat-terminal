package com.vermont.possin

import android.view.View
import android.widget.EditText
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

object CustomMatchers {

    fun withEmptyText(): Matcher<View> {
        return object : BoundedMatcher<View, EditText>(EditText::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with empty text")
            }

            override fun matchesSafely(editText: EditText): Boolean {
                val text = editText.text.toString().trim()
                println("EditText content: '$text'")
                return text.isEmpty()
            }
        }
    }
}
