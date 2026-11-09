package app.galeria.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

@OptIn(UnstableApi::class)
class SlideshowVideoExporter(context: Context) {
    private val appContext = context.applicationContext

    fun export(
        plan: SlideshowPlan,
        output: File,
        onComplete: (Result<File>) -> Unit,
    ): Transformer {
        val editedImages = plan.items.map { item ->
            val source = Media3Item.Builder()
                .setUri(Uri.parse(item.uri))
                .setImageDurationMs(plan.imageDurationMillis)
                .build()
            EditedMediaItem.Builder(source).setFrameRate(30).build()
        }
        val composition = Composition.Builder(
            EditedMediaItemSequence.withVideoFrom(editedImages),
        ).build()
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
        transformer.start(composition, output.absolutePath)
        return transformer
    }
}
