package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemMarketplaceProductBinding
import com.ecolocal.app.model.MarketProduct

class MarketplaceProductAdapter(
    private var products: List<MarketProduct>,
    private val onItemClick: (MarketProduct) -> Unit = {}
) : RecyclerView.Adapter<MarketplaceProductAdapter.ViewHolder>() {

    fun updateData(newProducts: List<MarketProduct>) {
        products = newProducts
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMarketplaceProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarketplaceProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = products[position]

        holder.binding.tvProductTitle.text = item.title
        holder.binding.tvProductPrice.text = item.price
        holder.binding.tvProductCondition.text = item.condition
        com.ecolocal.app.util.ImageLoaderHelper.load(
            holder.binding.ivProductImage,
            item.imageUri,
            item.imageRes
        )

        // Giveaway badge overlay
        holder.binding.tvGiveawayBadge.visibility = if (item.isGiveaway) View.VISIBLE else View.GONE

        // Heart favorite toggle
        updateHeartIcon(holder, item.isFavorite)
        holder.binding.btnFavorite.setOnClickListener {
            item.isFavorite = !item.isFavorite
            updateHeartIcon(holder, item.isFavorite)
        }

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    private fun updateHeartIcon(holder: ViewHolder, isFavorite: Boolean) {
        val context = holder.itemView.context
        if (isFavorite) {
            holder.binding.ivHeart.setColorFilter(
                ContextCompat.getColor(context, R.color.eco_orange)
            )
        } else {
            holder.binding.ivHeart.setColorFilter(
                ContextCompat.getColor(context, R.color.eco_secondary_dark)
            )
        }
    }

    override fun getItemCount(): Int = products.size
}
