package com.example.eastsyria.Search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemSearchResultBinding
import com.example.eastsyria.MainPage.Data.Landmark

class SearchResultAdapter(
    private var landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private var filteredLandmarks: List<Landmark> = landmarks

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            binding.apply {
                tvLandmarkName.text = landmark.name
                tvCategory.text = landmark.category.uppercase()
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
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
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

    fun filter(query: String, category: String = "All") {
        filteredLandmarks = landmarks.filter { landmark ->
            val matchesQuery = query.isEmpty() ||
                    landmark.name.contains(query, ignoreCase = true) ||
                    landmark.nameArabic.contains(query, ignoreCase = true) ||
                    landmark.location.city.contains(query, ignoreCase = true) ||
                    landmark.location.governorate.contains(query, ignoreCase = true) ||
                    landmark.description.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" ||
                    landmark.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
        notifyDataSetChanged()
    }

    fun updateData(newLandmarks: List<Landmark>) {
        landmarks = newLandmarks
        filteredLandmarks = newLandmarks
        notifyDataSetChanged()
    }

    fun getFilteredCount() = filteredLandmarks.size
}