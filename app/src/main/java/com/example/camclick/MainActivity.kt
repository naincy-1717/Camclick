package com.example.camclick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camclick.ui.theme.CamclickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamclickTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LandingScreen(
                        onOpenCameraClick = {
                            // Camera screen navigation will go here
                        },
                        onSavedPhotosClick = {
                            // Gallery screen navigation will go here
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
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
    // Bright Hot Pink Color
    val brightPink = Color(0xFFFF1493)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Dark background so the pink & white borders pop out
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Camera Box
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

        // Saved Photos Box
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

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    CamclickTheme {
        LandingScreen(
            onOpenCameraClick = {},
            onSavedPhotosClick = {}
        )
    }
}