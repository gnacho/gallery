package app.galeria

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Size
import android.widget.RemoteViews
import app.galeria.data.MediaStoreRepository
import app.galeria.data.WidgetMediaSelection

class GalleryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        Thread {
            val views = RemoteViews(applicationContext.packageName, R.layout.widget_gallery)
            try {
                val item = WidgetMediaSelection.latestImage(MediaStoreRepository(applicationContext).load())
                if (item == null) {
                    views.setTextViewText(R.id.widget_title, "No hay fotos disponibles")
                } else {
                    val thumbnail = applicationContext.contentResolver.loadThumbnail(Uri.parse(item.uri), Size(1024, 1024), null)
                    views.setImageViewBitmap(R.id.widget_image, thumbnail)
                    views.setTextViewText(R.id.widget_title, item.displayName)
                }
            } catch (_: Exception) {
                views.setTextViewText(R.id.widget_title, "Abre Galería para conceder acceso a tus fotos")
            }
            val openIntent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            pendingResult.finish()
        }.start()
    }

    companion object {
        fun requestRefresh(context: Context) {
            val applicationContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(applicationContext)
            val component = ComponentName(applicationContext, GalleryWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                applicationContext.sendBroadcast(
                    Intent(applicationContext, GalleryWidgetProvider::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }
}
