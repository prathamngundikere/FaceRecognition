package com.prathamngundikere.facerecognition.faceDetector.presentation

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.prathamngundikere.facerecognition.R
import kotlin.math.min

@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    results: FaceDetectorResult?,
    imageWidth: Int,
    imageHeight: Int,
    onScaleFactorCalculated: (Float) -> Unit
) {
    val context = LocalContext.current
    val boxColor = remember { Color(ContextCompat.getColor(context, R.color.teal_200)) }

    Canvas(modifier = modifier) {
        results?.let {
            val scaleFactor = min(size.width / imageWidth, size.height / imageHeight)

            // Send scaleFactor back to FaceDetectionScreen
            onScaleFactorCalculated(scaleFactor)

            for (detection in it.detections()) {
                val boundingBox = detection.boundingBox()
                val left = boundingBox.left * scaleFactor
                val top = boundingBox.top * scaleFactor
                val right = boundingBox.right * scaleFactor
                val bottom = boundingBox.bottom * scaleFactor

                Log.i("FaceOverlay", "The values - bounding box - $boundingBox left=$left bottom= $bottom")

                // Draw bounding box
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 8f)
                )
            }
        }
    }
}