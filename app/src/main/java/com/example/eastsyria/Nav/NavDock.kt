package com.example.eastsyria.Nav

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
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
 * The bottom tab bar every top-level screen shares: Explore, Saved, Map, Updates and Profile,
 * each an icon over a label, flat on the page with a hairline above. The active tab is in ink;
 * the rest are muted. The map is a tab like the others (it opens the map screen on top).
 *
 * Moving between tabs slides the next screen in from the side you moved toward. Explore is the
 * root: going back to it clears everything above it, and a tab opened from another tab replaces
 * it, so Back always lands on Explore rather than on a stack of tabs.
 */
class NavDock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private data class Tab(val index: Int, @DrawableRes val icon: Int, @StringRes val label: Int)

    // Visual order. TAB_MAP is not a screen with its own dock; it opens the map.
    private val tabs = listOf(
        Tab(TAB_EXPLORE, R.drawable.ic_explore, R.string.nav_explore),
        Tab(TAB_SAVED, R.drawable.ic_bookmark_filled, R.string.nav_saved),
        Tab(TAB_MAP, R.drawable.ic_map_outlined, R.string.nav_map_tab),
        Tab(TAB_UPDATES, R.drawable.ic_notifications, R.string.nav_updates),
        Tab(TAB_PROFILE, R.drawable.ic_profile, R.string.nav_profile),
    )
    private val views = mutableMapOf<Int, Pair<ImageView, TextView>>()
    private val cells = mutableMapOf<Int, View>()
    private val active = ContextCompat.getColor(context, R.color.ivory)
    private val inactive = ContextCompat.getColor(context, R.color.ivory_mute)
    private val basePaddingBottom: Int
    private var current = TAB_EXPLORE
    private var host: Activity? = null

    /** Called when the tab you are already on is tapped again. */
    var onReselected: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.canvas))
        elevation = 0f

        // Hairline above the bar.
        addView(View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.line))
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(4), dp(6), dp(4), dp(4))
        }
        tabs.forEach { tab -> row.addView(cell(tab), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)) }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        basePaddingBottom = paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = basePaddingBottom + bars.bottom)
            insets
        }
        paint(current)
    }

    private fun cell(tab: Tab): View {
        val icon = ImageView(context).apply {
            setImageResource(tab.icon)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val label = TextView(context).apply {
            setText(tab.label)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            maxLines = 1
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        views[tab.index] = icon to label
        return LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            minimumHeight = dp(56)
            setPadding(0, dp(6), 0, dp(4))
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(tab.label)
            val ripple = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true)
            setBackgroundResource(ripple.resourceId)
            addView(icon, LayoutParams(dp(24), dp(24)))
            addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
            setOnClickListener { onTap(tab.index) }
            cells[tab.index] = this
        }
    }

    /** Wires the bar to [activity], which is the screen for tab [tab]. */
    fun attach(activity: Activity, tab: Int) {
        host = activity
        current = tab
        paint(tab)
    }

    /** Marks a tab as current without opening anything (used when returning to a screen). */
    fun select(tab: Int, animate: Boolean = true) {
        current = tab
        paint(tab)
        if (animate) bounce(tab)
    }

    private fun onTap(index: Int) {
        val activity = host ?: return
        if (index == current) {
            onReselected?.invoke()
            return
        }
        bounce(index)
        if (index == TAB_MAP) {
            activity.startActivity(Intent(activity, MapActivity::class.java))
            return
        }
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
        val forward = position(index) > position(current)
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val (enter, exit) = if (forward != rtl) {
            R.anim.slide_in_right to R.anim.slide_out_left
        } else {
            R.anim.slide_in_left to R.anim.slide_out_right
        }
        activity.startActivity(intent, ActivityOptions.makeCustomAnimation(activity, enter, exit).toBundle())
        // A tab opened from another tab replaces it; Explore is the only root.
        if (current != TAB_EXPLORE && index != TAB_EXPLORE) activity.finish()
    }

    private fun paint(selected: Int) {
        views.forEach { (index, pair) ->
            val (icon, label) = pair
            val on = index == selected
            val color = if (on) active else inactive
            icon.imageTintList = ColorStateList.valueOf(color)
            label.setTextColor(color)
            label.paint.isFakeBoldText = on
            label.invalidate()
            cells[index]?.isSelected = on
        }
    }

    /** A small press of the icon so the change is felt, not only seen. Skipped with animations off. */
    private fun bounce(index: Int) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return
        val icon = views[index]?.first ?: return
        icon.animate().cancel()
        icon.scaleX = 0.82f
        icon.scaleY = 0.82f
        icon.animate().scaleX(1f).scaleY(1f).setDuration(260)
            .setInterpolator(android.view.animation.OvershootInterpolator(2.4f)).start()
    }

    private fun position(index: Int) = tabs.indexOfFirst { it.index == index }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val TAB_EXPLORE = 0
        const val TAB_SAVED = 1
        const val TAB_UPDATES = 2
        const val TAB_PROFILE = 3
        const val TAB_MAP = 4
    }
}
