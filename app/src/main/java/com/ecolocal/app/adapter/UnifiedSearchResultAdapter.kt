package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemSearchResultBinding
import com.ecolocal.app.model.CommunityService
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.util.ImageLoaderHelper
import com.ecolocal.app.util.LocationHelper

sealed class SearchResultItem {
    data class Marketplace(
        val listing: MarketplaceListing,
        val score: Int,
        val distanceKm: Double? = null,
        val reasons: List<String> = emptyList()
    ) : SearchResultItem()

    data class Service(
        val service: CommunityService,
        val score: Int,
        val distanceKm: Double? = null,
        val reasons: List<String> = emptyList()
    ) : SearchResultItem()
}

class UnifiedSearchResultAdapter(
    private var items: List<SearchResultItem>,
    private val onMarketplaceClick: (MarketplaceListing) -> Unit,
    private val onServiceClick: (CommunityService) -> Unit
) : RecyclerView.Adapter<UnifiedSearchResultAdapter.ViewHolder>() {

    fun updateData(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchResultItem.Marketplace -> bindMarketplace(holder, item)
            is SearchResultItem.Service -> bindService(holder, item)
        }
    }

    private fun bindMarketplace(holder: ViewHolder, item: SearchResultItem.Marketplace) {
        val listing = item.listing
        val b = holder.binding

        b.tvSearchTitle.text = listing.title
        b.tvSearchSubtitle.text = listing.price
        b.tvSearchCategory.text = listing.category

        val isGiveaway = listing.listingType.equals("GIVE AWAY", ignoreCase = true) || listing.price.equals("FREE", ignoreCase = true)
        b.tvSearchTypeBadge.text = if (isGiveaway) "GIVEAWAY" else listing.listingType
        b.tvSearchTypeBadge.setBackgroundResource(if (isGiveaway) R.drawable.bg_badge_giveaway else R.drawable.bg_badge_sale)

        // Location & Distance
        val distText = LocationHelper.formatDistance(item.distanceKm)
        val locDist = if (distText != null) "${listing.locationName} • $distText" else listing.locationName
        b.tvSearchLocationDist.text = locDist.ifBlank { "Sri Lanka" }

        // Thumbnail
        ImageLoaderHelper.load(b.ivSearchThumb, listing.imageUrl ?: listing.imageUri, listing.imageRes)

        // Smart reason
        if (item.reasons.isNotEmpty()) {
            b.tvSearchMatchReason.visibility = View.VISIBLE
            b.tvSearchMatchReason.text = "AI Smart: ${item.reasons.first()}"
        } else {
            b.tvSearchMatchReason.visibility = View.GONE
        }

        b.root.setOnClickListener { onMarketplaceClick(listing) }
    }

    private fun bindService(holder: ViewHolder, item: SearchResultItem.Service) {
        val service = item.service
        val b = holder.binding

        b.tvSearchTitle.text = service.title
        val isOffer = service.serviceType.equals("SERVICE_OFFER", ignoreCase = true)
        val rateText = if (service.price.isNotBlank()) service.price else "FREE"
        b.tvSearchSubtitle.text = if (isOffer) "$rateText • By ${service.providerName}" else "Help Requested • By ${service.providerName}"
        b.tvSearchCategory.text = service.category

        b.tvSearchTypeBadge.text = if (isOffer) "SERVICE OFFER" else "HELP REQUEST"
        b.tvSearchTypeBadge.setBackgroundResource(if (isOffer) R.drawable.bg_badge_offer else R.drawable.bg_badge_request)

        val distText = LocationHelper.formatDistance(item.distanceKm)
        val locDist = if (distText != null) "${service.locationName} • $distText" else service.locationName
        b.tvSearchLocationDist.text = locDist.ifBlank { "Sri Lanka" }

        val defaultThumb = if (isOffer) R.drawable.img_service_tutor else R.drawable.img_service_lawn
        ImageLoaderHelper.load(b.ivSearchThumb, service.imageUrl, defaultThumb)

        if (item.reasons.isNotEmpty()) {
            b.tvSearchMatchReason.visibility = View.VISIBLE
            b.tvSearchMatchReason.text = "AI Smart: ${item.reasons.first()}"
        } else {
            b.tvSearchMatchReason.visibility = View.GONE
        }

        b.root.setOnClickListener { onServiceClick(service) }
    }

    override fun getItemCount(): Int = items.size
}
