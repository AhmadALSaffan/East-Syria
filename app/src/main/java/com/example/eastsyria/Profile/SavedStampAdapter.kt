package com.example.eastsyria.Profile

import com.example.eastsyria.Ui.localName
import com.example.eastsyria.Ui.localCity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemSavedStampBinding

/**
 * The places this user actually bookmarked, as portrait photographs.
 *
 * The data was already there — users/{uid}/savedLandmarks, one entry per place
 * with a savedAt timestamp — and the profile screen had never read it.
 */
class SavedStampAdapter(
    private val stamps: List<Landmark>,
    private val onItemClick: (Landmark) -> Unit
) : RecyclerView.Adapter<SavedStampAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSavedStampBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(landmark: Landmark) {
            binding.apply {
                tvStampName.text = landmark.localName(root.context)

                val place = landmark.localCity(root.context)
                    .ifEmpty { landmark.location.governorate.trim() }
                tvStampPlace.text = place
                tvStampPlace.visibility = if (place.isEmpty()) View.GONE else View.VISIBLE

                Glide.with(root.context)
                    .load(landmark.imageUrl)
                    .placeholder(R.drawable.placeholder_dark)
                    .error(R.drawable.placeholder_dark)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(ivStamp)

                root.contentDescription = landmark.localName(root.context)
                root.setOnClickListener { onItemClick(landmark) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedStampBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.root.isClickable = true
        binding.root.isFocusable = true
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(stamps[position])
    }

    override fun getItemCount() = stamps.size
}
