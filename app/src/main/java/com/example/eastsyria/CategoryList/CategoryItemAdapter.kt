import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.CategoryList.CategoryItem
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemCategoryCardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoryItemAdapter(
    private var items: List<CategoryItem>,
    private val onItemClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryItemAdapter.ItemViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    inner class ItemViewHolder(private val binding: ItemCategoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            binding.apply {
                tvItemName.text = item.name
                tvDescription.text = item.description

                // Load image
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_site)
                    .error(R.drawable.placeholder_site)
                    .into(ivItemImage)

                // Rating
                if (item.rating > 0.0) {
                    layoutRating.visibility = View.VISIBLE
                    tvRating.text = item.rating.toString()
                } else {
                    layoutRating.visibility = View.GONE
                }

                // Location
                if (item.location.governorate.isNotEmpty()) {
                    layoutLocation.visibility = View.VISIBLE
                    tvLocation.text = item.location.governorate
                } else {
                    layoutLocation.visibility = View.GONE
                }

                // Badge
                val badgeText = item.getBadgeText()
                if (badgeText.isNotEmpty()) {
                    tvBadge.visibility = View.VISIBLE
                    tvBadge.text = badgeText
                } else {
                    tvBadge.visibility = View.GONE
                }

                // Check bookmark status from Firebase
                checkIfSaved(item, binding)

                // Click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                btnBookmark.setOnClickListener {
                    toggleBookmark(item, binding)
                }

                btnViewDetails.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun checkIfSaved(item: CategoryItem, binding: ItemCategoryCardBinding) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                updateBookmarkUI(binding, false)
                return
            }

            if (item.id.isEmpty()) {
                updateBookmarkUI(binding, false)
                return
            }

            database.reference
                .child("users")
                .child(userId)
                .child("savedLandmarks")
                .child(item.id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isSaved = snapshot.exists()
                        item.isBookmarked = isSaved
                        updateBookmarkUI(binding, isSaved)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        updateBookmarkUI(binding, false)
                    }
                })
        }

        private fun toggleBookmark(item: CategoryItem, binding: ItemCategoryCardBinding) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(
                    binding.root.context,
                    "Please log in to save landmarks",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (item.id.isEmpty()) {
                Toast.makeText(
                    binding.root.context,
                    "Error: Invalid landmark ID",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val savedRef = database.reference
                .child("users")
                .child(userId)
                .child("savedLandmarks")
                .child(item.id)

            if (item.isBookmarked) {
                // Remove from saved
                savedRef.removeValue()
                    .addOnSuccessListener {
                        item.isBookmarked = false
                        updateBookmarkUI(binding, false)
                        Toast.makeText(
                            binding.root.context,
                            "Removed from saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            binding.root.context,
                            "Failed to remove: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // Add to saved
                val saveData = mapOf(
                    "savedAt" to System.currentTimeMillis(),
                    "landmarkId" to item.id,
                    "name" to item.name,
                    "imageUrl" to item.imageUrl,
                    "category" to item.category
                )

                savedRef.setValue(saveData)
                    .addOnSuccessListener {
                        item.isBookmarked = true
                        updateBookmarkUI(binding, true)
                        Toast.makeText(
                            binding.root.context,
                            "Saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            binding.root.context,
                            "Failed to save: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        private fun updateBookmarkUI(binding: ItemCategoryCardBinding, saved: Boolean) {
            if (saved) {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.orange_accent)
                )
            } else {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_border)
                binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCategoryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CategoryItem>) {
        items = newItems
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