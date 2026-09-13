package com.example.uvcmonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Luminance histogram overlay: 256 bins, darks on the left, whites on the
 * right. Feed it with [update] from any thread.
 */
class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bins = IntArray(BINS)
    private val path = Path()
    private val bounds = RectF()

    private val bgPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 235, 235, 235)
        style = Paint.Style.FILL
    }
    private val guidePaint = Paint().apply {
        color = Color.argb(70, 255, 255, 255)
        strokeWidth = 1f
    }
    private val edgePaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        strokeWidth = 1f
    }

    /** Replace the displayed data. `newBins` must have [BINS] entries. */
    fun update(newBins: IntArray) {
        synchronized(bins) { System.arraycopy(newBins, 0, bins, 0, BINS) }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = 6f * resources.displayMetrics.density
        bounds.set(0f, 0f, w, h)
        canvas.drawRoundRect(bounds, radius, radius, bgPaint)

        val pad = 4f * resources.displayMetrics.density
        val plotW = w - 2 * pad
        val plotH = h - 2 * pad
        if (plotW <= 0 || plotH <= 0) return

        // Quarter guides: helps read shadows / mids / highlights at a glance.
        for (i in 1..3) {
            val x = pad + plotW * i / 4f
            canvas.drawLine(x, pad, x, h - pad, guidePaint)
        }

        val max: Int
        path.reset()
        synchronized(bins) {
            max = bins.maxOrNull()?.takeIf { it > 0 } ?: return
            path.moveTo(pad, h - pad)
            for (i in 0 until BINS) {
                val x = pad + plotW * i / (BINS - 1).toFloat()
                // sqrt compresses the peaks so small populations stay visible
                val v = Math.sqrt(bins[i].toDouble() / max).toFloat()
                path.lineTo(x, h - pad - plotH * v)
            }
            path.lineTo(pad + plotW, h - pad)
            path.close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawRoundRect(bounds, radius, radius, edgePaint.apply { style = Paint.Style.STROKE })
    }

    companion object {
        const val BINS = 256
    }
}
