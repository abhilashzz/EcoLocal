package com.ecolocal.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.ecolocal.app.model.CommunityService
import com.ecolocal.app.model.MarketplaceListing
import com.google.android.gms.location.LocationServices
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distance calculation and device location helper for EcoLocal.
 * Implements the Haversine formula and coordinates resolution.
 */
object LocationHelper {

    const val NEARBY_RADIUS_KM = 25.0

    var cachedLatitude: Double? = null
    var cachedLongitude: Double? = null

    fun getCachedCoordinates(): Pair<Double, Double>? {
        val lat = cachedLatitude
        val lon = cachedLongitude
        return if (lat != null && lon != null) Pair(lat, lon) else null
    }

    // Coordinates lookup table for Sri Lankan cities/suburbs
    private val cityCoordinates = mapOf(
        "malabe" to Pair(6.9041, 79.9547),
        "kaduwela" to Pair(6.9333, 79.9833),
        "battaramulla" to Pair(6.8994, 79.9234),
        "rajagiriya" to Pair(6.9092, 79.8938),
        "colombo" to Pair(6.9271, 79.8612),
        "nugegoda" to Pair(6.8649, 79.8997),
        "maharagama" to Pair(6.8480, 79.9267),
        "kotte" to Pair(6.8917, 79.9000),
        "dehiwala" to Pair(6.8511, 79.8653),
        "mount lavinia" to Pair(6.8375, 79.8636),
        "moratuwa" to Pair(6.7730, 79.8816),
        "negombo" to Pair(7.2008, 79.8737),
        "kandy" to Pair(7.2906, 80.6337),
        "galle" to Pair(6.0535, 80.2210),
        "matara" to Pair(5.9549, 80.5550),
        "anuradhapura" to Pair(8.3114, 80.4037),
        "kurunegala" to Pair(7.4863, 80.3623),
        "jaffna" to Pair(9.6615, 80.0255),
        "gampaha" to Pair(7.0840, 79.9939),
        "kalutara" to Pair(6.5854, 79.9607)
    )

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun getCoordinatesForLocation(locationName: String?): Pair<Double, Double>? {
        if (locationName.isNullOrBlank()) return null
        val cleaned = locationName.trim().lowercase()
        // Check exact match
        cityCoordinates[cleaned]?.let { return it }

        // Check token/contains match
        for ((city, coords) in cityCoordinates) {
            if (cleaned.contains(city) || city.contains(cleaned)) {
                return coords
            }
        }
        return null
    }

    fun getListingCoordinates(listing: MarketplaceListing): Pair<Double, Double>? {
        if (listing.latitude != null && listing.longitude != null && listing.latitude != 0.0) {
            return Pair(listing.latitude, listing.longitude)
        }
        return getCoordinatesForLocation(listing.locationName)
    }

    fun getServiceCoordinates(service: CommunityService): Pair<Double, Double>? {
        if (service.latitude != null && service.longitude != null && service.latitude != 0.0) {
            return Pair(service.latitude, service.longitude)
        }
        return getCoordinatesForLocation(service.locationName)
    }

    fun formatDistance(distanceKm: Double?): String? {
        if (distanceKm == null) return null
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m away"
        } else {
            String.format("%.1f km away", distanceKm)
        }
    }

    /**
     * Retrieves the current device location via FusedLocationProviderClient.
     * Respects AppPreferences.isLocationSuggestionsEnabled().
     * Returns null if permission is denied, disabled, or location unavailable.
     */
    fun getDeviceLocation(context: Context, onResult: (Location?) -> Unit) {
        if (!AppPreferences.isLocationSuggestionsEnabled()) {
            onResult(null)
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            onResult(null)
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cachedLatitude = location.latitude
                        cachedLongitude = location.longitude
                    }
                    onResult(location)
                }
                .addOnFailureListener {
                    onResult(null)
                }
        } catch (_: Exception) {
            onResult(null)
        }
    }
}
