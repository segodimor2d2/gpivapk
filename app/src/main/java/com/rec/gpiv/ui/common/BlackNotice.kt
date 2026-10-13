package com.rec.gpiv.ui.common

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** In-app notices with consistent colors, independent of the system toast theme. */
class BlackNotice(private val host: FrameLayout) {
    private val density = host.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private val label = TextView(host.context).apply {
        setBackgroundColor(Color.BLACK)
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8))
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val hide = Runnable { dismiss() }

    fun show(message: String, durationMillis: Long = 3500L) {
        label.removeCallbacks(hide)
        label.text = message
        if (label.parent == null) {
            val bottomInset = ViewCompat.getRootWindowInsets(host)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
            host.addView(label, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { setMargins(dp(12), 0, dp(12), bottomInset + dp(12)) })
        }
        label.bringToFront()
        label.postDelayed(hide, durationMillis)
    }

    fun dismiss() {
        label.removeCallbacks(hide)
        host.removeView(label)
    }
}
