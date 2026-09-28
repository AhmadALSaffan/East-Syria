package com.example.eastsyria.Ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.eastsyria.R

/**
 * The header shared by every secondary screen: back chip, title, and up to
 * two trailing controls. It pads itself for the status bar, so a screen only
 * has to draw edge to edge.
 */
class ScreenHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val backButton: ImageButton
    val titleView: TextView
    val actionText: TextView
    val iconA: ImageButton
    val iconB: ImageButton

    private val basePaddingTop: Int

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val density = resources.displayMetrics.density
        setPadding((20 * density).toInt(), (10 * density).toInt(), (20 * density).toInt(), (10 * density).toInt())
        basePaddingTop = paddingTop

        LayoutInflater.from(context).inflate(R.layout.view_screen_header, this, true)
        backButton = findViewById(R.id.btnBack)
        titleView = findViewById(R.id.tvScreenTitle)
        actionText = findViewById(R.id.btnHeaderAction)
        iconA = findViewById(R.id.btnHeaderIconA)
        iconB = findViewById(R.id.btnHeaderIconB)

        context.obtainStyledAttributes(attrs, R.styleable.ScreenHeader).use { a ->
            titleView.text = a.getString(R.styleable.ScreenHeader_headerTitle)
            backButton.visibility =
                if (a.getBoolean(R.styleable.ScreenHeader_showBack, true)) View.VISIBLE else View.GONE
        }
        if (backButton.visibility == View.GONE) {
            (titleView.layoutParams as MarginLayoutParams).marginStart = 0
        }

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(top = basePaddingTop + bars.top)
            insets
        }
    }

    fun setTitle(text: CharSequence?) {
        titleView.text = text
    }

    fun onBack(action: () -> Unit) {
        backButton.setOnClickListener { action() }
    }

    fun setAction(label: CharSequence?, onClick: (() -> Unit)? = null) {
        if (label.isNullOrEmpty()) {
            actionText.visibility = View.GONE
            return
        }
        actionText.text = label
        actionText.setOnClickListener { onClick?.invoke() }
        actionText.visibility = View.VISIBLE
    }

    fun setIconA(@DrawableRes icon: Int, description: CharSequence, onClick: () -> Unit) =
        configureIcon(iconA, icon, description, onClick)

    fun setIconB(@DrawableRes icon: Int, description: CharSequence, onClick: () -> Unit) =
        configureIcon(iconB, icon, description, onClick)

    private fun configureIcon(
        button: ImageButton,
        @DrawableRes icon: Int,
        description: CharSequence,
        onClick: () -> Unit
    ) {
        button.setImageResource(icon)
        button.contentDescription = description
        button.setOnClickListener { onClick() }
        button.visibility = View.VISIBLE
    }
}
