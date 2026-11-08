package com.example.eastsyria.Categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.CategoryList.CategoryRules
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemCategoryTileBinding

/** What the catalog says about one category once it has been read. */
data class CategoryStats(val count: Int, val imageUrl: String)

/**
 * The category tiles. Names and icons are known up front, so the grid is
 * complete on the first frame; [updateStats] fills in the counts and photos.
 */
class CategoryTileAdapter(
    private val onClick: (CategoryRules.Def) -> Unit
) : RecyclerView.Adapter<CategoryTileAdapter.ViewHolder>() {

    private val defs = CategoryRules.all
    private var stats: Map<String, CategoryStats> = emptyMap()

    inner class ViewHolder(private val binding: ItemCategoryTileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(def: CategoryRules.Def) {
            val context = binding.root.context
            binding.tvTileName.setText(def.name)
            binding.ivTileIcon.setImageResource(def.icon)

            val stat = stats[def.type]
            if (stat == null) {
                binding.tvTileCount.visibility = View.GONE
                binding.ivTilePhoto.alpha = 0f
                Glide.with(context).clear(binding.ivTilePhoto)
            } else {
                binding.tvTileCount.text =
                    context.resources.getQuantityString(R.plurals.categories_place_count, stat.count, stat.count)
                binding.tvTileCount.visibility = View.VISIBLE
                if (stat.imageUrl.isNotBlank()) {
                    binding.ivTilePhoto.alpha = 1f
                    Glide.with(context)
                        .load(stat.imageUrl)
                        .centerCrop()
                        .error(R.drawable.placeholder_dark)
                        .transition(DrawableTransitionOptions.withCrossFade(250))
                        .into(binding.ivTilePhoto)
                }
            }

            binding.cardTile.setOnClickListener { onClick(def) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(defs[position])

    override fun getItemCount() = defs.size

    fun updateStats(newStats: Map<String, CategoryStats>) {
        stats = newStats
        notifyDataSetChanged()
    }
}
