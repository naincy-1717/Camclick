package com.example.camclick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onOpenCameraClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(8.dp)
        ) {
            Text(text = "Open Camera")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSavedPhotosClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(8.dp)
        ) {
            Text(text = "Saved Photos")
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