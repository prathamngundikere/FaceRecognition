package com.prathamngundikere.facerecognition.faceDetector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class FaceDetectorClassifier(
    val context: Context,
    var faceDetectorListener: DetectorListener? = null
) {
    private val modelName = "blaze_face_short_range.tflite"
    private var faceDetector: FaceDetector? = null
    private val tag = "FaceDetectorClassifier"

    init {
        setupFaceDetector()
    }

    fun setupFaceDetector() {
        Log.i(tag, "In setupFaceDetector")
        val baseOptionsBuilder = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath(modelName)
            .build()

        val optionBuilder =
            FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder)
                .setMinDetectionConfidence(0.5f)
                .setResultListener(this::returnLiveStreamResult)
                .setErrorListener(this::returnLiveStreamError)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .build()

        faceDetector = FaceDetector.createFromOptions(
            context, optionBuilder
        )
        Log.i(tag, " setupFaceDetector is completed")
    }

    fun detectLiveStreamFrame(
        rotatedBitmap: Bitmap
    ) {
        Log.i(tag, "In detectLiveStreamFrame")
        val frameTime = SystemClock.uptimeMillis()

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        faceDetector?.detectAsync(mpImage, frameTime)
        Log.i(tag, "The live is done")
    }

     fun returnLiveStreamResult(
        result: FaceDetectorResult,
        input: MPImage
    ) {
         Log.i(tag, "In returnLiveStreamResult")
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

         faceDetectorListener?.onResults(
             ResultBundle(
                 results = listOf(result),
                 inferenceTime = inferenceTime,
                 inputImageHeight = input.height,
                 inputImageWidth = input.width
             )
         )
         Log.i(tag, "LiveStreamResult is completed $result Inference time $inferenceTime ")
    }

     fun returnLiveStreamError(error: RuntimeException) {
         faceDetectorListener?.onError(
             error.message ?: "An unknown error has occurred"
         )
         Log.e("FaceDetectionClassifier", "Error $error")
    }

    interface DetectorListener {
        fun onError(error: String, errorCode: Int = 0)
        fun onResults(
            resultBundle: ResultBundle
        )
    }
}