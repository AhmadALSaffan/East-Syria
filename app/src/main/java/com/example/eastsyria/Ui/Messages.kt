package com.example.eastsyria.Ui

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.eastsyria.R
import com.google.android.material.snackbar.Snackbar

/**
 * A transient message in the app's own colours. Toasts are for nothing here:
 * a Snackbar can be read at leisure, can carry an action, and sits above the
 * gesture bar instead of floating over the content.
 */
fun Activity.showSnack(root: View, message: CharSequence, long: Boolean = false) {
    Snackbar.make(root, message, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        .setBackgroundTint(ContextCompat.getColor(this, R.color.surface_high))
        .setTextColor(ContextCompat.getColor(this, R.color.ivory))
        .setActionTextColor(ContextCompat.getColor(this, R.color.terracotta_soft))
        .show()
}

/** Light status and navigation bar icons, for screens that are always dark. */
fun Activity.lightBarIcons() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}
