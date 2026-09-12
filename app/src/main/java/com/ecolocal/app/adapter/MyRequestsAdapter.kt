package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemMyRequestBinding
import com.ecolocal.app.model.entity.RequestEntity
import com.ecolocal.app.util.ImageLoaderHelper

class MyRequestsAdapter(
    private var requests: List<RequestEntity>,
    private val onItemClick: (RequestEntity) -> Unit
) : RecyclerView.Adapter<MyRequestsAdapter.ViewHolder>() {

    fun updateData(newRequests: List<RequestEntity>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMyRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = requests[position]
        val context = holder.itemView.context

        holder.binding.tvRequestTitle.text = item.listingTitle
        holder.binding.tvRequestOwner.text = "To: ${item.ownerName}"
        holder.binding.tvRequestLocation.text = item.location
        holder.binding.tvRequestPriceOrInfo.text = item.priceOrInfo
        holder.binding.tvRequestStatus.text = item.status.lowercase().replaceFirstChar { it.uppercase() }

        if (item.requestType == "MARKETPLACE_INTEREST") {
            holder.binding.tvRequestTypeBadge.text = "MARKETPLACE"
            holder.binding.tvRequestTypeBadge.setBackgroundResource(R.drawable.bg_badge_sale)
            holder.binding.tvRequestTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.eco_green_primary))
        } else {
            holder.binding.tvRequestTypeBadge.text = "COMMUNITY HELP"
            holder.binding.tvRequestTypeBadge.setBackgroundResource(R.drawable.bg_badge_request)
            holder.binding.tvRequestTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.eco_orange))
        }

        val fallbackDrawable = if (item.requestType == "MARKETPLACE_INTEREST") {
            R.drawable.img_mkt_desk
        } else {
            R.drawable.img_service_lawn
        }

        ImageLoaderHelper.load(
            holder.binding.ivRequestThumbnail,
            item.listingImageUri,
            if (item.listingImageRes != 0) item.listingImageRes else fallbackDrawable
        )

        holder.binding.cardMyRequest.setOnClickListener { onItemClick(item) }
    }
}
