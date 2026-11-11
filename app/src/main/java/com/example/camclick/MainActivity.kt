package com.example.camclick

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.camclick.ui.theme.CamclickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamclickTheme {
                var currentScreen by remember { mutableStateOf("landing") }
                var selectedImageUriForEditing by remember { mutableStateOf<Uri?>(null) }
                val context = LocalContext.current

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        currentScreen = "camera"
                    } else {
                        Toast.makeText(context, "Camera permission required to open camera", Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "landing" -> LandingScreen(
                            onOpenCameraClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    currentScreen = "camera"
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onSavedPhotosClick = {
                                currentScreen = "gallery"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "camera" -> CameraScreen(
                            onBackClick = { currentScreen = "landing" },
                            onGalleryClick = { currentScreen = "gallery" }
                        )
                        "gallery" -> GalleryScreen(
                            onBackClick = { currentScreen = "landing" },
                            onEditImageClick = { uri ->
                                selectedImageUriForEditing = uri
                                currentScreen = "edit"
                            }
                        )
                        "edit" -> selectedImageUriForEditing?.let { uri ->
                            EditImageScreen(
                                imageUri = uri,
                                onBackClick = { currentScreen = "gallery" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandingScreen(
    onOpenCameraClick: () -> Unit,
    onSavedPhotosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastelPink = Color(0xFFFFB6C1) // Soft Pastel Pink
    val cardTextColor = Color(0xFFC71585) // Deep rose text for clear contrast

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF0F5), // Soft pastel blush
                        Color(0xFFFFE4E1), // Soft rose pink
                        Color(0xFFFFF8DC)  // Soft warm cream
                    )
                )
            )
    ) {
        // Glitter / Sparkle Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sparkles = listOf(
                Offset(size.width * 0.15f, size.height * 0.10f),
                Offset(size.width * 0.85f, size.height * 0.15f),
                Offset(size.width * 0.25f, size.height * 0.32f),
                Offset(size.width * 0.75f, size.height * 0.42f),
                Offset(size.width * 0.12f, size.height * 0.68f),
                Offset(size.width * 0.88f, size.height * 0.76f),
                Offset(size.width * 0.40f, size.height * 0.88f),
                Offset(size.width * 0.65f, size.height * 0.08f),
                Offset(size.width * 0.50f, size.height * 0.22f),
                Offset(size.width * 0.30f, size.height * 0.80f)
            )

            sparkles.forEachIndexed { i, offset ->
                val radius = if (i % 2 == 0) 5f else 8f

                // Outer glow
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                    radius = radius * 1.8f,
                    center = offset
                )
                // Center core
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.9f,
                    center = offset
                )
                // Star sparkle lines
                val arm = radius * 2.8f
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(offset.x - arm, offset.y),
                    end = Offset(offset.x + arm, offset.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(offset.x, offset.y - arm),
                    end = Offset(offset.x, offset.y + arm),
                    strokeWidth = 2f
                )
            }
        }

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Text
            Text(
                text = "Hello user :)",
                color = Color(0xFFC71585),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "what u wanna choose today ?",
                color = Color(0xFF4A4A4A),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Open Camera Button Card
            Card(
                onClick = onOpenCameraClick,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(110.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(3.dp, Color.White),
                colors = CardDefaults.cardColors(containerColor = pastelPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open Camera",
                        color = cardTextColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Photos Button Card
            Card(
                onClick = onSavedPhotosClick,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(110.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(3.dp, Color.White),
                colors = CardDefaults.cardColors(containerColor = pastelPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Saved Photos",
                        color = cardTextColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}