package org.opbutton.sdk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

internal class OpButtonLiveOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAP_RELEASE_MS = 90L
        private const val TAP_HIDE_MS = 420L
        private const val RELEASE_HIDE_MS = 260L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x66FFFFFF
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFF7043.toInt()
    }

    private var visible = false
    private var pressed = false
    private var centerX = 0f
    private var centerY = 0f

    private val releaseRunnable = Runnable {
        pressed = false
        invalidate()
    }
    private val hideRunnable = Runnable {
        visible = false
        pressed = false
        invalidate()
    }

    init {
        isClickable = false
        isFocusable = false
        setWillNotDraw(false)
    }

    fun showTap(x: Float, y: Float) {
        updatePosition(x, y)
        pressed = true
        visible = true
        invalidate()

        handler.removeCallbacks(releaseRunnable)
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(releaseRunnable, TAP_RELEASE_MS)
        handler.postDelayed(hideRunnable, TAP_HIDE_MS)
    }

    fun showPress(x: Float, y: Float) {
        updatePosition(x, y)
        pressed = true
        visible = true
        handler.removeCallbacks(releaseRunnable)
        handler.removeCallbacks(hideRunnable)
        invalidate()
    }

    fun showRelease(x: Float, y: Float) {
        updatePosition(x, y)
        pressed = false
        visible = true
        handler.removeCallbacks(releaseRunnable)
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, RELEASE_HIDE_MS)
        invalidate()
    }

    fun clearIndicator() {
        handler.removeCallbacks(releaseRunnable)
        handler.removeCallbacks(hideRunnable)
        visible = false
        pressed = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!visible) return

        val outerRadius = dp(if (pressed) 28f else 22f)
        val innerRadius = dp(if (pressed) 12f else 8f)

        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)
    }

    private fun updatePosition(x: Float, y: Float) {
        centerX = x
        centerY = y
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
