package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemHelpRequestBinding
import com.ecolocal.app.databinding.ItemServiceOfferBinding
import com.ecolocal.app.model.CommunityServiceItem

class CommunityServicesAdapter(
    private var items: List<CommunityServiceItem>,
    private val onItemClick: (CommunityServiceItem) -> Unit = {},
    private val onOfferHelpClick: (CommunityServiceItem.Request) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OFFER = 1
        private const val VIEW_TYPE_REQUEST = 2
    }

    fun updateData(newItems: List<CommunityServiceItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CommunityServiceItem.Offer -> VIEW_TYPE_OFFER
            is CommunityServiceItem.Request -> VIEW_TYPE_REQUEST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OFFER) {
            val binding = ItemServiceOfferBinding.inflate(inflater, parent, false)
            OfferViewHolder(binding)
        } else {
            val binding = ItemHelpRequestBinding.inflate(inflater, parent, false)
            RequestViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CommunityServiceItem.Offer -> (holder as OfferViewHolder).bind(item)
            is CommunityServiceItem.Request -> (holder as RequestViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class OfferViewHolder(private val binding: ItemServiceOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommunityServiceItem.Offer) {
            binding.tvServiceTitle.text = item.title
            binding.tvServiceLocation.text = item.location
            binding.tvServicePrice.text = item.price
            binding.tvServiceStatus.text = item.status
            binding.ivServiceThumb.setImageResource(item.imageRes)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    inner class RequestViewHolder(private val binding: ItemHelpRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommunityServiceItem.Request) {
            binding.tvRequestTitle.text = item.title
            binding.tvRequestLocation.text = item.location
            binding.tvRequestStatus.text = item.timeText

            binding.btnOfferHelp.setOnClickListener {
                onOfferHelpClick(item)
            }

            binding.btnOptions.setOnClickListener {
                onItemClick(item)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
