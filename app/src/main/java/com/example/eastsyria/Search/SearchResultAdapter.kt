package com.example.eastsyria.Search

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemSearchResultBinding

/**
 * Search rows over the published catalog. The part of a name that matches the
 * query is coloured, so a result explains why it is there.
 */
class SearchResultAdapter(
    landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private var landmarks: List<Landmark> = landmarks.filter { it.status == "published" }
    private var filteredLandmarks: List<Landmark> = this.landmarks
    private var query: String = ""

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            val context = binding.root.context
            binding.apply {
                tvLandmarkName.text = highlighted(landmark.name, context)

                val meta = joinMeta(landmark)
                tvMeta.text = meta
                tvMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

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
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(ivLandmark)

                root.setOnClickListener { onItemClick(landmark) }
            }
        }

        private fun joinMeta(landmark: Landmark): String {
            val context = binding.root.context
            val category = landmark.category.trim()
            val city = landmark.location.city.trim().ifEmpty { landmark.location.governorate.trim() }
            return when {
                category.isNotEmpty() && city.isNotEmpty() ->
                    context.getString(R.string.home_meta_separator, category, city)

                category.isNotEmpty() -> category
                else -> city
            }
        }

        private fun highlighted(name: String, context: android.content.Context): CharSequence {
            val q = query.trim()
            if (q.isEmpty()) return name
            val start = name.indexOf(q, ignoreCase = true)
            if (start < 0) return name
            return SpannableString(name).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.terracotta_soft)),
                    start,
                    start + q.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
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

    /** [category] null means every category. */
    fun filter(query: String, category: String? = null) {
        this.query = query
        val q = query.trim()
        filteredLandmarks = landmarks.filter { landmark ->
            val matchesQuery = q.isEmpty() ||
                    landmark.name.contains(q, ignoreCase = true) ||
                    landmark.nameArabic.contains(q, ignoreCase = true) ||
                    landmark.location.city.contains(q, ignoreCase = true) ||
                    landmark.location.governorate.contains(q, ignoreCase = true) ||
                    landmark.description.contains(q, ignoreCase = true)

            val matchesCategory = category == null ||
                    landmark.category.trim().equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
        notifyDataSetChanged()
    }

    fun updateData(newLandmarks: List<Landmark>) {
        landmarks = newLandmarks.filter { it.status == "published" }
        filteredLandmarks = landmarks
        query = ""
        notifyDataSetChanged()
    }

    /** The categories that actually exist in the catalog, most common first. */
    fun categories(): List<String> = landmarks
        .map { it.category.trim() }
        .filter { it.isNotEmpty() }
        .groupBy { it.lowercase() }
        .values
        .sortedByDescending { it.size }
        .map { it.first() }

    fun getFilteredCount() = filteredLandmarks.size
}
