package com.footballpluse.footballapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.footballpluse.footballapp.MainActivity
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.data.local.db.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteClubWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun appDatabase(): AppDatabase
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val imageLoader = coil.Coil.imageLoader(context)

        scope.launch {
            val clubs = runCatching {
                entryPoint.appDatabase().favouriteClubDao().getFavouriteClubsSync()
            }.getOrNull()

            val topClub = clubs?.firstOrNull()

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_favorite_club)

                if (topClub != null) {
                    views.setTextViewText(R.id.widget_club_name, topClub.clubName)
                    views.setTextViewText(R.id.widget_match_info, topClub.leagueName ?: "Followed Club")
                    views.setTextViewText(R.id.widget_status_badge, "FAVORITE")

                    // Load club crest
                    if (!topClub.logoUrl.isNullOrBlank()) {
                        val request = ImageRequest.Builder(context)
                            .data(topClub.logoUrl)
                            .allowHardware(false)
                            .build()

                        val result = runCatching { imageLoader.execute(request) }.getOrNull()
                        if (result is SuccessResult) {
                            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_club_badge, bitmap)
                            }
                        }
                    }
                } else {
                    views.setTextViewText(R.id.widget_club_name, "No Club Selected")
                    views.setTextViewText(R.id.widget_match_info, "Tap to add your favorite team")
                    views.setTextViewText(R.id.widget_status_badge, "SETUP")
                }

                // Click intent to open MainActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }

    companion object {
        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, FavoriteClubWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, FavoriteClubWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
