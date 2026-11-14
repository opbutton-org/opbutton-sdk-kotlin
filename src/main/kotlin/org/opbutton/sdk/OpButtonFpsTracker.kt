package org.opbutton.sdk

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import java.util.concurrent.atomic.AtomicBoolean

internal class OpButtonFpsTracker : Choreographer.FrameCallback {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    @Volatile var fps: Double = 0.0
        private set
    @Volatile var frameTimeMs: Double = 0.0
        private set

    private var prevFrameNanos: Long = 0
    private var frameCount: Int = 0
    private var fpsWindowStartNanos: Long = 0

    companion object {
        private const val FPS_WINDOW_NANOS = 500_000_000L
    }

    fun start() {
        if (running.compareAndSet(false, true)) {
            mainHandler.post {
                prevFrameNanos = 0
                frameCount = 0
                fpsWindowStartNanos = 0
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun stop() {
        if (running.compareAndSet(true, false)) {
            mainHandler.post {
                Choreographer.getInstance().removeFrameCallback(this)
            }
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running.get()) return

        if (prevFrameNanos > 0) {
            val deltaNanos = frameTimeNanos - prevFrameNanos
            frameTimeMs = deltaNanos / 1_000_000.0
        }

        prevFrameNanos = frameTimeNanos
        frameCount++

        if (fpsWindowStartNanos == 0L) {
            fpsWindowStartNanos = frameTimeNanos
        } else {
            val elapsed = frameTimeNanos - fpsWindowStartNanos
            if (elapsed >= FPS_WINDOW_NANOS) {
                fps = (frameCount * 1_000_000_000.0) / elapsed
                frameCount = 0
                fpsWindowStartNanos = frameTimeNanos
            }
        }

        Choreographer.getInstance().postFrameCallback(this)
    }
}
