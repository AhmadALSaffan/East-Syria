package com.example.eastsyria.Nav

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Map.MapActivity
import com.example.eastsyria.Notifications.NotificationsActivity
import com.example.eastsyria.Profile.ProfileActivity
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity

/**
 * The bottom dock every top-level screen shares: Explore, Saved, Updates and
 * Profile as four tabs around the map button. One place decides where each tab
 * goes, so the bar behaves the same wherever you meet it.
 *
 * Moving between tabs slides the next screen in from the side you moved
 * toward. Explore is the root: going back to it clears everything above it,
 * and a tab opened from another tab replaces it, so Back always lands on
 * Explore rather than on a stack of tabs.
 */
class NavDock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pill: PillNavBar
    private val basePaddingBottom: Int
    private var current = TAB_EXPLORE

    /** Called when the tab you are already on is tapped again. */
    var onReselected: (() -> Unit)? = null

    init {
        clipChildren = false
        clipToPadding = false
        val density = resources.displayMetrics.density
        setPadding(0, 0, 0, (12 * density).toInt())
        basePaddingBottom = paddingBottom

        LayoutInflater.from(context).inflate(R.layout.view_nav_dock, this, true)
        pill = findViewById(R.id.dockPill)

        // The pill sits in the lower part of the dock; the map button rises above it.
        (pill.layoutParams as LayoutParams).topMargin = (36 * density).toInt()

        pill.setItems(
            listOf(
                PillNavBar.Item(R.drawable.ic_explore, R.string.nav_explore),
                PillNavBar.Item(R.drawable.ic_bookmark_filled, R.string.nav_saved),
                PillNavBar.Item(R.drawable.ic_notifications, R.string.nav_updates),
                PillNavBar.Item(R.drawable.ic_profile, R.string.nav_profile)
            )
        )
        pill.setCenterGap(afterIndex = 1, widthDp = 84)

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = basePaddingBottom + bars.bottom)
            insets
        }
    }

    /** Wires the dock to [activity], which is the screen for tab [tab]. */
    fun attach(activity: Activity, tab: Int) {
        current = tab
        pill.select(tab, animate = false)

        pill.listener = object : PillNavBar.Listener {
            override fun onTabSelected(index: Int, direction: Int) = open(activity, index, direction)
            override fun onTabReselected(index: Int) {
                onReselected?.invoke()
            }
        }
        findViewById<android.view.View>(R.id.dockMap).setOnClickListener {
            activity.startActivity(Intent(activity, MapActivity::class.java))
        }
    }

    /** Moves the selector without opening anything (used when returning to a screen). */
    fun select(tab: Int, animate: Boolean = true) {
        current = tab
        pill.select(tab, animate)
    }

    private fun open(activity: Activity, index: Int, direction: Int) {
        if (index == current) return
        val target = when (index) {
            TAB_EXPLORE -> MainPageActivity::class.java
            TAB_SAVED -> SavedLandmarksActivity::class.java
            TAB_UPDATES -> NotificationsActivity::class.java
            else -> ProfileActivity::class.java
        }
        val intent = Intent(activity, target)
        if (index == TAB_EXPLORE) {
            intent.putExtra(MainPageActivity.EXTRA_SELECT_EXPLORE, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val (enter, exit) = if (direction >= 0) {
            R.anim.slide_in_right to R.anim.slide_out_left
        } else {
            R.anim.slide_in_left to R.anim.slide_out_right
        }
        activity.startActivity(
            intent,
            ActivityOptions.makeCustomAnimation(activity, enter, exit).toBundle()
        )
        // A tab opened from another tab replaces it; Explore is the only root.
        if (current != TAB_EXPLORE && index != TAB_EXPLORE) activity.finish()
    }

    companion object {
        const val TAB_EXPLORE = 0
        const val TAB_SAVED = 1
        const val TAB_UPDATES = 2
        const val TAB_PROFILE = 3
    }
}
