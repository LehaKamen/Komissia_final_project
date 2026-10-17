package com.example.pokeplanner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.pokeplanner.work.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp class PokePlannerApp:Application(),Configuration.Provider {
 @Inject lateinit var workerFactory:HiltWorkerFactory
 override val workManagerConfiguration:Configuration get()=Configuration.Builder().setWorkerFactory(workerFactory).build()
 override fun onCreate(){super.onCreate();SyncWorker.schedule(this)}
}
