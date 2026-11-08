package com.example.eastsyria.MainPage.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.Ui.localCategory
import com.example.eastsyria.Ui.localCity
import com.example.eastsyria.Ui.localGovernorate
import com.example.eastsyria.Ui.localName
import com.example.eastsyria.Ui.otherName
import com.example.eastsyria.databinding.ItemFeaturedLandmarkBinding

/**
 * Hero pages: one featured landmark per full-width page. The photograph is the
 * page; the name (in both scripts when the record has both), the place and a
 * single action sit on the scrim at its foot. The whole page is the target,
 * and the button is the same target made visible.
 */
class FeaturedLandmarkAdapter(
    private val landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<FeaturedLandmarkAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemFeaturedLandmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            val context = binding.root.context
            binding.apply {
                tvLandmarkName.text = landmark.localName(context)

                // The name in the other script, small, above the main one.
                val other = landmark.otherName(context)
                tvNameArabic.text = other
                tvNameArabic.visibility = if (other.isEmpty()) View.GONE else View.VISIBLE

                tvMeta.text = joinMeta(landmark.localCategory(context), landmark.localCity(context))
                tvMeta.visibility = if (tvMeta.text.isEmpty()) View.GONE else View.VISIBLE

                if (landmark.rating > 0.0) {
                    val rating = String.format("%.1f", landmark.rating)
                    tvRating.text = rating
                    layoutRating.contentDescription = context.getString(R.string.home_rating_desc, rating)
                    layoutRating.visibility = View.VISIBLE
                } else {
                    layoutRating.visibility = View.GONE
                }

                Glide.with(context)
                    .load(landmark.imageUrl)
                    .placeholder(R.drawable.placeholder_dark)
                    .error(R.drawable.placeholder_dark)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(250))
                    .into(ivLandmark)

                root.contentDescription = null
                root.setOnClickListener { onItemClick(landmark) }
                btnView.setOnClickListener { onItemClick(landmark) }
            }
        }

        private fun joinMeta(category: String, city: String): String = when {
            category.isNotEmpty() && city.isNotEmpty() ->
                binding.root.context.getString(R.string.home_meta_separator, category, city)

            category.isNotEmpty() -> category
            else -> city
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeaturedLandmarkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(landmarks[position])
    }

    override fun getItemCount() = landmarks.size
}
