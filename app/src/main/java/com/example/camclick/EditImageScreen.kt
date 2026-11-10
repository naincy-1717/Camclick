package com.example.camclick

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditImageScreen(
    imageUri: Uri,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var currentBitmap by remember(imageUri) {
        mutableStateOf(loadBitmapFromUri(context, imageUri))
    }
    var exifInfo by remember(imageUri) {
        mutableStateOf(readExifData(context, imageUri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }

            Text(
                text = "Edit & Share",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { shareImage(context, imageUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
            ) {
                Text("Share", color = Color.White)
            }
        }

        // Image Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            currentBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Edited Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } ?: Text("Failed to load image", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EXIF Metadata Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "EXIF Metadata",
                    color = Color(0xFFFF1493),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = "Date/Time: ${exifInfo.dateTime}", color = Color.White, fontSize = 12.sp)
                Text(text = "Resolution: ${exifInfo.resolution}", color = Color.White, fontSize = 12.sp)
                Text(text = "Geotag (Lat/Lng): ${exifInfo.location}", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Editing Toolbar Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Rotate 90 Degrees
            Button(
                onClick = {
                    currentBitmap?.let { bmp ->
                        val matrix = Matrix().apply { postRotate(90f) }
                        currentBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    }
                }
            ) {
                Text("Rotate 90°")
            }

            // Crop Square (1:1)
            Button(
                onClick = {
                    currentBitmap?.let { bmp ->
                        val size = minOf(bmp.width, bmp.height)
                        val x = (bmp.width - size) / 2
                        val y = (bmp.height - size) / 2
                        currentBitmap = Bitmap.createBitmap(bmp, x, y, size, size)
                    }
                }
            ) {
                Text("Crop 1:1")
            }

            // Save Edited Copy
            Button(
                onClick = {
                    currentBitmap?.let { bmp ->
                        saveBitmapToMediaStore(context, bmp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
            ) {
                Text("Save Copy", color = Color.White)
            }
        }
    }
}

// Data class for EXIF details
data class ExifData(
    val dateTime: String,
    val resolution: String,
    val location: String
)

// Helper to extract EXIF data
private fun readExifData(context: Context, uri: Uri): ExifData {
    var dateTime = "Unknown"
    var resolution = "Unknown"
    var location = "No Geotag"

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)

            val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!date.isNullOrEmpty()) dateTime = date

            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            if (width != null && length != null) resolution = "${width}x${length}"

            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                location = "${latLong[0]}, ${latLong[1]}"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ExifData(dateTime, resolution, location)
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        null
    }
}

private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Image Via"))
}

private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap) {
    val timeStamp = System.currentTimeMillis()
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "CamCraft_Edited_$timeStamp.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CamCraft")
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            Toast.makeText(context, "Edited photo saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}