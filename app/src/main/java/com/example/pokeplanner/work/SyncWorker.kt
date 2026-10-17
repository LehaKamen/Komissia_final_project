package com.example.pokeplanner.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.pokeplanner.data.Repository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker class SyncWorker @AssistedInject constructor(
 @Assisted c:Context,@Assisted p:WorkerParameters,private val r:Repository
):CoroutineWorker(c,p){
 override suspend fun doWork():Result=try{r.refresh();Result.success()}catch(e:Exception){Result.retry()}
 companion object {
  fun schedule(context:Context,hours:Long=12){
   val request=PeriodicWorkRequestBuilder<SyncWorker>(hours.coerceAtLeast(1),TimeUnit.HOURS)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
   WorkManager.getInstance(context).enqueueUniquePeriodicWork("pokemon_cache_sync",ExistingPeriodicWorkPolicy.UPDATE,request)
  }
 }
}
