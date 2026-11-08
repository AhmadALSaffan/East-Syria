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
import com.example.eastsyria.databinding.ItemTrendingDestinationBinding

/**
 * Trending cards: the photograph is the card, the name and place sit on a
 * scrim at its foot.
 *
 * The review count this once rendered ("(1.2k)") was social proof the product
 * has not earned — nothing in the app can author a review. The place is shown
 * instead, which is real and more use to someone deciding whether to go. The
 * rating stays as a curator's mark and hides entirely when it is unset rather
 * than showing a hollow 0.0.
 */
class TrendingDestinationAdapter(
    private val destinations: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<TrendingDestinationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTrendingDestinationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            val context = binding.root.context
            binding.apply {
                tvLandmarkName.text = landmark.localName(context)

                val place = formatPlace(landmark)
                tvLocation.text = place
                tvLocation.visibility = if (place.isEmpty()) View.GONE else View.VISIBLE

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

                cardContent.setOnClickListener { onItemClick(landmark) }
            }
        }

        private fun formatPlace(landmark: Landmark): String {
            val context = binding.root.context
            val city = landmark.localCity(context)
            val governorate = landmark.localGovernorate(context)
            // The city alone reads cleaner on a small card; the governorate only when there is no city.
            return when {

                city.isNotEmpty() -> city
                else -> governorate
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrendingDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(destinations[position])
    }

    override fun getItemCount() = destinations.size
}
