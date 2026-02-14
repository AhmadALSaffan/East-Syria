package com.example.eastsyria.MainPage.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemTrendingDestinationBinding
import com.example.eastsyria.MainPage.Data.Landmark

class TrendingDestinationAdapter(
    private val destinations: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<TrendingDestinationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTrendingDestinationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            binding.apply {
                tvRating.text = landmark.rating.toString()
                tvReviewCount.text = "(${formatReviewCount(landmark.reviewCount)})"
                tvLandmarkName.text = landmark.name
                tvDescription.text = landmark.description

                Glide.with(binding.root.context)
                    .load(landmark.imageUrl)
                    .placeholder(R.drawable.placeholder_destination)
                    .error(R.drawable.placeholder_destination)
                    .centerCrop()
                    .into(ivLandmark)

                root.setOnClickListener {
                    onItemClick(landmark)
                }
            }
        }

        private fun formatReviewCount(count: Int): String {
            return when {
                count >= 1000 -> String.format("%.1fk", count / 1000.0)
                else -> count.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrendingDestinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(destinations[position])
    }

    override fun getItemCount() = destinations.size
}
