package com.example.eastsyria.Admin.Reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        holder.binding.tvViews.text = formatViews(item.views)
        holder.binding.tvRating.text = String.format("%.1f", item.rating)
        holder.binding.tvGrowth.text = item.growth

        if (item.growth.startsWith("-")) {
            holder.binding.tvGrowth.setTextColor(
                holder.itemView.context.resources.getColor(android.R.color.holo_red_light, null)
            )
        } else {
            holder.binding.tvGrowth.setTextColor(
                holder.itemView.context.resources.getColor(R.color.green, null)
            )
        }

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_landmark_placeholder)
            .centerCrop()
            .into(holder.binding.ivLandmarkImage)
    }

    override fun getItemCount(): Int = items.size

    private fun formatViews(views: Long): String {
        return when {
            views >= 1000 -> String.format("%.1fk views", views / 1000.0)
            else -> "$views views"
        }
    }
}