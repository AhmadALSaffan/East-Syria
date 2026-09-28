package com.example.eastsyria.Saved

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
 * Saved rows: the shared landmark row with a filled bookmark where the chevron
 * would be. The list is already newest-first when it arrives.
 */
class SavedLandmarkAdapter(
    private var landmarks: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit,
    private val onBookmarkClick: (Landmark) -> Unit
) : RecyclerView.Adapter<SavedLandmarkAdapter.ViewHolder>() {

    private var filteredLandmarks: List<Landmark> = landmarks
    private var query: String = ""

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            val context = binding.root.context
            binding.apply {
                tvLandmarkName.text = landmark.name

                val category = landmark.category.trim()
                val city = landmark.location.city.trim().ifEmpty { landmark.location.governorate.trim() }
                val meta = when {
                    category.isNotEmpty() && city.isNotEmpty() ->
                        context.getString(R.string.home_meta_separator, category, city)

                    category.isNotEmpty() -> category
                    else -> city
                }
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

                ivChevron.visibility = View.GONE
                btnBookmark.visibility = View.VISIBLE
                btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                btnBookmark.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.terracotta_soft)
                )
                btnBookmark.contentDescription =
                    context.getString(R.string.saved_remove_desc, landmark.name)

                Glide.with(context)
                    .load(landmark.imageUrl)
                    .placeholder(R.drawable.placeholder_dark)
                    .error(R.drawable.placeholder_dark)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(ivLandmark)

                root.setOnClickListener { onItemClick(landmark) }
                btnBookmark.setOnClickListener { onBookmarkClick(landmark) }
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

    fun filter(query: String) {
        this.query = query
        val q = query.trim()
        filteredLandmarks = if (q.isEmpty()) {
            landmarks
        } else {
            landmarks.filter { landmark ->
                landmark.name.contains(q, ignoreCase = true) ||
                        landmark.nameArabic.contains(q, ignoreCase = true) ||
                        landmark.location.city.contains(q, ignoreCase = true) ||
                        landmark.location.governorate.contains(q, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newLandmarks: List<Landmark>) {
        landmarks = newLandmarks
        filter(query)
    }

    fun getFilteredCount() = filteredLandmarks.size
}
