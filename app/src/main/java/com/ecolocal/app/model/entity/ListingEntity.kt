package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ecolocal.app.model.MarketplaceListing

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String = "user_nimal",
    val listingType: String = "FOR SALE",
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val description: String,
    val location: String,
    val imageRes: Int = 0,
    val imageUri: String? = null,
    val secondaryImageRes: Int? = null,
    val isAvailable: Boolean = true,
    val sellerName: String = "Nimal Perera",
    val sellerAvatarRes: Int = 0,
    val memberSince: String = "Member since 2026",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toModel(): MarketplaceListing {
        return MarketplaceListing(
            id = id,
            ownerId = ownerId,
            listingType = listingType,
            title = title,
            price = price,
            category = category,
            condition = condition,
            description = description,
            location = location,
            imageRes = imageRes,
            imageUri = imageUri,
            secondaryImageRes = secondaryImageRes,
            isAvailable = isAvailable,
            sellerName = sellerName,
            sellerAvatarRes = sellerAvatarRes,
            memberSince = memberSince,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromModel(model: MarketplaceListing): ListingEntity {
            return ListingEntity(
                id = model.id,
                ownerId = model.ownerId,
                listingType = model.listingType,
                title = model.title,
                price = model.price,
                category = model.category,
                condition = model.condition,
                description = model.description,
                location = model.location,
                imageRes = model.imageRes,
                imageUri = model.imageUri,
                secondaryImageRes = model.secondaryImageRes,
                isAvailable = model.isAvailable,
                sellerName = model.sellerName,
                sellerAvatarRes = model.sellerAvatarRes,
                memberSince = model.memberSince,
                createdAt = model.createdAt
            )
        }
    }
}
