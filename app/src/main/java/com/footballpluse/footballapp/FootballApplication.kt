package com.footballpluse.footballapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FootballApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB disk cache for player headshots, team badges, flags
                    .build()
            }
            .crossfade(true)
            .crossfade(200)
            .respectCacheHeaders(false) // Cache images aggressively even without explicit upstream HTTP TTL
            .build()
    }
}
