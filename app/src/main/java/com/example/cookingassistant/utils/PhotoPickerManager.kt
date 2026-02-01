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

object PhotoPickerManager {

    fun createImageFileUri(context: Context): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "RECIPE_${timeStamp}.jpg"

            val storageDir = File(context.cacheDir, "camera")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, imageFileName)

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

    fun copyToAppStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "recipe_${timeStamp}.jpg"

            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val destFile = File(imagesDir, imageFileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

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

@Composable
fun rememberPhotoPickerLaunchers(
    onPhotoSelected: (Uri?) -> Unit
): PhotoPickerLaunchers {
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentPhotoUri = remember { mutableListOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = PhotoPickerManager.copyToAppStorage(context, it)
            onPhotoSelected(copiedUri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            currentPhotoUri[0]?.let { uri ->
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

data class PhotoPickerLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)
