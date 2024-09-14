package com.example.rotatingbox

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rotatingbox.ui.theme.RotatingBoxTheme
import com.example.rotatingbox.util.BitmapUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RotatingBoxTheme {
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                var selectedBackgroundColor by remember { mutableStateOf(Color.Black) }
                val context = LocalContext.current
                val pickSinglePhoto = rememberLauncherForActivityResult(contract =
                ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
                    uri?.let { selectedImageUri = it }
                })
                LaunchedEffect(selectedImageUri) {
                    val time = measureTimeMillis {
                        withContext(Dispatchers.Default) {
                            val bitmap = selectedImageUri?.let { BitmapUtil.uriToBitmap(context, it) }
                            val selectedColor = bitmap?.let { BitmapUtil.findDominantColor(it) }
                            if (selectedColor != null) {
                                selectedBackgroundColor = selectedColor
                            }
                        }
                    }
                    Log.d("timeTaken","$time")

                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RotatingBoxScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        backgroundColor = selectedBackgroundColor,
                        onLaunchMediaPicker = {
                            pickSinglePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RotatingBoxScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onLaunchMediaPicker: () -> Unit
) {
    Surface(modifier = modifier, color = backgroundColor) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RotatingBox(modifier = Modifier)
            Button(onClick = {
                onLaunchMediaPicker()
            }) {
                Text("Pick Image")
            }
        }

    }
}


@Composable
fun RotatingBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(250.dp)) {
        rotate(rotation % 360) {
            drawRect(color = Color(50, 205, 50))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RotatingBoxPreview() {
    RotatingBox(modifier = Modifier.size(100.dp))
}