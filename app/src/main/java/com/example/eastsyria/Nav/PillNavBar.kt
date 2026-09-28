package com.example.eastsyria.Nav

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.eastsyria.R
import kotlin.math.abs
import kotlin.math.min

/**
 * A fixed, icon-only pill navigation with a selector that can be carried.
 *
 * Tap a tab to commit it. Press and slide instead and the selector follows the
 * finger, previewing each tab it passes (icons lift and warm as it nears them);
 * lifting the finger commits the tab underneath it. The selector glides with a
 * slight overshoot, swells while dragged, and reports the direction of travel
 * so the host can slide the next screen in from the matching side.
 *
 * The bar never moves. It is laid out by its parent and only the selector
 * inside it travels.
 */
class PillNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class Item(@DrawableRes val icon: Int, @StringRes val label: Int)

    interface Listener {
        /** [direction] is +1 when the finger (or the target) moved right, -1 when left. */
        fun onTabSelected(index: Int, direction: Int)
        fun onTabReselected(index: Int)
    }

    var listener: Listener? = null

    private val items = mutableListOf<Item>()
    private val icons = mutableListOf<ImageView>()

    private val iconBox = dp(48)
    private val pillHeight = dp(48)
    private val sidePad = dp(8)
    private val overshootSlack = dp(6).toFloat()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val colorInactive = ContextCompat.getColor(context, R.color.ivory_dim)
    private val colorActive = ContextCompat.getColor(context, R.color.terracotta_soft)
    private val argb = ArgbEvaluator()

    private val highlight = View(context).apply {
        background = ContextCompat.getDrawable(context, R.drawable.bg_nav_highlight)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Room left in the middle of the bar for the docked map button, after tab [gapAfter]. */
    private var gapAfter = -1
    private var gapPx = 0

    private var selectedIndex = 0
    private var pillCenterX = 0f
    private var pillScale = 1f
    private var dragging = false
    private var previewIndex = -1
    private var downX = 0f
    private var followTarget = 0f
    private var laidOut = false

    private var moveAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null

    init {
        addView(highlight, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    /** Leaves [widthDp] of empty space after tab [afterIndex] (the map button sits in it). */
    fun setCenterGap(afterIndex: Int, widthDp: Int) {
        gapAfter = afterIndex
        gapPx = dp(widthDp)
        requestLayout()
    }

    fun setItems(newItems: List<Item>) {
        icons.forEach { removeView(it) }
        icons.clear()
        items.clear()
        items.addAll(newItems)

        newItems.forEachIndexed { index, item ->
            val icon = ImageView(context).apply {
                setImageResource(item.icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(12), dp(12), dp(12), dp(12))
                contentDescription = context.getString(item.label)
                isFocusable = true
                isClickable = true
                // Touch is taken by the bar itself; this click only fires for
                // TalkBack, switch access and the keyboard.
                setOnClickListener { commit(index, 0) }
                setColorFilter(colorInactive)
            }
            icons.add(icon)
            addView(icon, LayoutParams(iconBox, iconBox))
        }
        selectedIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        updateSelectedState()
        requestLayout()
    }

    /** Moves the selector without reporting a selection. */
    fun select(index: Int, animate: Boolean = true) {
        if (index == selectedIndex && !dragging) {
            if (laidOut && abs(pillCenterX - centerOf(index)) > 0.5f) {
                if (animate) animatePillTo(centerOf(index)) else snapPillTo(index)
            }
            return
        }
        selectedIndex = index
        updateSelectedState()
        if (!laidOut || items.isEmpty()) return
        if (animate) animatePillTo(centerOf(index)) else snapPillTo(index)
    }

    // ------------------------------------------------------------ layout

    private val tabWidth: Float
        get() = if (items.isEmpty()) 0f else (width - 2 * sidePad - gapPx) / items.size.toFloat()

    private fun centerOf(index: Int): Float =
        sidePad + tabWidth * (index + 0.5f) + if (gapAfter >= 0 && index > gapAfter) gapPx else 0

    /** The tab whose centre is nearest, so the gap in the middle belongs to whichever side is closer. */
    private fun indexAt(x: Float): Int {
        if (items.isEmpty() || tabWidth <= 0f) return 0
        return items.indices.minByOrNull { abs(x - centerOf(it)) } ?: 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            dp(64)
        }
        setMeasuredDimension(w, h)

        val perTab = if (items.isEmpty()) 0f else (w - 2 * sidePad - gapPx) / items.size.toFloat()
        val pillWidth = min(dp(72).toFloat(), perTab - dp(12)).toInt().coerceAtLeast(dp(40))
        highlight.measure(
            MeasureSpec.makeMeasureSpec(pillWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(pillHeight, MeasureSpec.EXACTLY)
        )
        val box = MeasureSpec.makeMeasureSpec(iconBox, MeasureSpec.EXACTLY)
        icons.forEach { it.measure(box, box) }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val h = bottom - top
        highlight.layout(
            0, (h - pillHeight) / 2,
            highlight.measuredWidth, (h + pillHeight) / 2
        )
        icons.forEachIndexed { i, icon ->
            val cx = centerOf(i).toInt()
            icon.layout(
                cx - iconBox / 2, (h - iconBox) / 2,
                cx + iconBox / 2, (h + iconBox) / 2
            )
        }
        if (!laidOut || (!dragging && moveAnimator?.isRunning != true)) {
            pillCenterX = if (items.isEmpty()) 0f else centerOf(selectedIndex)
        }
        laidOut = true
        updateVisuals()
    }

    // ------------------------------------------------------------ touch

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (items.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x
                dragging = false
                previewIndex = indexAt(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging && abs(event.x - downX) > touchSlop) beginDrag(event.x)
                if (dragging) {
                    followTarget = clampCenter(event.x)
                    val index = indexAt(event.x)
                    if (index != previewIndex) {
                        previewIndex = index
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging
                val index = indexAt(event.x)
                val direction = if (wasDragging) {
                    if (event.x >= downX) 1 else -1
                } else {
                    0
                }
                endDrag()
                commit(index, direction)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                endDrag()
                animatePillTo(centerOf(selectedIndex))
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun beginDrag(x: Float) {
        dragging = true
        moveAnimator?.cancel()
        followTarget = clampCenter(x)
        animateScaleTo(1.12f, overshoot = false)
        postOnAnimation(follow)
    }

    private fun endDrag() {
        dragging = false
        removeCallbacks(follow)
        animateScaleTo(1f, overshoot = true)
    }

    private fun clampCenter(x: Float): Float =
        x.coerceIn(centerOf(0), centerOf(items.lastIndex))

    /** Eases the selector toward the finger a frame at a time, so it trails it. */
    private val follow = object : Runnable {
        override fun run() {
            if (!dragging) return
            val gap = followTarget - pillCenterX
            pillCenterX = if (abs(gap) < 0.5f) followTarget else pillCenterX + gap * 0.34f
            updateVisuals()
            postOnAnimation(this)
        }
    }

    // ------------------------------------------------------------ selection

    private fun commit(index: Int, dragDirection: Int) {
        if (items.isEmpty()) return
        if (index == selectedIndex) {
            animatePillTo(centerOf(index))
            listener?.onTabReselected(index)
            return
        }
        val direction = when {
            dragDirection != 0 -> dragDirection
            index > selectedIndex -> 1
            else -> -1
        }
        selectedIndex = index
        updateSelectedState()
        animatePillTo(centerOf(index))
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        listener?.onTabSelected(index, direction)
    }

    private fun updateSelectedState() {
        icons.forEachIndexed { i, icon -> icon.isSelected = i == selectedIndex }
    }

    // ------------------------------------------------------------ motion

    private fun snapPillTo(index: Int) {
        moveAnimator?.cancel()
        pillCenterX = centerOf(index)
        updateVisuals()
    }

    private fun animatePillTo(target: Float) {
        moveAnimator?.cancel()
        val from = pillCenterX
        if (abs(target - from) < 0.5f) {
            pillCenterX = target
            updateVisuals()
            return
        }
        val lo = centerOf(0) - overshootSlack
        val hi = centerOf(items.lastIndex) + overshootSlack
        moveAnimator = ValueAnimator.ofFloat(from, target).apply {
            duration = 460
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener {
                pillCenterX = (it.animatedValue as Float).coerceIn(lo, hi)
                updateVisuals()
            }
            start()
        }
    }

    private fun animateScaleTo(target: Float, overshoot: Boolean) {
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(pillScale, target).apply {
            duration = if (overshoot) 320 else 180
            interpolator = if (overshoot) OvershootInterpolator(2f) else DecelerateInterpolator()
            addUpdateListener {
                pillScale = it.animatedValue as Float
                updateVisuals()
            }
            start()
        }
    }

    /** Places the selector, then lets each icon react to how near it is. */
    private fun updateVisuals() {
        if (items.isEmpty() || tabWidth <= 0f) return
        highlight.translationX = pillCenterX - highlight.width / 2f
        highlight.scaleX = pillScale
        highlight.scaleY = pillScale

        val lift = ((pillScale - 1f) / 0.12f).coerceIn(0f, 1f)
        icons.forEachIndexed { i, icon ->
            val nearness = (1f - abs(pillCenterX - centerOf(i)) / tabWidth).coerceIn(0f, 1f)
            icon.setColorFilter(argb.evaluate(nearness, colorInactive, colorActive) as Int)
            val scale = 1f + nearness * (0.12f + 0.16f * lift)
            icon.scaleX = scale
            icon.scaleY = scale
        }
    }

    override fun onDetachedFromWindow() {
        moveAnimator?.cancel()
        scaleAnimator?.cancel()
        removeCallbacks(follow)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
