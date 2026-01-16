package com.example.cookingassistant.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Photo picker manager that handles camera and gallery photo selection
 * Provides composable functions to create launchers for both photo sources
 */
object PhotoPickerManager {

    /**
     * Creates a file URI for camera photo capture
     * Uses FileProvider to create a content:// URI for the camera app
     *
     * @param context Application context
     * @return URI for the camera to save the photo, or null if file creation fails
     */
    fun createImageFileUri(context: Context): Uri? {
        return try {
            // Create a unique filename with timestamp
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "RECIPE_${timeStamp}.jpg"

            // Create the file in cache directory
            val storageDir = File(context.cacheDir, "camera")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, imageFileName)

            // Generate content URI using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies a photo from temporary location to permanent storage
     * Used after capturing a photo with camera to persist it
     *
     * @param context Application context
     * @param sourceUri URI of the temporary photo
     * @return URI of the copied photo in permanent storage, or null if copy fails
     */
    fun copyToAppStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "recipe_${timeStamp}.jpg"

            // Create images directory in internal storage
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val destFile = File(imagesDir, imageFileName)

            // Copy the file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Return content URI for the copied file
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Composable function that creates launchers for camera and gallery photo picking
 * Returns a PhotoPickerLaunchers object containing both launchers
 *
 * @param onPhotoSelected Callback invoked when a photo is selected from either source
 * @return PhotoPickerLaunchers containing camera and gallery launchers
 */
@Composable
fun rememberPhotoPickerLaunchers(
    onPhotoSelected: (Uri?) -> Unit
): PhotoPickerLaunchers {
    val context = androidx.compose.ui.platform.LocalContext.current

    // URI holder for camera photos (needs to be remembered across recompositions)
    val currentPhotoUri = remember { mutableListOf<Uri?>(null) }

    // Gallery launcher - directly returns the selected photo URI
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to app storage for persistence
            val copiedUri = PhotoPickerManager.copyToAppStorage(context, it)
            onPhotoSelected(copiedUri)
        }
    }

    // Camera launcher - uses the pre-created URI to save the photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            currentPhotoUri[0]?.let { uri ->
                // Copy from cache to permanent storage
                val copiedUri = PhotoPickerManager.copyToAppStorage(context, uri)
                onPhotoSelected(copiedUri)
            }
        }
        currentPhotoUri[0] = null
    }

    return remember {
        PhotoPickerLaunchers(
            launchCamera = {
                val uri = PhotoPickerManager.createImageFileUri(context)
                currentPhotoUri[0] = uri
                uri?.let { cameraLauncher.launch(it) }
            },
            launchGallery = {
                galleryLauncher.launch("image/*")
            }
        )
    }
}

/**
 * Data class holding launchers for camera and gallery
 *
 * @param launchCamera Function to launch the camera
 * @param launchGallery Function to launch the gallery picker
 */
data class PhotoPickerLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)
