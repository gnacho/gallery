package app.galeria

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.galeria.data.MediaItem
import app.galeria.data.MediaStoreRepository
import app.galeria.data.PickerResults
import app.galeria.ui.GalleryTheme
import app.galeria.ui.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        setContent {
            GalleryTheme {
                var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
                var selected by remember { mutableStateOf<Set<Uri>>(emptySet()) }
                var loading by remember { mutableStateOf(true) }
                var denied by remember { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    denied = !hasMediaAccess()
                    if (!denied) {
                        loading = true
                        lifecycleScope.launch {
                            items = withContext(Dispatchers.IO) { MediaStoreRepository(this@PickerActivity).load() }
                            loading = false
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    if (hasMediaAccess()) {
                        items = withContext(Dispatchers.IO) { MediaStoreRepository(this@PickerActivity).load() }
                        loading = false
                    } else {
                        val permissions = buildList {
                            add(Manifest.permission.READ_MEDIA_IMAGES)
                            add(Manifest.permission.READ_MEDIA_VIDEO)
                            if (Build.VERSION.SDK_INT >= 34) add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }
                Scaffold(
                    topBar = { TopAppBar(title = { Text(if (allowMultiple) "Selecciona fotos y vídeos" else "Selecciona un elemento") }) },
                    bottomBar = {
                        Button(
                            onClick = {
                                val chosen = items.filter { selected.contains(Uri.parse(it.uri)) }.map { Uri.parse(it.uri) }
                                if (chosen.isNotEmpty()) {
                                    setResult(Activity.RESULT_OK, if (allowMultiple) PickerResults.multiple(chosen) else PickerResults.single(chosen.first()))
                                    finish()
                                }
                            },
                            enabled = selected.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) { Text(if (selected.isEmpty()) "Selecciona para continuar" else "Confirmar ${selected.size}") }
                    },
                ) { padding ->
                    when {
                        loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        denied -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Se necesita acceso a tus fotos") }
                        items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No hay elementos") }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(140.dp),
                            contentPadding = PaddingValues(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize().padding(padding),
                        ) {
                            items(items, key = { it.uri }) { item ->
                                val uri = Uri.parse(item.uri)
                                val isSelected = selected.contains(uri)
                                Box(
                                    Modifier.aspectRatio(1f).clickable {
                                        selected = when {
                                            isSelected -> selected - uri
                                            allowMultiple -> selected + uri
                                            else -> setOf(uri)
                                        }
                                    },
                                ) {
                                    MediaImage(item, Modifier.fillMaxSize())
                                    if (isSelected) {
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                                        Icon(Icons.Default.CheckCircle, "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasMediaAccess(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
}
