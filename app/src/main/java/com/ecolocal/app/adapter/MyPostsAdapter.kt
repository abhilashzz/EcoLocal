package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemMyPostBinding
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.util.ImageLoaderHelper

class MyPostsAdapter(
    private var posts: List<MarketplaceListing>,
    private val onItemClick: (MarketplaceListing) -> Unit,
    private val onEditClick: (MarketplaceListing) -> Unit,
    private val onDeleteClick: (MarketplaceListing) -> Unit
) : RecyclerView.Adapter<MyPostsAdapter.ViewHolder>() {

    fun updateData(newPosts: List<MarketplaceListing>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMyPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = posts[position]
        val context = holder.itemView.context

        holder.binding.tvMyPostTitle.text = item.title
        holder.binding.tvMyPostPrice.text = item.price
        holder.binding.tvMyPostLocation.text = item.location
        holder.binding.tvMyPostBadge.text = item.listingType

        if (item.isAvailable) {
            holder.binding.tvMyPostStatus.text = "Active"
            holder.binding.tvMyPostStatus.setBackgroundResource(R.drawable.bg_badge_available)
            holder.binding.tvMyPostStatus.setTextColor(ContextCompat.getColor(context, R.color.eco_green_primary))
        } else {
            holder.binding.tvMyPostStatus.text = "Inactive"
            holder.binding.tvMyPostStatus.setBackgroundResource(R.drawable.bg_condition_chip_unselected)
            holder.binding.tvMyPostStatus.setTextColor(ContextCompat.getColor(context, R.color.eco_secondary_dark))
        }

        ImageLoaderHelper.load(
            holder.binding.ivMyPostThumbnail,
            item.imageUri,
            item.imageRes
        )

        holder.binding.cardMyPost.setOnClickListener { onItemClick(item) }
        holder.binding.btnMyPostEdit.setOnClickListener { onEditClick(item) }
        holder.binding.btnMyPostDelete.setOnClickListener { onDeleteClick(item) }
    }
}
