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
        val boxScaleFactor = 1.2f // Increase box size by 20%
        faces.forEach { face ->
            val rect = face.boundingBox()

            // Scale and position the box
            val width = rect.width() * scaleX * boxScaleFactor
            val height = rect.height() * scaleY * boxScaleFactor
            val centerX = rect.centerX() * scaleX
            val centerY = rect.centerY() * scaleY
            val left = centerX - width / 2
            val top = centerY - height / 2
            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )
        }
    }
}