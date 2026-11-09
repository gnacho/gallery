package app.galeria

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.galeria.data.ExternalMediaReader
import app.galeria.data.GallerySettingsStore
import app.galeria.data.MediaItem
import app.galeria.data.MediaStoreOperations
import app.galeria.data.MediaStoreRepository
import app.galeria.ui.GalleryApp
import app.galeria.ui.GalleryNavigation
import app.galeria.ui.GalleryTheme
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val repository by lazy { MediaStoreRepository(applicationContext) }
    private var galleryState by mutableStateOf(GalleryNavigation.initial())
    private var favoriteItems by mutableStateOf(emptyList<app.galeria.data.MediaItem>())
    private var trashItems by mutableStateOf(emptyList<app.galeria.data.MediaItem>())
    private var isLoading by mutableStateOf(true)
    private var loadError by mutableStateOf<String?>(null)
    private var requestedEditor by mutableStateOf<MediaItem?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.any { it.value }) {
            loadMedia()
        } else {
            isLoading = false
            galleryState = GalleryNavigation.permissionDenied(galleryState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsStore = remember { GallerySettingsStore(this) }
            val settings = remember { mutableStateOf(settingsStore.load()) }
            GalleryTheme(dynamicColor = settings.value.dynamicColor) {
                GalleryApp(
                    state = galleryState,
                    requestedEditor = requestedEditor,
                    settings = settings.value,
                    onSettings = {
                        settings.value = it
                        settingsStore.save(it)
                    },
                    favoriteItems = favoriteItems,
                    trashItems = trashItems,
                    isLoading = isLoading,
                    loadError = loadError,
                    onDestination = { galleryState = GalleryNavigation.select(galleryState, it) },
                    onOpen = { galleryState = GalleryNavigation.openViewer(galleryState, it) },
                    onCloseViewer = { galleryState = GalleryNavigation.closeViewer(galleryState) },
                    onRetry = ::requestMedia,
                    onReload = ::loadMedia,
                )
            }
        }
        if (!handleIncomingIntent(intent)) requestMedia()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent): Boolean {
        val uris: List<Uri> = when (incoming.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> listOfNotNull(incoming.data)
            Intent.ACTION_SEND -> listOfNotNull(incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return false
        isLoading = true
        loadError = null
        lifecycleScope.launch {
            val shouldImport = incoming.action == Intent.ACTION_SEND || incoming.action == Intent.ACTION_SEND_MULTIPLE
            val loaded = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    val item = ExternalMediaReader.read(contentResolver, uri, incoming.type)
                    if (shouldImport && item != null) {
                        runCatching {
                            val importedUri = MediaStoreOperations.importUri(contentResolver, uri, item.displayName, item.mimeType)
                            ExternalMediaReader.read(contentResolver, importedUri, item.mimeType)
                        }.getOrNull()
                    } else {
                        item
                    }
                }
            }
            if (loaded.isEmpty()) {
                loadError = if (shouldImport) "No se pudieron importar los elementos compartidos" else "No se pudieron abrir los elementos compartidos"
            } else {
                galleryState = GalleryNavigation.mediaLoaded(galleryState, loaded, ZoneId.systemDefault())
                if (shouldImport) {
                    favoriteItems = emptyList()
                    trashItems = emptyList()
                }
                if (incoming.action == Intent.ACTION_VIEW && loaded.size == 1) {
                    galleryState = GalleryNavigation.openViewer(galleryState, loaded.single())
                }
                if (incoming.action == Intent.ACTION_EDIT && loaded.size == 1) {
                    requestedEditor = loaded.single()
                }
            }
            isLoading = false
        }
        return true
    }

    private fun requestMedia() {
        if (hasMediaAccess()) {
            loadMedia()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ),
            )
        }
    }

    private fun hasMediaAccess(): Boolean = listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun loadMedia() {
        isLoading = true
        loadError = null
        lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    Triple(repository.load(), repository.loadFavorites(), repository.loadTrash())
                }
                val items = loaded.first
                favoriteItems = loaded.second
                trashItems = loaded.third
                galleryState = GalleryNavigation.mediaLoaded(galleryState, items, ZoneId.systemDefault())
                GalleryWidgetProvider.requestRefresh(this@MainActivity)
                isLoading = false
            } catch (_: SecurityException) {
                galleryState = GalleryNavigation.permissionDenied(galleryState)
                isLoading = false
            } catch (_: Exception) {
                loadError = getString(R.string.media_load_error)
                isLoading = false
            }
        }
    }
}
