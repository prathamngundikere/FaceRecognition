package com.prathamngundikere.facerecognition.faceRecognition

import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class FaceRecognitionClassifier(
    var faceRecognitionListener: RecognitionListener? = null
) {

    fun cropFacesFromImageProxy(
        rotatedBitmap: Bitmap,
        results: FaceDetectorResult,
        scaleFactor: Float
    ) {
        // Crop faces
        val croppedFaces = mutableListOf<Bitmap>()
        results.detections().forEach { detection ->
            val boundingBox = detection.boundingBox()

            var left = (boundingBox.left * scaleFactor).toInt().coerceIn(0, rotatedBitmap.width)
            var top = (boundingBox.top * scaleFactor).toInt().coerceIn(0, rotatedBitmap.height)
            var right = (boundingBox.right * scaleFactor).toInt().coerceIn(0, rotatedBitmap.width)
            var bottom = (boundingBox.bottom * scaleFactor).toInt().coerceIn(0, rotatedBitmap.height)

            val width = (right - left)
            val height = (bottom - top)

            val maxSide = maxOf(width, height)

            val centerX = boundingBox.centerX().toInt()
            val centerY = boundingBox.centerY().toInt()

            val newLeft = (centerX - maxSide / 2).coerceIn(0, rotatedBitmap.width - maxSide)
            val newTop = (centerY - maxSide / 2).coerceIn(0, rotatedBitmap.height - maxSide)

            try {
                val faceBitmap = Bitmap.createBitmap(rotatedBitmap, newLeft, newTop, maxSide, maxSide)
                croppedFaces.add(faceBitmap)
            } catch (e: Exception) {
                Log.e("FaceCrop", "Error Cropping the image: ${e.message}")
            }
        }

        faceRecognitionListener?.getCroppedFaces(croppedFaces)
    }

    interface RecognitionListener {
        fun getCroppedFaces(
            croppedFaces: List<Bitmap>
        )
    }
}