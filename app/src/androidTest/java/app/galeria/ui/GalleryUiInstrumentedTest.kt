package app.galeria.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.galeria.MainActivity
import app.galeria.data.MediaItem
import app.galeria.data.SyntheticMedia
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryUiInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var item: MediaItem

    @Before
    fun seedAndLaunch() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ).forEach { automation.grantRuntimePermission(context.packageName, it) }
        val (uri, mediaItem) = SyntheticMedia.importImage(resolver)
        created += uri
        item = mediaItem
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitThumbnail()
    }

    @After
    fun closeAndCleanUp() {
        scenario.close()
        created.forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun realImageOpensViewerDetailsAndEditor() {
        capture("01-photos.png")
        compose.onNodeWithContentDescription(thumbnailDescription()).performClick()
        compose.onNodeWithText(item.displayName).assertIsDisplayed()
        compose.onNodeWithText("Detalles").assertIsDisplayed().performClick()
        compose.onNodeWithText("Tipo").assertIsDisplayed()
        compose.onNodeWithText("image/jpeg").assertIsDisplayed()
        capture("02-details.png")
        Espresso.pressBack()
        compose.onNodeWithText("Editar").assertIsDisplayed().performClick()
        compose.onNodeWithText("Guardar copia").assertIsDisplayed()
        compose.onRoot().performTouchInput {
            swipeUp(startY = bottom * 0.85f, endY = bottom * 0.55f)
        }
        compose.waitForIdle()
        capture("03-editor.png")
        compose.onNodeWithText("Girar").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Vista previa editada").assertIsDisplayed()
    }

    @Test
    fun searchAndMainDestinationsWorkWithIndexedMedia() {
        compose.onNodeWithContentDescription("Buscar").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("consulta-sin-resultados")
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement(item.displayName)
        compose.onNodeWithContentDescription(thumbnailDescription()).assertIsDisplayed()

        compose.onNodeWithContentDescription("Álbumes").performClick()
        compose.onNodeWithText("Galeria").assertIsDisplayed()
        compose.onNodeWithContentDescription("Crear").performClick()
        compose.onNodeWithText("Imagen a vídeo").assertIsDisplayed()
        compose.onNodeWithText("Collage").assertIsDisplayed()
        capture("04-create.png")
        compose.onNodeWithContentDescription("Recuerdos").performClick()
        compose.onNodeWithText("Resumen anual").assertIsDisplayed()
    }

    private fun awaitThumbnail() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription(thumbnailDescription()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun thumbnailDescription() = "Miniatura de ${item.displayName}"

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        check(bitmap.width > 0 && bitmap.height > 0)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$runId-${name.removeSuffix(".png")}.capture")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GaleriaVerification/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private companion object {
        val runId: String = System.currentTimeMillis().toString()
    }
}
