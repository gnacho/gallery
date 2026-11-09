package app.galeria.data

import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3InstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()
    private val temporaryFiles = mutableListOf<File>()

    @After
    fun cleanUp() {
        created.forEach { resolver.delete(it, null, null) }
        temporaryFiles.forEach { it.delete() }
    }

    @Test
    fun videoTrimProducesOneSecondPlayableMp4() {
        val sourceItem = importSyntheticVideo()
        val output = temporaryFile("trimmed", ".mp4")
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<File>>()

        instrumentation.runOnMainSync {
            VideoTrimmer(context).trim(
                sourceItem,
                VideoTrimSpec(durationMillis = 2_000, startMillis = 500, endMillis = 1_500),
                output,
            ) {
                result.set(it)
                completed.countDown()
            }
        }

        assertTrue(completed.await(60, TimeUnit.SECONDS), "Video trimming timed out")
        assertEquals(output, result.get().getOrThrow())
        assertPlayableVideo(output, expectedMinimumDuration = 800, expectedMaximumDuration = 1_300, expectedAudio = true)
        publishEvidence(output, "trimmed.mp4.capture")
    }

    @Test
    fun slideshowExportProducesPlayableMp4FromRealImages() {
        val first = SyntheticMedia.importImage(resolver)
        val second = SyntheticMedia.importImage(resolver)
        created += first.first
        created += second.first
        val output = temporaryFile("slideshow", ".mp4")
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<File>>()

        instrumentation.runOnMainSync {
            SlideshowVideoExporter(context).export(
                SlideshowPlan(listOf(first.second, second.second), secondsPerImage = 1),
                output,
            ) {
                result.set(it)
                completed.countDown()
            }
        }

        assertTrue(completed.await(60, TimeUnit.SECONDS), "Slideshow export timed out")
        assertEquals(output, result.get().getOrThrow())
        assertPlayableVideo(output, expectedMinimumDuration = 1_500, expectedMaximumDuration = 2_500, expectedAudio = false)
        publishEvidence(output, "slideshow.mp4.capture")
    }

    private fun importSyntheticVideo(): MediaItem {
        val (uri, item) = SyntheticMedia.importVideo(resolver, instrumentation.context.assets)
        created += uri
        return item
    }

    private fun assertPlayableVideo(file: File, expectedMinimumDuration: Long, expectedMaximumDuration: Long, expectedAudio: Boolean) {
        assertTrue(file.length() > 0)
        val metadata = MediaMetadataRetriever()
        metadata.setDataSource(file.absolutePath)
        val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        val width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
        val height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
        assertNotNull(duration)
        assertTrue(duration in expectedMinimumDuration..expectedMaximumDuration, "Unexpected duration $duration ms")
        assertEquals(96, width)
        assertEquals(72, height)
        if (expectedAudio) assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
        assertNotNull(metadata.getFrameAtTime(500_000))
        metadata.release()
    }

    private fun publishEvidence(source: File, name: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}-$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GaleriaVerification/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            source.inputStream().use { input ->
                requireNotNull(resolver.openOutputStream(uri, "w")).use { output -> input.copyTo(output) }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun temporaryFile(prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix, context.cacheDir).also { temporaryFiles += it }
}
