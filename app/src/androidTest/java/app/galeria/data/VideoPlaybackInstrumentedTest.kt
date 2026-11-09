package app.galeria.data

import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.Surface
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlaybackInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()
    private val player = AtomicReference<ExoPlayer?>()
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    @After
    fun cleanUp() {
        instrumentation.runOnMainSync {
            player.getAndSet(null)?.release()
            surface?.release()
            surfaceTexture?.release()
            surface = null
            surfaceTexture = null
        }
        created.forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun exoPlayerDecodesSyntheticVideoToCompletion() {
        val (uri, item) = SyntheticMedia.importVideo(resolver, instrumentation.context.assets)
        created += uri
        val ready = CountDownLatch(1)
        val firstFrame = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        val videoSize = AtomicReference<VideoSize>()

        instrumentation.runOnMainSync {
            val exoPlayer = ExoPlayer.Builder(context).build()
            player.set(exoPlayer)
            surfaceTexture = SurfaceTexture(0)
            surface = Surface(requireNotNull(surfaceTexture))
            exoPlayer.setVideoSurface(surface)
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) ready.countDown()
                    if (playbackState == Player.STATE_ENDED) ended.countDown()
                }

                override fun onPlayerError(playbackError: androidx.media3.common.PlaybackException) {
                    error.set(playbackError)
                    ready.countDown()
                    firstFrame.countDown()
                    ended.countDown()
                }

                override fun onRenderedFirstFrame() {
                    firstFrame.countDown()
                }

                override fun onVideoSizeChanged(size: VideoSize) {
                    videoSize.set(size)
                }
            })
            exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(item.uri)))
            exoPlayer.prepare()
            exoPlayer.play()
        }

        assertTrue(ready.await(15, TimeUnit.SECONDS), "ExoPlayer did not become ready")
        assertTrue(firstFrame.await(15, TimeUnit.SECONDS), "ExoPlayer did not render a frame")
        val paused = AtomicReference(false)
        instrumentation.runOnMainSync {
            val current = requireNotNull(player.get())
            current.pause()
            paused.set(!current.playWhenReady && current.playbackState == Player.STATE_READY)
        }
        assertTrue(paused.get(), "ExoPlayer did not pause in READY state")
        instrumentation.runOnMainSync { requireNotNull(player.get()).play() }
        assertTrue(ended.await(20, TimeUnit.SECONDS), "ExoPlayer did not reach the end")
        assertNull(error.get())
        val size = videoSize.get()
        assertNotNull(size)
        assertEquals(96, size.width)
        assertEquals(72, size.height)
    }
}
