package com.vermont.possin

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.min

class OnboardingActivity : AppCompatActivity() {

    private data class TutorialPage(
        val image: Int,
        val title: String,
        val description: String,
        val focus: RectF
    )

    // Focus rectangles are measured in the ORIGINAL 945 x 2048 screenshots.
    private val pages = listOf(
        TutorialPage(R.drawable.tutorial_home, "Start a payment",
            "Tap Accept Payment on the home screen.", RectF(35f, 390f, 795f, 810f)),
        TutorialPage(R.drawable.tutorial_amount, "Enter the amount",
            "Use the keypad to enter the amount, then tap the red Enter button.", RectF(625f, 1220f, 807f, 1670f)),
        TutorialPage(R.drawable.tutorial_coin_choice, "Choose a cryptocurrency",
            "Select the coin your customer wants to pay with.", RectF(72f, 690f, 763f, 931f)),
        TutorialPage(R.drawable.tutorial_payment_qr, "Show the payment QR",
            "Ask the customer to scan the payment QR. Watch the timer and confirmation status.", RectF(260f, 640f, 650f, 958f)),
        TutorialPage(R.drawable.tutorial_coin_settings, "Configure receiving wallets",
            "Open Coin Settings, choose a coin, and enter its public receiving address. Never enter private keys or seed phrases.", RectF(92f, 663f, 650f, 764f)),
        TutorialPage(R.drawable.tutorial_merchant, "Add merchant information",
            "Choose your base currency and enter your business details.", RectF(75f, 370f, 763f, 517f)),
        TutorialPage(R.drawable.tutorial_api, "Configure the API key",
            "Enter your own API key in Settings. This screenshot uses a hidden example.", RectF(37f, 1090f, 808f, 1198f)),
        TutorialPage(R.drawable.tutorial_export, "Export transactions",
            "Choose a date range and tap Export CSV to export transaction records.", RectF(35f, 552f, 795f, 676f))
    )

    private var currentPage = 0
    private lateinit var screenshot: ImageView
    private lateinit var overlay: HighlightOverlay
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var progress: TextView
    private lateinit var next: Button
    private lateinit var back: Button
    private var pulse: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)
        currentPage = (savedInstanceState?.getInt("tutorial_page") ?: 0).coerceIn(0, pages.lastIndex)

        screenshot = findViewById(R.id.tutorialScreenshot)
        title = findViewById(R.id.onboardingTitle)
        description = findViewById(R.id.onboardingDescription)
        progress = findViewById(R.id.onboardingProgress)
        next = findViewById(R.id.onboardingNext)
        back = findViewById(R.id.onboardingBack)
        val skip = findViewById<Button>(R.id.onboardingSkip)

        overlay = HighlightOverlay(this)
        findViewById<FrameLayout>(R.id.screenshotFrame).addView(
            overlay, FrameLayout.LayoutParams(-1, -1)
        )

        next.setOnClickListener {
            if (currentPage == pages.lastIndex) finishTutorial()
            else showPage(currentPage + 1)
        }
        back.setOnClickListener { if (currentPage > 0) showPage(currentPage - 1) }
        skip.setOnClickListener { finishTutorial() }

        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 950L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { overlay.pulse = it.animatedValue as Float; overlay.invalidate() }
            start()
        }
        showPage(currentPage, animate = false)
    }

    private fun showPage(index: Int, animate: Boolean = true) {
        currentPage = index
        val page = pages[index]
        screenshot.setImageResource(page.image)
        overlay.focus = page.focus
        overlay.invalidate()
        title.text = page.title
        description.text = page.description
        progress.text = "${index + 1} / ${pages.size}"
        next.text = if (index == pages.lastIndex) "Finish" else "Next"
        back.isEnabled = index > 0

        if (animate) {
            screenshot.alpha = 0.35f
            screenshot.translationX = 24f * resources.displayMetrics.density
            screenshot.animate().alpha(1f).translationX(0f).setDuration(250L)
                .setInterpolator(DecelerateInterpolator()).start()
        } else {
            screenshot.alpha = 1f
            screenshot.translationX = 0f
        }
    }

    private fun finishTutorial() {
        getSharedPreferences("lcterm_onboarding", MODE_PRIVATE).edit()
            .putBoolean("onboarding_seen", true).apply()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tutorial_page", currentPage)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }

    private class HighlightOverlay(context: Context) : View(context) {
        var focus = RectF()
        var pulse = 0f
        private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 193, 7)
            style = Paint.Style.STROKE
            strokeWidth = 5f * resources.displayMetrics.density
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // ImageView uses fitCenter: scale uniformly and center the 945x2048 image.
            val scale = min(width / 945f, height / 2048f)
            val dx = (width - 945f * scale) / 2f
            val dy = (height - 2048f * scale) / 2f
            val rect = RectF(
                dx + focus.left * scale, dy + focus.top * scale,
                dx + focus.right * scale, dy + focus.bottom * scale
            )
            outline.alpha = (130 + 125 * pulse).toInt()
            canvas.drawRoundRect(rect, 14f, 14f, outline)
        }
    }
}