package com.example.eastsyria.Ui

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * The slow breath that tells you a screen is loading. Every skeleton in the
 * app shares it, and nothing runs when the system has animations switched off.
 */
class SkeletonPulse(private vararg val views: View) {

    private var animator: ValueAnimator? = null

    fun start() {
        if (animator != null || !ValueAnimator.areAnimatorsEnabled()) return
        animator = ValueAnimator.ofFloat(1f, 0.55f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { update ->
                val alpha = update.animatedValue as Float
                views.forEach { it.alpha = alpha }
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        views.forEach { it.alpha = 1f }
    }
}
