package com.prathamngundikere.facerecognition.faceDetector.presentation

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
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
    val boxPaint = Paint()
    boxPaint.color.green
    boxPaint.strokeWidth = 8f

    Canvas(modifier = modifier) {
        results?.let {
            val scaleFactor = min(size.width / imageWidth, size.height / imageHeight)

            // Send scaleFactor back to FaceDetectionScreen
            onScaleFactorCalculated(scaleFactor)

            for (detection in it.detections()) {

                val paddingBox = 20f

                val boundingBox = detection.boundingBox()

                val left = boundingBox.left * scaleFactor - paddingBox
                val top = boundingBox.top * scaleFactor - paddingBox
                val right = boundingBox.right * scaleFactor + paddingBox
                val bottom = boundingBox.bottom * scaleFactor + paddingBox

                val width = right - left
                val height = bottom - top

                Log.i("FaceOverlay", "The values - bounding box - $boundingBox left=$left bottom= $bottom")

                // Draw bounding box
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 8f)
                )
            }
        }
    }
}