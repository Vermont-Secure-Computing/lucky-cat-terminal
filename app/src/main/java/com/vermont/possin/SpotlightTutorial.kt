
package com.vermont.possin

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SpotlightTutorial(private val activity: Activity) {


    private data class Step(
        val targetId: Int,
        val titleRes: Int,
        val messageRes: Int
    )

    private val steps = listOf(
        Step(
            R.id.button1,
            R.string.tutorial_accept_payment_title,
            R.string.tutorial_accept_payment_message
        ),
        Step(
            R.id.button2,
            R.string.tutorial_api_key_title,
            R.string.tutorial_api_key_message
        ),
        Step(
            R.id.button4,
            R.string.tutorial_merchant_title,
            R.string.tutorial_merchant_message
        ),
        Step(
            R.id.button5,
            R.string.tutorial_coin_title,
            R.string.tutorial_coin_message
        ),
        Step(
            R.id.button6,
            R.string.tutorial_export_title,
            R.string.tutorial_export_message
        )
    )


    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density + 0.5f).toInt()

    private val overlay = FrameLayout(activity)
    private val shade = SpotlightShade(activity)
    private val panel = LinearLayout(activity)
    private val heading = TextView(activity)
    private val description = TextView(activity)
    private val back = Button(activity)
    private val next = Button(activity)
    private val skip = Button(activity)

    private var index = 0
    private var closed = false

    fun show() {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)

        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.addView(shade, FrameLayout.LayoutParams(-1, -1))

        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        panel.setBackgroundColor(Color.rgb(22, 29, 43))

        heading.setTextColor(Color.WHITE)
        heading.setTypeface(null, Typeface.BOLD)
        heading.textSize = 18f

        description.setTextColor(Color.rgb(225, 230, 239))
        description.textSize = 14f
        description.setPadding(0, dp(6f), 0, dp(8f))

        panel.addView(heading)
        panel.addView(description)

        val actions = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        actions.addView(back, LinearLayout.LayoutParams(0, dp(48f), 1f))
        actions.addView(next, LinearLayout.LayoutParams(0, dp(48f), 1f))
        actions.addView(skip, LinearLayout.LayoutParams(0, dp(48f), 1f))
        panel.addView(actions)

        overlay.addView(
            panel,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
        )

        root.addView(overlay, ViewGroup.LayoutParams(-1, -1))

        back.setText(R.string.tutorial_back)
        skip.setText(R.string.tutorial_skip)

        back.setOnClickListener {
            if (index > 0) {
                index--
                render()
            }
        }

        next.setOnClickListener {
            if (index == steps.lastIndex) {
                close()
            } else {
                index++
                render()
            }
        }

        skip.setOnClickListener { close() }
        overlay.post { render() }
    }

    private fun render() {
        if (closed) return


        val step = steps[index]

        heading.text = activity.getString(
            R.string.tutorial_step_heading,
            index + 1,
            steps.size,
            activity.getString(step.titleRes)
        )

        description.setText(step.messageRes)

        back.isEnabled = index > 0

        next.setText(
            if (index == steps.lastIndex) {
                R.string.tutorial_finish
            } else {
                R.string.tutorial_next
            }
        )


        val target = activity.findViewById<View>(step.targetId)

        if (target == null) {
            shade.highlight = null
            shade.invalidate()
            return
        }

        val scroll = findScrollView(
            activity.findViewById(android.R.id.content)
        )

        if (scroll != null) {
            val targetLocation = IntArray(2)
            val scrollLocation = IntArray(2)

            target.getLocationOnScreen(targetLocation)
            scroll.getLocationOnScreen(scrollLocation)

            val targetCenter =
                targetLocation[1] - scrollLocation[1] + target.height / 2

            val visibleHeight =
                (scroll.height - panel.height - dp(20f))
                    .coerceAtLeast(dp(120f))

            val desired =
                (scroll.scrollY + targetCenter - visibleHeight / 2)
                    .coerceAtLeast(0)

            scroll.smoothScrollTo(0, desired)
        }

        overlay.postDelayed({
            if (!closed && index == steps.indexOf(step)) {
                val a = IntArray(2)
                val b = IntArray(2)

                target.getLocationOnScreen(a)
                overlay.getLocationOnScreen(b)

                shade.highlight = RectF(
                    (a[0] - b[0] - dp(6f)).toFloat(),
                    (a[1] - b[1] - dp(6f)).toFloat(),
                    (a[0] - b[0] + target.width + dp(6f)).toFloat(),
                    (a[1] - b[1] + target.height + dp(6f)).toFloat()
                )

                shade.invalidate()
            }
        }, 400L)
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findScrollView(view.getChildAt(i))
                if (found != null) return found
            }
        }

        return null
    }

    private fun close() {
        if (closed) return

        closed = true
        (overlay.parent as? ViewGroup)?.removeView(overlay)

        activity.getSharedPreferences(
            "AppPrefs",
            Activity.MODE_PRIVATE
        ).edit()
            .putBoolean("SPOTLIGHT_TUTORIAL_SEEN", true)
            .apply()
    }

    private class SpotlightShade(activity: Activity) : View(activity) {

        var highlight: RectF? = null

        private val scale = resources.displayMetrics.density

        private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB9000000.toInt()
        }

        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }

        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 4f * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawRect(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                dimPaint
            )

            val r = highlight ?: return

            canvas.drawRoundRect(
                r, 12f * scale, 12f * scale, clearPaint
            )

            canvas.drawRoundRect(
                r, 12f * scale, 12f * scale, borderPaint
            )

            val x = r.centerX()
            val below =
                r.bottom + 48f * scale < height - 180f * scale

            val tip = if (below) {
                r.bottom + 8f * scale
            } else {
                r.top - 8f * scale
            }

            val tail = if (below) {
                tip + 34f * scale
            } else {
                tip - 34f * scale
            }

            val path = Path().apply {
                moveTo(x, tail)
                lineTo(x, tip)

                moveTo(
                    x - 9f * scale,
                    if (below) tip + 10f * scale
                    else tip - 10f * scale
                )

                lineTo(x, tip)

                lineTo(
                    x + 9f * scale,
                    if (below) tip + 10f * scale
                    else tip - 10f * scale
                )
            }

            canvas.drawPath(path, arrowPaint)
        }
    }
}
