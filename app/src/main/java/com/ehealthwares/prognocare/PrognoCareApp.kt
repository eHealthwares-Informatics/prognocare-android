package com.ehealthwares.prognocare

import android.app.Application
import com.ehealthwares.prognocare.data.sync.OfflineSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrognoCareApp : Application() {

    @Inject
    lateinit var offlineSyncManager: OfflineSyncManager

    override fun onCreate() {
        super.onCreate()
        // Flush any offline writes and warm the patient cache.
        offlineSyncManager.start()
    }
}
