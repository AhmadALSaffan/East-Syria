package com.example.eastsyria.Admin.Reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.eastsyria.R

/**
 * A bar per time bucket, counted from real records. The tallest bar is gold and
 * carries its value; everything else is terracotta. Drawn in the app's own
 * colours and sized in dp, so it holds up on any screen density.
 */
class EngagementChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: List<Pair<String, Float>> = emptyList()
    private var maxValue: Float = 1f
    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.terracotta)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.wayfinding)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ivory_mute)
        textSize = 12f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ivory)
        textSize = 13f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setData(entries: List<Pair<String, Float>>) {
        data = entries
        maxValue = entries.maxOfOrNull { it.second } ?: 1f
        if (maxValue == 0f) maxValue = 1f
        contentDescription = entries.joinToString(", ") { "${it.first}: ${it.second.toInt()}" }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val bottomPad = 28f * density
        val topPad = 28f * density
        val chartH = h - bottomPad - topPad
        val slotW = w / data.size
        val barW = slotW * 0.5f
        val radius = 10f * density

        var maxIdx = 0
        for (i in data.indices) if (data[i].second > data[maxIdx].second) maxIdx = i

        for (i in data.indices) {
            val (label, value) = data[i]
            val left = i * slotW + (slotW - barW) / 2
            val right = left + barW

            // The empty track behind every bar, so a zero still reads as a bucket.
            canvas.drawRoundRect(
                RectF(left, topPad, right, topPad + chartH), radius, radius, trackPaint
            )

            val barH = chartH * (value / maxValue)
            if (value > 0f) {
                val top = topPad + chartH - barH
                canvas.drawRoundRect(
                    RectF(left, top, right, topPad + chartH),
                    radius, radius,
                    if (i == maxIdx) highlightPaint else barPaint
                )
                if (i == maxIdx) {
                    canvas.drawText(value.toInt().toString(), left + barW / 2, top - 8f * density, valuePaint)
                }
            }

            canvas.drawText(label, left + barW / 2, h - 6f * density, labelPaint)
        }
    }
}
