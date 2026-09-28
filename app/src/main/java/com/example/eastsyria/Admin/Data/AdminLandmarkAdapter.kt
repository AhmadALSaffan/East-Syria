package com.example.eastsyria.Admin.Data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemAdminLandmarkBinding

/**
 * The curator's list. The whole row opens the editor; delete is its own
 * control and always asks first (that is the screen's job, not the row's).
 */
class AdminLandmarkAdapter(
    private var list: List<LandmarkAdminModel>,
    private val onEdit: (LandmarkAdminModel) -> Unit,
    private val onDelete: (LandmarkAdminModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminLandmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminLandmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as ViewHolder).binding
        val context = binding.root.context
        val item = list[position]

        binding.tvName.text = item.name
        binding.tvCity.text = item.location.city.ifEmpty { context.getString(R.string.admin_unknown_city) }

        when (item.status.lowercase()) {
            "published" -> {
                binding.tvStatus.setText(R.string.admin_status_published)
                binding.tvStatus.setTextColor(context.getColor(R.color.green))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_published)
            }

            "pending" -> {
                binding.tvStatus.setText(R.string.admin_status_pending)
                binding.tvStatus.setTextColor(context.getColor(R.color.gold))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }

            else -> {
                binding.tvStatus.setText(R.string.admin_status_unknown)
                binding.tvStatus.setTextColor(context.getColor(R.color.ivory_mute))
                binding.tvStatus.background = null
            }
        }

        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_dark)
            .error(R.drawable.placeholder_dark)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(binding.ivLandmark)

        binding.btnDelete.contentDescription = context.getString(R.string.admin_delete) + ": " + item.name
        binding.btnEdit.contentDescription = context.getString(R.string.admin_edit) + ": " + item.name
        binding.rowContent.setOnClickListener { onEdit(item) }
        binding.btnEdit.setOnClickListener { onEdit(item) }
        binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<LandmarkAdminModel>) {
        list = newList
        notifyDataSetChanged()
    }
}
