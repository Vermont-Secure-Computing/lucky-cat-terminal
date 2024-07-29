package com.example.possin

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.content.ContextCompat

class CustomAutoCompleteTextView(context: Context, attrs: AttributeSet) : AppCompatAutoCompleteTextView(context, attrs) {

    private var clearButtonImage: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_clear_search)

    init {
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = compoundDrawablesRelative[2] ?: compoundDrawables[2]
                drawableEnd?.let {
                    if (event.rawX >= (right - it.bounds.width())) {
                        text?.clear()
                        handleClearButton()
                        performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                handleClearButton()
            }
        })

        handleClearButton()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun handleClearButton() {
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            null, null, if (text?.isNotEmpty() == true) clearButtonImage else null, null
        )
    }
}
