package com.example.eastsyria.Admin.Data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ItemAdminLandmarkBinding

class AdminLandmarkAdapter(
    private var list: List<LandmarkAdminModel>,
    private val onEdit: (LandmarkAdminModel) -> Unit,
    private val onDelete: (LandmarkAdminModel) -> Unit
) : RecyclerView.Adapter<AdminLandmarkAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminLandmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminLandmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvCity.text = item.location.city.ifEmpty { "Unknown city" }

        val status = item.status.ifEmpty { "unknown" }
        holder.binding.tvStatus.text = status.uppercase()

        when (status.lowercase()) {
            "published" -> {
                holder.binding.tvStatus.setTextColor(holder.binding.root.context.getColor(R.color.green))
                holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_published)
            }
            "pending" -> {
                holder.binding.tvStatus.setTextColor(holder.binding.root.context.getColor(R.color.orange))
                holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
            else -> {
                holder.binding.tvStatus.setTextColor(holder.binding.root.context.getColor(R.color.white))
                holder.binding.tvStatus.background = null
            }
        }

        Glide.with(holder.binding.root.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.binding.ivLandmark)

        holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<LandmarkAdminModel>) {
        list = newList
        notifyDataSetChanged()
    }
}
