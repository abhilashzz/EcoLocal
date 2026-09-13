package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemEcomatchCardBinding
import com.ecolocal.app.util.EcoMatchEngine
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.LocationHelper

class EcoMatchAdapter(
    private var recommendations: List<EcoMatchEngine.EcoMatchRecommendation>,
    private val onItemClick: (EcoMatchEngine.EcoMatchRecommendation) -> Unit
) : RecyclerView.Adapter<EcoMatchAdapter.ViewHolder>() {

    fun updateData(newItems: List<EcoMatchEngine.EcoMatchRecommendation>) {
        recommendations = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemEcomatchCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEcomatchCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recommendations[position]
        val listing = item.listing
        val b = holder.binding

        b.tvEcomatchPercent.text = "${item.matchPercentage}% Match"
        b.tvEcomatchTitle.text = listing.title
        b.tvEcomatchPrice.text = listing.price

        val dist = LocationHelper.formatDistance(item.distanceKm)
        val loc = if (dist != null) "${listing.locationName} • $dist" else listing.locationName
        b.tvEcomatchLocation.text = loc.ifBlank { "Sri Lanka" }

        b.tvEcomatchReason.text = item.primaryReason

        ImageLoaderHelper.load(
            b.ivEcomatchImage,
            listing.imageUrl ?: listing.imageUri,
            listing.imageRes
        )

        b.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = recommendations.size
}
