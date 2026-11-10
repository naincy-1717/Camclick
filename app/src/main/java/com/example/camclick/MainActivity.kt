package com.example.camclick

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
                            onBackClick = { currentScreen = "landing" }
                        )
                        "gallery" -> GalleryScreen(
                            onBackClick = { currentScreen = "landing" }
                        )
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
    val brightPink = Color(0xFFFF1493)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            onClick = onOpenCameraClick,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(130.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, Color.White),
            colors = CardDefaults.cardColors(containerColor = brightPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open Camera",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            onClick = onSavedPhotosClick,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(130.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, Color.White),
            colors = CardDefaults.cardColors(containerColor = brightPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Saved Photos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}