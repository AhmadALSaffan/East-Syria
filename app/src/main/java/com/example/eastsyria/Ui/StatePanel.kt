package com.example.eastsyria.Ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.eastsyria.R

/**
 * A centred message for the moments a list has nothing to show: empty, failed,
 * or signed out. It carries a title, a line of explanation and, optionally,
 * one action.
 */
class StatePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val title: TextView
    private val body: TextView
    private val action: TextView

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        val gutter = (32 * resources.displayMetrics.density).toInt()
        setPadding(gutter, paddingTop, gutter, paddingBottom)
        LayoutInflater.from(context).inflate(R.layout.view_state_panel, this, true)
        title = findViewById(R.id.tvStateTitle)
        body = findViewById(R.id.tvStateBody)
        action = findViewById(R.id.btnStateAction)
        visibility = GONE
    }

    fun show(
        titleText: CharSequence,
        bodyText: CharSequence? = null,
        actionLabel: CharSequence? = null,
        onAction: (() -> Unit)? = null
    ) {
        title.text = titleText
        body.text = bodyText
        body.visibility = if (bodyText.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (actionLabel != null && onAction != null) {
            action.text = actionLabel
            action.setOnClickListener { onAction() }
            action.visibility = View.VISIBLE
        } else {
            action.visibility = View.GONE
        }
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
