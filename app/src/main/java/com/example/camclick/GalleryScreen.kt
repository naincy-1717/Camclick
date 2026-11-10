package com.example.camclick

import android.content.ContentUris
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GalleryScreen(
    onBackClick: () -> Unit,
    onEditImageClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        imageUris = fetchAllImageUris(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Saved Photos",
                color = Color.White,
                fontSize = 22.sp
            )
        }

        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved photos found", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(imageUris) { uri ->
                    GalleryImageItem(
                        context = context,
                        uri = uri,
                        onClick = { selectedImageUri = uri }
                    )
                }
            }
        }
    }

    // Full Screen Image Viewer Modal
    selectedImageUri?.let { uri ->
        Dialog(
            onDismissRequest = { selectedImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val fullBitmap = remember(uri) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                fullBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Full Photo View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Top Control Bar on Full View (Close & Edit Buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { selectedImageUri = null }) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            val uriToEdit = uri
                            selectedImageUri = null
                            onEditImageClick(uriToEdit)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
                    ) {
                        Text("Edit & Details", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryImageItem(context: Context, uri: Uri, onClick: () -> Unit) {
    val bitmap = remember(uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, android.util.Size(250, 250), null)
            } else {
                @Suppress("DEPRECATION")
                val fullBmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                android.graphics.Bitmap.createScaledBitmap(fullBmp, 250, 250, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable { onClick() }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun fetchAllImageUris(context: Context): List<Uri> {
    val uriList = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else {
        "${MediaStore.Images.Media.DATA} LIKE ?"
    }
    val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf("Pictures/CamCraft%")
    } else {
        arrayOf("%Pictures/CamCraft%")
    }
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            uriList.add(uri)
        }
    }
    return uriList
}