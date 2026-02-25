package com.example.eastsyria.Admin.Reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class EngagementChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: List<Pair<String, Float>> = emptyList()
    private var maxValue: Float = 1f

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFC8A87A.toInt()
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF9800.toInt()
    }

    fun setData(entries: List<Pair<String, Float>>) {
        data = entries
        maxValue = entries.maxOfOrNull { it.second } ?: 1f
        if (maxValue == 0f) maxValue = 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val bottomPad = 60f
        val topPad = 48f
        val sidePad = 20f
        val chartH = h - bottomPad - topPad
        val count = data.size
        val slotW = (w - sidePad * 2) / count
        val barW = slotW * 0.45f

        var maxIdx = 0
        var maxVal = 0f
        for (i in data.indices) {
            if (data[i].second > maxVal) { maxVal = data[i].second; maxIdx = i }
        }

        for (i in data.indices) {
            val (label, value) = data[i]
            val ratio = if (maxValue > 0) value / maxValue else 0f
            val barH = chartH * ratio
            val left = sidePad + i * slotW + (slotW - barW) / 2
            val right = left + barW
            val top = topPad + chartH - barH
            val bottom = topPad + chartH
            val rect = RectF(left, top, right, bottom)
            val paint = if (i == maxIdx) highlightPaint else barPaint
            canvas.drawRoundRect(rect, 12f, 12f, paint)

            canvas.drawText(
                label.take(3).uppercase(),
                left + barW / 2,
                h - 10f,
                labelPaint
            )

            if (i == maxIdx) {
                val tooltipW = 100f
                val tooltipH = 44f
                val tooltipLeft = left + barW / 2 - tooltipW / 2
                val tooltipTop = top - tooltipH - 8f
                canvas.drawRoundRect(
                    RectF(tooltipLeft, tooltipTop, tooltipLeft + tooltipW, tooltipTop + tooltipH),
                    10f, 10f, tooltipPaint
                )
                val displayVal = if (value >= 1000) String.format("%.1fk", value / 1000f) else value.toInt().toString()
                canvas.drawText(displayVal, left + barW / 2, tooltipTop + 30f, valuePaint)
            }
        }
    }
}