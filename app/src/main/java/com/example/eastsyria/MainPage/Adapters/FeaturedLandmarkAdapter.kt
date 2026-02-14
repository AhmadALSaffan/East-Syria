package com.example.eastsyria.MainPage.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemFeaturedLandmarkBinding
import com.example.eastsyria.MainPage.Data.Landmark

class FeaturedLandmarkAdapter(
    private val landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<FeaturedLandmarkAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemFeaturedLandmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            binding.apply {
                tvCategory.text = landmark.category.uppercase()
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


                btnViewDetails.setOnClickListener {
                    onItemClick(landmark)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeaturedLandmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(landmarks[position])
    }

    override fun getItemCount() = landmarks.size
}
