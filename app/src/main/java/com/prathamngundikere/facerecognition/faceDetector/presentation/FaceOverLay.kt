package com.prathamngundikere.facerecognition.faceDetector.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.components.containers.Detection
import kotlin.math.min

@Composable
fun FaceOverlay(
    faces: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    preViewWidth: Float,
    preViewHeight: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = preViewWidth / imageWidth
        val scaleY = preViewHeight / imageHeight
        faces.forEach { face ->
            val rect = face.boundingBox()

            // Scale factor to convert from image coordinates to preview coordinates


            // Scale and position the box
            val left = rect.left * scaleX
            val top = rect.top * scaleY
            val width = rect.width() * scaleX
            val height = rect.height() * scaleY
            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )
        }
    }
}