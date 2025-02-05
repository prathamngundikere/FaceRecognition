package com.prathamngundikere.facerecognition.faceDetector.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.prathamngundikere.facerecognition.faceDetector.FaceDetectorClassifier
import com.prathamngundikere.facerecognition.faceDetector.FaceDetectorClassifier.DetectorListener
import com.prathamngundikere.facerecognition.faceDetector.ResultBundle
import com.prathamngundikere.facerecognition.faceRecognition.FaceRecognitionClassifier
import kotlin.use

@Composable
fun FaceDetectionScreen(
    context: Context
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    var scaleFactor by remember { mutableFloatStateOf(1f) }

    var imageWidth by remember {mutableIntStateOf(0)}
    var imageHeight by remember {mutableIntStateOf(0)}
    var faceDetectorResult by remember {
        mutableStateOf<List<FaceDetectorResult>>(emptyList())
    }

    var croppedFacesList by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val faceDetectorClassifier = remember {
        FaceDetectorClassifier(
            context = context,
            faceDetectorListener = object : DetectorListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("FaceDetection", "Error: $error")
                }

                override fun onResults(resultBundle: ResultBundle) {
                    faceDetectorResult = resultBundle.results
                    imageWidth = resultBundle.inputImageWidth
                    imageHeight = resultBundle.inputImageHeight
                }
            }
        )
    }

    val faceRecognitionClassifier = remember {
        FaceRecognitionClassifier(
            faceRecognitionListener = object : FaceRecognitionClassifier.RecognitionListener {
                override fun getCroppedFaces(croppedFaces: List<Bitmap>) {
                    croppedFacesList = croppedFaces
                }

            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            CameraPreview(
                cameraProviderFuture = cameraProviderFuture,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                imageProxyHandler = { image ->
                    val bitmapBuffer = Bitmap.createBitmap(
                        image.width,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        //Only for front camera
                        postScale(-1f, 1f, image.width.toFloat(), image.height.toFloat())
                    }

                    val bitmap = Bitmap
                        .createBitmap(
                            bitmapBuffer,
                            0, 0,
                            bitmapBuffer.width, bitmapBuffer.height,
                            matrix,
                            true
                        )
                    image.close()
                    faceDetectorClassifier.detectLiveStreamFrame(
                        rotatedBitmap = bitmap
                    )
                    faceDetectorResult.forEach {
                        faceRecognitionClassifier.cropFacesFromImageProxy(
                            rotatedBitmap = bitmap,
                            results = it,
                            scaleFactor = scaleFactor
                        )
                    }
                }
            )
            faceDetectorResult.forEach {
                FaceOverlay(
                    results = it,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    onScaleFactorCalculated = { factor ->
                        scaleFactor = factor
                    }
                )
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(croppedFacesList) {
                Image(
                    modifier = Modifier.size(200.dp),
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Cropped Face",
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}