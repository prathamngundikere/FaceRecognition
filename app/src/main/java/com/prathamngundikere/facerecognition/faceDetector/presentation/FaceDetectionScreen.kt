package com.prathamngundikere.facerecognition.faceDetector.presentation

import android.content.Context
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.prathamngundikere.facerecognition.faceDetector.FaceDetectorHelper

@Composable
fun FaceDetectionScreen(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var detectedFaces by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imageWidth by remember {mutableIntStateOf(0)}
    var imageHeight by remember {mutableIntStateOf(0)}

    val faceDetectorHelper = remember {
        FaceDetectorHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            faceDetectorListener = object : FaceDetectorHelper.DetectorListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("FaceDetection", "Error: $error")
                }

                override fun onResults(resultBundle: FaceDetectorHelper.ResultBundle) {
                    detectedFaces = resultBundle.results.flatMap { it.detections() }
                    imageWidth = resultBundle.inputImageWidth
                    imageHeight = resultBundle.inputImageHeight
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            cameraProviderFuture = cameraProviderFuture,
            lifecycleOwner = lifecycleOwner,
            faceDetectorHelper = faceDetectorHelper,
            modifier = Modifier.fillMaxSize()
        )
        val previewSize = LocalConfiguration.current
        FaceOverlay(
            faces = detectedFaces,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            preViewWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() },
            preViewHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() },
            modifier = Modifier.fillMaxSize()
        )
    }
}