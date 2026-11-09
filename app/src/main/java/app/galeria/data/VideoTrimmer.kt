package app.galeria.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

@OptIn(UnstableApi::class)
class VideoTrimmer(context: Context) {
    private val appContext = context.applicationContext

    fun trim(
        item: MediaItem,
        spec: VideoTrimSpec,
        output: File,
        onComplete: (Result<File>) -> Unit,
    ): Transformer {
        val clipping = Media3Item.ClippingConfiguration.Builder()
            .setStartPositionMs(spec.startMillis)
            .setEndPositionMs(spec.endMillis)
            .build()
        val source = Media3Item.Builder()
            .setUri(Uri.parse(item.uri))
            .setClippingConfiguration(clipping)
            .build()
        val edited = EditedMediaItem.Builder(source).build()
        val transformer = Transformer.Builder(appContext)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onComplete(Result.success(output))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    output.delete()
                    onComplete(Result.failure(exportException))
                }
            })
            .build()
        output.parentFile?.mkdirs()
        transformer.start(edited, output.absolutePath)
        return transformer
    }
}
