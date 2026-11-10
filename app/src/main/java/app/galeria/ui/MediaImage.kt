package app.galeria.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.galeria.R
import app.galeria.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaImage(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSize: Int = 512,
) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, targetSize) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                resolver.loadThumbnail(Uri.parse(item.uri), Size(targetSize, targetSize), null)
            }.getOrNull()
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = stringResource(R.string.photo_thumbnail, item.displayName),
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (item.mimeType.startsWith("video/")) GalleryIcons.Video else GalleryIcons.Photo,
                contentDescription = stringResource(R.string.photo_thumbnail, item.displayName),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Decodes a full-resolution image (via [ImageDecoder], subsampled only above
 * 4096px) for the viewer. Unlike [MediaImage], this does not go through the
 * MediaStore thumbnail cache, so photos render at their real resolution.
 */
@Composable
fun FullResImage(
    item: MediaItem,
    modifier: Modifier = Modifier,
) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(resolver, Uri.parse(item.uri))
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDimension = maxOf(info.size.width, info.size.height)
                    var sample = 1
                    while (maxDimension / sample > 4096) sample *= 2
                    decoder.setTargetSampleSize(sample)
                }
            }.getOrNull()
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = item.displayName,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = GalleryIcons.Photo,
                contentDescription = item.displayName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
