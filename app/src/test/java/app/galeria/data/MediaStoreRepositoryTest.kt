package app.galeria.data

import android.content.ContentProvider
import android.content.ContentValues
import android.os.Bundle
import android.os.CancellationSignal
import org.robolectric.shadows.ShadowContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreRepositoryTest {
    @Test
    fun readsImageMetadataFromMediaStore() {
        val context = RuntimeEnvironment.getApplication()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "sample.jpg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Camera/")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            put(MediaStore.Images.Media.DATE_ADDED, 456L)
            put(MediaStore.Images.Media.DATE_TAKEN, 123_000L)
            put(MediaStore.Images.Media.WIDTH, 640)
            put(MediaStore.Images.Media.HEIGHT, 480)
            put(MediaStore.Images.Media.SIZE, 42_000L)
            put(MediaStore.Images.Media.BUCKET_ID, 99L)
            put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, "Camera")
        }
        val inserted = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull(inserted)

        val items = MediaStoreRepository(context).load()

        assertEquals(1, items.size)
        assertEquals("sample.jpg", items.single().displayName)
        assertEquals(123_000L, items.single().dateTakenMillis)
        assertEquals(640, items.single().width)
        assertEquals("Camera", items.single().bucketName)
    }

    @Test
    fun mergesVideosBeforeOlderImagesAndKeepsDuration() {
        val context = RuntimeEnvironment.getApplication()
        val image = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "older.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            put(MediaStore.Images.Media.DATE_TAKEN, 1_000L)
        }
        val video = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "newer.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            put(MediaStore.Video.Media.DATE_TAKEN, 2_000L)
            put(MediaStore.Video.Media.DURATION, 9_000L)
        }
        assertNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image))
        assertNotNull(context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video))

        val items = MediaStoreRepository(context).load()

        assertEquals(listOf("newer.mp4", "older.jpg"), items.map { it.displayName })
        assertEquals("video/mp4", items.first().mimeType)
        assertEquals(9_000L, items.first().durationMillis)
    }

    @Test
    fun queriesBothImageAndVideoCollections() {
        val provider = RecordingMediaProvider()
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)

        MediaStoreRepository(resolver).load()

        assertEquals(
            listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ),
            provider.queries,
        )
    }
    @Test
    fun queriesFavoritesAndTrashWithMediaStoreMatchArguments() {
        val provider = RecordingMediaProvider()
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)

        MediaStoreRepository(resolver).loadFavorites()
        MediaStoreRepository(resolver).loadTrash()

        assertEquals(4, provider.queryBundles.size)
        assert(provider.queryBundles.take(2).all {
            it.getInt(MediaStore.QUERY_ARG_MATCH_FAVORITE) == MediaStore.MATCH_INCLUDE
        })
        assert(provider.queryBundles.drop(2).all {
            it.getInt(MediaStore.QUERY_ARG_MATCH_TRASHED) == MediaStore.MATCH_INCLUDE
        })
    }
}

private class RecordingMediaProvider : ContentProvider() {
    val queries = mutableListOf<Uri>()
    val queryBundles = mutableListOf<Bundle>()

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        queries += uri
        return MatrixCursor(projection ?: MediaStoreQuery.projection)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        queryBundles += queryArgs ?: Bundle()
        return MatrixCursor(projection ?: MediaStoreQuery.projection)
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0
}
