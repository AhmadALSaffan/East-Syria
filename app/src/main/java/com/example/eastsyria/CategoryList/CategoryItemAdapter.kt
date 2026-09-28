package com.example.eastsyria.CategoryList

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemSearchResultBinding

/**
 * The category rows: the shared landmark row with a short description and a
 * bookmark. Which places are saved is handed in as a set by the screen, which
 * reads it once and keeps it live; the adapter never goes to the network.
 *
 * With no [onBookmarkClick] (the category search) the bookmark is hidden and
 * the chevron stays.
 */
class CategoryItemAdapter(
    items: List<CategoryItem>,
    private val onItemClick: (CategoryItem) -> Unit,
    private val onBookmarkClick: ((CategoryItem) -> Unit)? = null
) : RecyclerView.Adapter<CategoryItemAdapter.ItemViewHolder>() {

    private var items = items.filter { it.status == "published" }
    private var savedIds: Set<String> = emptySet()

    inner class ItemViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            val context = binding.root.context
            binding.apply {
                tvLandmarkName.text = item.name

                val place = item.location.city.trim().ifEmpty { item.location.governorate.trim() }
                // The category, or UNESCO where the record says so. The publish state
                // ("published") is a curator's concern and means nothing to a visitor.
                val badge = if (item.getBadgeText().startsWith("UNESCO")) {
                    context.getString(R.string.category_unesco)
                } else {
                    item.category.trim()
                }
                val meta = when {
                    badge.isNotEmpty() && place.isNotEmpty() ->
                        context.getString(R.string.home_meta_separator, badge, place)

                    badge.isNotEmpty() -> badge
                    else -> place
                }
                tvMeta.text = meta
                tvMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

                val description = item.description.trim()
                tvDescription.text = description
                tvDescription.visibility = if (description.isEmpty()) View.GONE else View.VISIBLE

                if (item.rating > 0.0) {
                    val rating = String.format("%.1f", item.rating)
                    tvRating.text = rating
                    layoutRating.contentDescription = context.getString(R.string.home_rating_desc, rating)
                    layoutRating.visibility = View.VISIBLE
                } else {
                    layoutRating.visibility = View.GONE
                }

                Glide.with(context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder_dark)
                    .error(R.drawable.placeholder_dark)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(ivLandmark)

                if (onBookmarkClick != null) {
                    val saved = item.id in savedIds
                    ivChevron.visibility = View.GONE
                    btnBookmark.visibility = View.VISIBLE
                    btnBookmark.setImageResource(
                        if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
                    )
                    btnBookmark.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            if (saved) R.color.terracotta_soft else R.color.ivory
                        )
                    )
                    btnBookmark.contentDescription = context.getString(
                        if (saved) R.string.detail_unbookmark else R.string.detail_bookmark
                    )
                    btnBookmark.setOnClickListener { onBookmarkClick.invoke(item) }
                } else {
                    ivChevron.visibility = View.VISIBLE
                    btnBookmark.visibility = View.GONE
                }

                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CategoryItem>) {
        items = newItems.filter { it.status == "published" }
        notifyDataSetChanged()
    }

    fun setSavedIds(ids: Set<String>) {
        savedIds = ids
        notifyDataSetChanged()
    }

    fun filterItems(query: String): List<CategoryItem> {
        return if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.location.city.contains(query, ignoreCase = true) ||
                        it.location.governorate.contains(query, ignoreCase = true)
            }
        }
    }
}
