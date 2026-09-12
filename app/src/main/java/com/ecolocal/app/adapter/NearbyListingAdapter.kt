package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemNearbyListingBinding
import com.ecolocal.app.model.NearbyListing

class NearbyListingAdapter(
    private var listings: List<NearbyListing>,
    private val onItemClick: (NearbyListing) -> Unit = {}
) : RecyclerView.Adapter<NearbyListingAdapter.ViewHolder>() {

    fun updateData(newListings: List<NearbyListing>) {
        listings = newListings
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemNearbyListingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listings[position]
        val context = holder.itemView.context

        holder.binding.tvListingTitle.text = item.title
        holder.binding.tvListingDesc.text = item.description
        com.ecolocal.app.util.ImageLoaderHelper.load(
            holder.binding.ivListingImage,
            item.imageUri,
            item.imageRes
        )

        // Status chip
        holder.binding.tvStatusChip.text = item.statusText
        if (item.isStatusAvailable) {
            holder.binding.tvStatusChip.setBackgroundResource(R.drawable.bg_badge_available)
            holder.binding.tvStatusChip.setTextColor(
                ContextCompat.getColor(context, R.color.eco_badge_available_text)
            )
        } else {
            holder.binding.tvStatusChip.setBackgroundResource(R.drawable.bg_badge_sale)
            holder.binding.tvStatusChip.setTextColor(
                ContextCompat.getColor(context, R.color.eco_badge_sale_text)
            )
        }

        // Price formatting
        holder.binding.tvListingPrice.text = item.price
        if (item.isPriceOrange) {
            holder.binding.tvListingPrice.setTextColor(
                ContextCompat.getColor(context, R.color.eco_price_orange)
            )
        } else {
            holder.binding.tvListingPrice.setTextColor(
                ContextCompat.getColor(context, R.color.eco_price_green)
            )
        }

        // Location
        if (!item.location.isNullOrEmpty()) {
            holder.binding.layoutLocation.visibility = View.VISIBLE
            holder.binding.tvListingLocation.text = item.location
        } else {
            holder.binding.layoutLocation.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = listings.size
}
