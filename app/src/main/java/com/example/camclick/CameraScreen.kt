package com.example.camclick

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // ImageCapture use case
    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    // Bind camera lifecycle
    LaunchedEffect(lensFacing, previewView) {
        val currentPreviewView = previewView ?: return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            if (cameraProvider.hasCamera(cameraSelector)) {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(currentPreviewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Helper function to capture and save photo
    fun takePhoto() {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CamCraft")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(context, "Photo saved to Pictures/CamCraft", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(context, "Failed to capture photo: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also {
                    previewView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }

            Button(
                onClick = {
                    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
                    if (!hasFlash) {
                        Toast.makeText(context, "Flash not available", Toast.LENGTH_SHORT).show()
                    } else {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }
                }
            ) {
                val flashText = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> "Flash: ON"
                    ImageCapture.FLASH_MODE_AUTO -> "Flash: AUTO"
                    else -> "Flash: OFF"
                }
                Text(flashText)
            }

            Button(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            ) {
                Text(if (lensFacing == CameraSelector.LENS_FACING_BACK) "Front" else "Back")
            }
        }

        // Bottom Shutter / Capture Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(BorderStroke(4.dp, Color(0xFFFF1493)), CircleShape)
                    .clickable { takePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1493))
                )
            }
        }
    }
}