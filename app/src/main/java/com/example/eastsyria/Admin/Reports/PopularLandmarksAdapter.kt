package com.example.eastsyria.Admin.Reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemPopularLandmarkBinding

class PopularLandmarksAdapter(private val items: List<PopularLandmarkItem>) :
    RecyclerView.Adapter<PopularLandmarksAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPopularLandmarkBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPopularLandmarkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLandmarkName.text = item.name
        holder.binding.tvCategory.text = item.category
        holder.binding.tvCategory.visibility = if (item.category.isBlank()) View.GONE else View.VISIBLE
        holder.binding.tvRating.text = String.format("%.1f", item.rating)

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_dark)
            .error(R.drawable.placeholder_dark)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(holder.binding.ivLandmarkImage)
    }

    override fun getItemCount(): Int = items.size
}
