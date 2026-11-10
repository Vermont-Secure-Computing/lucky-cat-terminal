package com.vermont.possin

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
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Read-only guided tour: never modifies coin settings. */
class CoinSetupTutorial(private val activity: Activity) {
    private val root = activity.findViewById<ViewGroup>(android.R.id.content)
    private val overlay = FrameLayout(activity)
    private val shade = Shade(activity)
    private val panel = LinearLayout(activity)
    private val title = TextView(activity)
    private val message = TextView(activity)
    private val back = Button(activity)
    private val next = Button(activity)
    private val skip = Button(activity)
    private val scale = activity.resources.displayMetrics.density
    private fun dp(n: Int) = (n * scale + 0.5f).toInt()
    private var position = 0
    private var dismissed = false
    private fun selectedCard(): View? {
        val container = activity.findViewById<ViewGroup>(R.id.cryptocurrency_container)
        return (0 until container.childCount).map { container.getChildAt(it) }
            .firstOrNull { it.findViewById<TextView>(R.id.crypto_name)?.text?.toString() == "Bitcoin" }
            ?: container.getChildAt(0)
    }

    private data class Step(
        val titleRes: Int,
        val messageRes: Int,
        val target: () -> View?
    )

    private val steps by lazy {
        listOf(
            Step(
                R.string.coin_tour_search_title,
                R.string.coin_tour_search_message
            ) { activity.findViewById(R.id.search_field) },

            Step(
                R.string.coin_tour_card_title,
                R.string.coin_tour_card_message
            ) { selectedCard() },

            Step(
                R.string.coin_tour_type_title,
                R.string.coin_tour_type_message
            ) { selectedCard()?.findViewById(R.id.type_spinner) },

            Step(
                R.string.coin_tour_wallet_title,
                R.string.coin_tour_wallet_message
            ) { selectedCard()?.findViewById(R.id.input_field) },

            Step(
                R.string.coin_tour_scanner_title,
                R.string.coin_tour_scanner_message
            ) { selectedCard()?.findViewById(R.id.scanner_button) },

            Step(
                R.string.coin_tour_save_title,
                R.string.coin_tour_save_message
            ) { activity.findViewById(R.id.submit_text) },

            Step(
                R.string.coin_tour_backup_title,
                R.string.coin_tour_backup_message
            ) { activity.findViewById(R.id.exportSettingsBtn) }
        )
    }

    fun show() {
        if (overlay.parent != null) return
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.addView(shade, FrameLayout.LayoutParams(-1, -1))
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(12), dp(8), dp(12), dp(8))
        panel.setBackgroundColor(Color.rgb(23, 30, 42))
        title.setTextColor(Color.WHITE)
        title.textSize = 17f
        message.setTextColor(Color.WHITE)
        message.textSize = 13f
        panel.addView(title)
        panel.addView(message)
        val buttons = LinearLayout(activity)
        back.setText(R.string.tutorial_back)
        next.setText(R.string.tutorial_next)
        skip.setText(R.string.tutorial_skip)
        for (button in listOf(back, next, skip)) buttons.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        panel.addView(buttons)
        overlay.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        root.addView(overlay, ViewGroup.LayoutParams(-1, -1))
        back.setOnClickListener { if (position > 0) { position--; render() } }
        next.setOnClickListener { if (position == steps.lastIndex) close() else { position++; render() } }
        skip.setOnClickListener { close() }
        overlay.post { render() }
    }

    private fun render() {
        if (dismissed) return

        val step = steps[position]

        title.text = activity.getString(
            R.string.tutorial_step_heading,
            position + 1,
            steps.size,
            activity.getString(step.titleRes)
        )

        message.setText(step.messageRes)

        back.isEnabled = position > 0

        next.setText(
            if (position == steps.lastIndex)
                R.string.tutorial_finish
            else
                R.string.tutorial_next
        )

        val target = step.target()

        if (target == null) { shade.highlight = null; shade.invalidate(); return }
        val scroll = findScroll(activity.findViewById(android.R.id.content))
        if (scroll != null && isDescendant(target, scroll)) {
            val targetXY = IntArray(2)
            val scrollXY = IntArray(2)
            target.getLocationOnScreen(targetXY)
            scroll.getLocationOnScreen(scrollXY)
            val available = (scroll.height - panel.height - dp(12)).coerceAtLeast(dp(90))
            val desired = scroll.scrollY + targetXY[1] - scrollXY[1] - available / 3
            scroll.smoothScrollTo(0, desired.coerceAtLeast(0))
        }
        val expected = position
        overlay.postDelayed({
            if (dismissed || expected != position) return@postDelayed
            val a = IntArray(2)
            val b = IntArray(2)
            target.getLocationOnScreen(a)
            overlay.getLocationOnScreen(b)
            shade.highlight = RectF(
                (a[0] - b[0] - dp(5)).toFloat(),
                (a[1] - b[1] - dp(5)).toFloat(),
                (a[0] - b[0] + target.width + dp(5)).toFloat(),
                (a[1] - b[1] + target.height + dp(5)).toFloat()
            )
            shade.invalidate()
        }, 350L)
    }
    private fun isDescendant(child: View, ancestor: View): Boolean {
        var current: android.view.ViewParent? = child.parent
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }
    private fun findScroll(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findScroll(view.getChildAt(i))?.let { return it }
        }
        return null
    }
    private fun close() {
        if (dismissed) return
        dismissed = true
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        activity.getSharedPreferences("AppPrefs", Activity.MODE_PRIVATE).edit()
            .putBoolean("COIN_TUTORIAL_SEEN", true).apply()
    }
    private class Shade(activity: Activity) : View(activity) {
        var highlight: RectF? = null
        private val scale = resources.displayMetrics.density
        private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB9000000.toInt() }
        private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 3f * scale
        }
        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
        override fun onDraw(canvas: Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
            highlight?.let {
                canvas.drawRoundRect(it, 9f * scale, 9f * scale, clear)
                canvas.drawRoundRect(it, 9f * scale, 9f * scale, border)
            }
        }
    }
}