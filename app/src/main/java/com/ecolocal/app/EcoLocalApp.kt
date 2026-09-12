package com.ecolocal.app

import android.app.Application
import com.ecolocal.app.data.ChatRepository
import com.ecolocal.app.data.ListingRepository
import com.ecolocal.app.data.NotificationRepository
import com.ecolocal.app.data.RequestRepository
import com.ecolocal.app.data.SavedRepository

class EcoLocalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Room-backed repositories
        ListingRepository.init(this)
        SavedRepository.init(this)
        RequestRepository.init(this)
        NotificationRepository.init(this)
        ChatRepository.init(this)
    }
}
