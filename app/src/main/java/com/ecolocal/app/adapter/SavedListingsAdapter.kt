package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemSavedListingBinding
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.util.ImageLoaderHelper

class SavedListingsAdapter(
    private var listings: List<MarketplaceListing>,
    private val onItemClick: (MarketplaceListing) -> Unit,
    private val onBookmarkToggle: (MarketplaceListing) -> Unit
) : RecyclerView.Adapter<SavedListingsAdapter.ViewHolder>() {

    fun updateData(newListings: List<MarketplaceListing>) {
        listings = newListings
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemSavedListingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listings.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listings[position]

        holder.binding.tvSavedTitle.text = item.title
        holder.binding.tvSavedPrice.text = item.price
        holder.binding.tvSavedLocation.text = item.location
        holder.binding.tvSavedBadge.text = item.listingType
        holder.binding.tvSavedCondition.text = "• ${item.condition} Condition"

        ImageLoaderHelper.load(
            holder.binding.ivSavedThumbnail,
            item.imageUri,
            item.imageRes
        )

        holder.binding.cardSavedItem.setOnClickListener { onItemClick(item) }
        holder.binding.btnSavedBookmarkToggle.setOnClickListener { onBookmarkToggle(item) }
    }
}
