package com.example.eastsyria.Onboarding

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemOnboardingPageBinding

/**
 * How a piece of drawn art sits over a stop's photograph. All sizes in dp.
 */
data class ArtSpec(
    @DrawableRes val res: Int,
    val widthDp: Int,
    val heightDp: Int,
    val gravity: Int,
    val marginStartDp: Int = 0,
    val marginTopDp: Int = 0,
    val marginEndDp: Int = 0,
    val marginBottomDp: Int = 0,
    val rotation: Float = 0f,
    val alpha: Float = 1f
)

/**
 * One stop of the onboarding: a photograph from the catalog, a piece of drawn
 * art over it (or the wall label, for the stop about bilingual records), and
 * the words.
 */
data class Stop(
    val titleRes: Int,
    val bodyRes: Int,
    @DrawableRes val photo: Int,
    val art: ArtSpec? = null,
    val showPlate: Boolean = false
)

val approachStops = listOf(
    Stop(
        titleRes = R.string.onboarding_1_title,
        bodyRes = R.string.onboarding_1_body,
        photo = R.drawable.onb_palmyra,
        art = ArtSpec(
            res = R.drawable.art_onb_arch,
            widthDp = 300, heightDp = 390,
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            marginTopDp = 70
        )
    ),
    Stop(
        titleRes = R.string.onboarding_2_title,
        bodyRes = R.string.onboarding_2_body,
        photo = R.drawable.onb_tell_halaf,
        showPlate = true
    ),
    Stop(
        titleRes = R.string.onboarding_3_title,
        bodyRes = R.string.onboarding_3_body,
        photo = R.drawable.onb_qalaat_jabar,
        art = ArtSpec(
            res = R.drawable.art_onb_route,
            widthDp = 320, heightDp = 240,
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            marginBottomDp = 56
        )
    ),
    Stop(
        titleRes = R.string.onboarding_4_title,
        bodyRes = R.string.onboarding_4_body,
        photo = R.drawable.onb_dura_europos,
        art = ArtSpec(
            res = R.drawable.art_onb_stamp,
            widthDp = 150, heightDp = 150,
            gravity = Gravity.TOP or Gravity.END,
            marginTopDp = 118, marginEndDp = 28,
            rotation = -12f, alpha = 0.95f
        )
    )
)

class OnboardingAdapter(
    private val stops: List<Stop> = approachStops
) : RecyclerView.Adapter<OnboardingAdapter.StopViewHolder>() {

    inner class StopViewHolder(
        val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stops[position]
        val density = holder.binding.root.resources.displayMetrics.density
        holder.binding.apply {
            tvTitle.setText(stop.titleRes)
            tvBody.setText(stop.bodyRes)
            layerPhoto.setImageResource(stop.photo)

            plate.visibility = if (stop.showPlate) View.VISIBLE else View.GONE

            val art = stop.art
            if (art == null) {
                layerArt.visibility = View.GONE
            } else {
                layerArt.setImageResource(art.res)
                layerArt.rotation = art.rotation
                layerArt.alpha = art.alpha
                layerArt.layoutParams = FrameLayout.LayoutParams(
                    (art.widthDp * density).toInt(),
                    (art.heightDp * density).toInt(),
                    art.gravity
                ).apply {
                    marginStart = (art.marginStartDp * density).toInt()
                    topMargin = (art.marginTopDp * density).toInt()
                    marginEnd = (art.marginEndDp * density).toInt()
                    bottomMargin = (art.marginBottomDp * density).toInt()
                }
                layerArt.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = stops.size
}
