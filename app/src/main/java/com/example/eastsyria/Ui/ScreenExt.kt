package com.example.eastsyria.Ui

import android.app.Activity
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

/** Edge to edge on the warm palette: the system bars never turn light on us. */
fun ComponentActivity.warmEdgeToEdge() {
    enableEdgeToEdge()
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}

/**
 * Keeps this view's bottom padding equal to [extraDp] plus whatever the
 * gesture bar or keyboard is taking, so the last row is never hidden.
 */
fun View.padBottomForInsets(extraDp: Int = 0) {
    val extra = (extraDp * resources.displayMetrics.density).toInt()
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(bottom = extra + max(bars.bottom, ime.bottom))
        insets
    }
}

/** Keeps this view's top padding equal to its own plus the status bar. */
fun View.padTopForInsets() {
    val base = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(top = base + bars.top)
        insets
    }
}

fun Activity.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
