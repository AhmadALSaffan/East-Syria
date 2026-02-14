package com.example.eastsyria.Saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemSavedLandmarkBinding
import com.example.eastsyria.MainPage.Data.Landmark

class SavedLandmarkAdapter(
    private var landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit,
    private val onBookmarkClick: (Landmark) -> Unit
) : RecyclerView.Adapter<SavedLandmarkAdapter.ViewHolder>() {

    private var filteredLandmarks: List<Landmark> = landmarks

    inner class ViewHolder(private val binding: ItemSavedLandmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            binding.apply {
                tvLandmarkName.text = landmark.name
                tvLocation.text = "${landmark.location.city}, ${landmark.location.governorate}"

                Glide.with(binding.root.context)
                    .load(landmark.imageUrl)
                    .placeholder(R.drawable.placeholder_destination)
                    .error(R.drawable.placeholder_destination)
                    .centerCrop()
                    .into(ivLandmark)

                root.setOnClickListener {
                    onItemClick(landmark)
                }

                btnBookmark.setOnClickListener {
                    onBookmarkClick(landmark)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedLandmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredLandmarks[position])
    }

    override fun getItemCount() = filteredLandmarks.size

    fun filter(query: String) {
        filteredLandmarks = if (query.isEmpty()) {
            landmarks
        } else {
            landmarks.filter { landmark ->
                landmark.name.contains(query, ignoreCase = true) ||
                        landmark.location.city.contains(query, ignoreCase = true) ||
                        landmark.location.governorate.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newLandmarks: List<Landmark>) {
        landmarks = newLandmarks
        filteredLandmarks = newLandmarks
        notifyDataSetChanged()
    }
}