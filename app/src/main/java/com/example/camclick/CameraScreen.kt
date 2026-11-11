package com.example.camclick

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// Enum Definitions
enum class SaveLocationType {
    PUBLIC_CAMCRAFT,
    PRIVATE_INTERNAL,
    CUSTOM_SAF
}

enum class ImageFormatType {
    JPEG,
    PNG
}

enum class ResolutionType {
    HIGH,
    MEDIUM,
    LOW
}

@Composable
fun CameraScreen(
    onBackClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera & Flash States
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastThumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

    // Expandable Menu State
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Add-On Features States
    var isDarkMode by remember { mutableStateOf(true) } // Light/Dark Theme toggle
    var isGridEnabled by remember { mutableStateOf(false) } // Framing Grid Overlay
    var timerSeconds by remember { mutableIntStateOf(0) } // Self-Timer (0, 3, 5, 10s)
    var countdownState by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isBurstMode by remember { mutableStateOf(false) } // Burst Mode (5 Shots)
    var isGeotagEnabled by remember { mutableStateOf(false) } // GPS Geotagging

    // Settings States
    var showSettingsDialog by remember { mutableStateOf(false) }
    var saveLocationType by remember { mutableStateOf(SaveLocationType.PUBLIC_CAMCRAFT) }
    var customSafUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFormat by remember { mutableStateOf(ImageFormatType.JPEG) }
    var selectedResolution by remember { mutableStateOf(ResolutionType.HIGH) }

    // SAF Folder Picker Launcher
    val safFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            customSafUri = uri
            saveLocationType = SaveLocationType.CUSTOM_SAF
            Toast.makeText(context, "Custom folder set!", Toast.LENGTH_SHORT).show()
        }
    }

    // GPS Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isGeotagEnabled = true
            Toast.makeText(context, "GPS Geotagging Enabled", Toast.LENGTH_SHORT).show()
        } else {
            isGeotagEnabled = false
            Toast.makeText(context, "Location permission required for geotagging", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val latestUri = fetchLatestImageUri(context)
        latestUri?.let { uri ->
            lastThumbnail = loadThumbnailFromUri(context, uri)
        }
    }

    // Dynamic ImageCapture configuration
    val imageCapture = remember(selectedFormat, selectedResolution, flashMode) {
        val targetSize = when (selectedResolution) {
            ResolutionType.HIGH -> Size(1920, 1080)
            ResolutionType.MEDIUM -> Size(1280, 720)
            ResolutionType.LOW -> Size(640, 480)
        }

        val resSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            )
            .build()

        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setResolutionSelector(resSelector)
            .build()
    }

    // Bind Camera Lifecycle
    LaunchedEffect(lensFacing, previewView, imageCapture) {
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

    // Helper to get GPS Location
    fun getCurrentLocation(): Location? {
        if (!isGeotagEnabled) return null
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    // Single Photo Capture Engine
    fun executeSingleCapture(shotSuffix: String = "") {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val extension = if (selectedFormat == ImageFormatType.JPEG) "jpg" else "png"
        val mimeType = if (selectedFormat == ImageFormatType.JPEG) "image/jpeg" else "image/png"
        val fileName = "CamCraft_$timeStamp$shotSuffix.$extension"

        // Attach Location Metadata if enabled
        val metadata = ImageCapture.Metadata().apply {
            location = getCurrentLocation()
        }

        when (saveLocationType) {
            SaveLocationType.PUBLIC_CAMCRAFT -> {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CamCraft")
                    }
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).setMetadata(metadata).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Toast.makeText(context, "Saved photo $shotSuffix", Toast.LENGTH_SHORT).show()
                            output.savedUri?.let { uri ->
                                lastThumbnail = loadThumbnailFromUri(context, uri)
                            }
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Toast.makeText(context, "Failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SaveLocationType.PRIVATE_INTERNAL -> {
                val internalFolder = File(context.filesDir, "CamCraft")
                if (!internalFolder.exists()) internalFolder.mkdirs()

                val photoFile = File(internalFolder, fileName)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Toast.makeText(context, "Saved to App Storage", Toast.LENGTH_SHORT).show()
                            val savedUri = Uri.fromFile(photoFile)
                            lastThumbnail = loadThumbnailFromUri(context, savedUri)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Toast.makeText(context, "Failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SaveLocationType.CUSTOM_SAF -> {
                val safUri = customSafUri
                if (safUri == null) {
                    Toast.makeText(context, "Select a custom folder in settings first!", Toast.LENGTH_SHORT).show()
                    return
                }

                try {
                    val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                        safUri,
                        DocumentsContract.getTreeDocumentId(safUri)
                    )
                    val docUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        treeDocumentUri,
                        mimeType,
                        fileName
                    )

                    if (docUri != null) {
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            docUri,
                            ContentValues()
                        ).setMetadata(metadata).build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    Toast.makeText(context, "Saved to Custom SAF Folder!", Toast.LENGTH_SHORT).show()
                                    lastThumbnail = loadThumbnailFromUri(context, docUri)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(context, "Failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "SAF Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Shutter Trigger (Timer + Burst Mode Logic)
    fun onShutterClick() {
        if (isTimerRunning) return

        coroutineScope.launch {
            // Step 1: Timer Countdown
            if (timerSeconds > 0) {
                isTimerRunning = true
                for (i in timerSeconds downTo 1) {
                    countdownState = i
                    delay(1000L)
                }
                countdownState = 0
                isTimerRunning = false
            }

            // Step 2: Burst Mode or Single Shot
            val totalShots = if (isBurstMode) 5 else 1
            for (shotIndex in 1..totalShots) {
                val suffix = if (isBurstMode) "_Burst_$shotIndex" else ""
                executeSingleCapture(suffix)
                if (isBurstMode && shotIndex < totalShots) {
                    delay(500L) // Half-second pause between burst shots
                }
            }
        }
    }

    // Dynamic Colors for Theme Toggle
    val panelBgColor = if (isDarkMode) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
    val panelTextColor = if (isDarkMode) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Live Feed
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

        // 2. Framing Grid Overlay
        if (isGridEnabled) {
            GridOverlay()
        }

        // 3. Countdown Overlay
        CountdownOverlay(secondsLeft = countdownState)

        // 4. Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }

            Button(
                onClick = { isMenuExpanded = !isMenuExpanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMenuExpanded) Color(0xFFFF1493) else Color.Black.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.5.dp, Color.White)
            ) {
                Text(
                    text = if (isMenuExpanded) "✕ Close Features" else "⚡ Controls Menu",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Expandable Controls Menu Panel
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 95.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = panelBgColor,
                border = BorderStroke(2.dp, Color(0xFFFF1493)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Controls & Add-Ons",
                        color = Color(0xFFFF1493),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Front/Back & Flash
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Text(if (lensFacing == CameraSelector.LENS_FACING_BACK) "📷 Front" else "📷 Back")
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
                                ImageCapture.FLASH_MODE_ON -> "⚡ Flash: ON"
                                ImageCapture.FLASH_MODE_AUTO -> "⚡ Flash: AUTO"
                                else -> "⚡ Flash: OFF"
                            }
                            Text(flashText)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Grid & Timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isGridEnabled = !isGridEnabled },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGridEnabled) Color(0xFFFF1493) else Color.DarkGray
                            )
                        ) {
                            Text(if (isGridEnabled) "📐 Grid: ON" else "📐 Grid: OFF")
                        }

                        Button(
                            onClick = {
                                timerSeconds = when (timerSeconds) {
                                    0 -> 3
                                    3 -> 5
                                    5 -> 10
                                    else -> 0
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerSeconds > 0) Color(0xFFFF1493) else Color.DarkGray
                            )
                        ) {
                            Text(if (timerSeconds > 0) "⏱️ Timer: ${timerSeconds}s" else "⏱️ Timer: OFF")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Burst Mode & GPS Geotagging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isBurstMode = !isBurstMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBurstMode) Color(0xFFFF1493) else Color.DarkGray
                            )
                        ) {
                            Text(if (isBurstMode) "💥 Burst (5x): ON" else "💥 Burst: OFF")
                        }

                        Button(
                            onClick = {
                                if (!isGeotagEnabled) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    isGeotagEnabled = false
                                    Toast.makeText(context, "GPS Geotagging Disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGeotagEnabled) Color(0xFFFF1493) else Color.DarkGray
                            )
                        ) {
                            Text(if (isGeotagEnabled) "🌍 GPS: ON" else "🌍 GPS: OFF")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 4: Theme Toggle & Advanced Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isDarkMode = !isDarkMode },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                        ) {
                            Text(if (isDarkMode) "🌙 Dark Mode" else "☀️ Light Mode", color = Color.White)
                        }

                        Button(
                            onClick = {
                                isMenuExpanded = false
                                showSettingsDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                        ) {
                            Text("⚙️ Settings", color = Color.White)
                        }
                    }
                }
            }
        }

        // 6. Bottom Controls Bar (Thumbnail & Shutter Button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(12.dp))
                    .clickable { onGalleryClick() },
                contentAlignment = Alignment.Center
            ) {
                lastThumbnail?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Last Image Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Text(
                    text = "No Pic",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(BorderStroke(4.dp, Color(0xFFFF1493)), CircleShape)
                    .clickable { onShutterClick() },
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

    // Camera Settings Dialog Modal
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Camera Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Save Location:", color = Color(0xFFFF1493), fontWeight = FontWeight.SemiBold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (saveLocationType == SaveLocationType.PUBLIC_CAMCRAFT),
                            onClick = { saveLocationType = SaveLocationType.PUBLIC_CAMCRAFT }
                        )
                        Text("Public (Pictures/CamCraft)", color = Color.White, fontSize = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (saveLocationType == SaveLocationType.PRIVATE_INTERNAL),
                            onClick = { saveLocationType = SaveLocationType.PRIVATE_INTERNAL }
                        )
                        Text("Private (App Folder)", color = Color.White, fontSize = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (saveLocationType == SaveLocationType.CUSTOM_SAF),
                            onClick = {
                                if (customSafUri == null) {
                                    safFolderPickerLauncher.launch(null)
                                } else {
                                    saveLocationType = SaveLocationType.CUSTOM_SAF
                                }
                            }
                        )
                        Text("Custom Folder (SAF)", color = Color.White, fontSize = 14.sp)
                    }

                    if (saveLocationType == SaveLocationType.CUSTOM_SAF || customSafUri != null) {
                        Button(
                            onClick = { safFolderPickerLauncher.launch(null) },
                            modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                        ) {
                            Text(if (customSafUri != null) "Change Folder" else "Pick Folder")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Image Format:", color = Color(0xFFFF1493), fontWeight = FontWeight.SemiBold)
                    Row {
                        Button(
                            onClick = { selectedFormat = ImageFormatType.JPEG },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFormat == ImageFormatType.JPEG) Color(0xFFFF1493) else Color.Gray
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("JPEG")
                        }
                        Button(
                            onClick = { selectedFormat = ImageFormatType.PNG },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFormat == ImageFormatType.PNG) Color(0xFFFF1493) else Color.Gray
                            )
                        ) {
                            Text("PNG")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Resolution:", color = Color(0xFFFF1493), fontWeight = FontWeight.SemiBold)
                    Row {
                        Button(
                            onClick = { selectedResolution = ResolutionType.HIGH },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedResolution == ResolutionType.HIGH) Color(0xFFFF1493) else Color.Gray
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("High")
                        }
                        Button(
                            onClick = { selectedResolution = ResolutionType.MEDIUM },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedResolution == ResolutionType.MEDIUM) Color(0xFFFF1493) else Color.Gray
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Med")
                        }
                        Button(
                            onClick = { selectedResolution = ResolutionType.LOW },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedResolution == ResolutionType.LOW) Color(0xFFFF1493) else Color.Gray
                            )
                        ) {
                            Text("Low")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

// Rule-of-Thirds Grid Overlay Composable
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val firstVertical = width / 3
        val secondVertical = (width / 3) * 2

        val firstHorizontal = height / 3
        val secondHorizontal = (height / 3) * 2

        val strokeWidth = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.4f)

        // Vertical lines
        drawLine(
            color = gridColor,
            start = Offset(firstVertical, 0f),
            end = Offset(firstVertical, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(secondVertical, 0f),
            end = Offset(secondVertical, height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(0f, firstHorizontal),
            end = Offset(width, firstHorizontal),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, secondHorizontal),
            end = Offset(width, secondHorizontal),
            strokeWidth = strokeWidth
        )
    }
}

// Visual Countdown Overlay Composable
@Composable
fun CountdownOverlay(
    secondsLeft: Int
) {
    if (secondsLeft > 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$secondsLeft",
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun fetchLatestImageUri(context: Context): Uri? {
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

    return context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = cursor.getLong(idColumn)
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        } else {
            null
        }
    }
}

private fun loadThumbnailFromUri(context: Context, uri: Uri): ImageBitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(120, 120), null).asImageBitmap()
        } else {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            Bitmap.createScaledBitmap(bitmap, 120, 120, true).asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}