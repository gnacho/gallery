package app.galeria.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.galeria.data.GallerySettings
import app.galeria.data.MediaItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w724dp-h784dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryVisualTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<GalleryVisualTestActivity>()

    private val items = List(10) { index ->
        MediaItem(
            id = index + 1L,
            uri = "content://media/external/images/media/${index + 1L}",
            displayName = "sintetica-${index + 1}.jpg",
            mimeType = "image/jpeg",
            dateTakenMillis = 1_792_934_400_000L - index * 1_000L,
            width = 1200,
            height = 900,
            sizeBytes = 250_000,
            bucketId = 17,
            bucketName = "Camera",
        )
    }

    @Test
    fun darkThemeProvidesLightContentByDefault() {
        var observedColor: androidx.compose.ui.graphics.Color? = null
        composeRule.setContent {
            GalleryTheme(dynamicColor = false, darkTheme = true) {
                observedColor = androidx.compose.material3.LocalContentColor.current
            }
        }
        composeRule.runOnIdle {
            org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color.White, observedColor)
        }
    }

    @Test
    fun photosCaptureContainsReferenceNavigation() {
        render(GalleryNavigation.initial(), "01-photos.png")
        composeRule.onNodeWithText("Galería").assertExists()
        composeRule.onNodeWithText("Recuerdos").assertExists()
        composeRule.onNodeWithText("Crear").assertExists()
        composeRule.onNodeWithText("Álbumes").assertExists()
        composeRule.onNodeWithContentDescription("Buscar").assertExists()
    }

    @Test
    fun viewerCaptureContainsVisibleReferenceActions() {
        val state = GalleryNavigation.openViewer(GalleryNavigation.initial(), items.first())
        render(state, "02-viewer.png")
        composeRule.onNodeWithText("Detalles").assertExists()
        composeRule.onNodeWithText("Compartir").assertExists()
        composeRule.onNodeWithText("Favorito").assertExists()
        composeRule.onNodeWithText("Editar").assertExists()
        composeRule.onNodeWithText("Eliminar").assertExists()
        composeRule.onNodeWithText("Más").assertExists()
    }

    @Test
    fun createCaptureContainsImplementedCreationTools() {
        render(GalleryNavigation.select(GalleryNavigation.initial(), GalleryDestination.Create), "03-create.png")
        composeRule.onNodeWithText("Imagen a vídeo").assertExists()
        composeRule.onNodeWithText("Collage").assertExists()
        composeRule.onNodeWithText("Presentación instantánea").assertExists()
    }

    @Test
    fun editorCaptureContainsCoreControls() {
        render(GalleryNavigation.initial(), "04-editor.png", requestedEditor = items.first())
        composeRule.onNodeWithText("Girar").assertExists()
        composeRule.onNodeWithText("Voltear").assertExists()
        composeRule.onNodeWithText("Brillo").assertExists()
        composeRule.onNodeWithText("Contraste").assertExists()
        composeRule.onNodeWithText("Guardar copia").assertExists()
    }

    private fun render(state: GalleryUiState, fileName: String, requestedEditor: MediaItem? = null) {
        val loaded = GalleryNavigation.mediaLoaded(state, items, ZoneId.of("Europe/Madrid"))
        composeRule.setContent {
            GalleryTheme(dynamicColor = false, darkTheme = true) {
                Box(modifier = Modifier.requiredSize(724.dp, 784.dp)) {
                    GalleryApp(
                        state = loaded,
                        requestedEditor = requestedEditor,
                        settings = GallerySettings(dynamicColor = false),
                        onSettings = {},
                        favoriteItems = emptyList(),
                        trashItems = emptyList(),
                        isLoading = false,
                        loadError = null,
                        onDestination = {},
                        onOpen = {},
                        onCloseViewer = {},
                        onRetry = {},
                        onReload = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val projectRoot = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val output = File(projectRoot, "verification/captures/$fileName")
        checkNotNull(output.parentFile).mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(bitmap.width == 2172 && bitmap.height == 2352) {
            "Unexpected capture size ${bitmap.width}x${bitmap.height}"
        }
    }
}
