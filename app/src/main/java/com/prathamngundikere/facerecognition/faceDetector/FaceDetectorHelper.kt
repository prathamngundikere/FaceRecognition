package com.prathamngundikere.facerecognition.faceDetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import java.io.ByteArrayOutputStream

class FaceDetectorHelper(
    var threshold: Float = THRESHOLD_DEFAULT,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val context: Context,
    var faceDetectorListener: DetectorListener? = null
) {

    private var faceDetector: FaceDetector? = null

    init {
        setupFaceDetector()
    }

    fun clearFaceDetector() {
        faceDetector?.close()
        faceDetector = null
    }

    fun setupFaceDetector() {
        val baseOptionsBuilder = BaseOptions.builder()

        when(currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }
        }

        val modelName = "blaze_face_short_range.tflite"

        baseOptionsBuilder.setModelAssetPath(modelName)

        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (faceDetectorListener == null) {
                    throw IllegalStateException(
                        "faceDetectorListener must be set when runningMode is LIVE_STREAM"
                    )
                }
            }
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                //no op
            }
        }

        try {
            val optionsBuilder =
                FaceDetector.FaceDetectorOptions.builder()
                    .setBaseOptions(baseOptionsBuilder.build())
                    .setMinDetectionConfidence(threshold)
                    .setRunningMode(runningMode)

            when (runningMode) {
                RunningMode.IMAGE,
                RunningMode.VIDEO -> optionsBuilder.setRunningMode(runningMode)
                RunningMode.LIVE_STREAM ->
                    optionsBuilder.setRunningMode(runningMode)
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            faceDetector = FaceDetector.createFromOptions(
                context,
                options
            )
        } catch (e: IllegalStateException) {
            faceDetectorListener?.onError(
                "Face detector failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLIte failed to load model with error: "+e.message)
        } catch (e: RuntimeException) {
            faceDetectorListener?.onError(
                "Face detector failed to initialize. See logs for details", GPU_ERROR
            )
            Log.e(TAG, "Face detector failed to load model with error: "+e.message)
        }
    }

    fun isClosed(): Boolean {
        return faceDetector == null
    }

    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): ResultBundle? {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call video File while not using RunningModel.Video"
            )
        }

        if (faceDetector == null) return null

        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        var retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong()
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        if (
            (videoLengthMs == null) ||
            (width == null) ||
            (height == null)
        ) return null

        val resultList = mutableListOf<FaceDetectorResult>()
        val numberOffFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        for (i in 0..numberOffFrameToRead) {
            val timestamp = i * inferenceIntervalMs

            retriever
                .getFrameAtTime(
                    timestamp * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                    else frame.copy(Bitmap.Config.ARGB_8888, false)

                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    faceDetector?.detectForVideo(mpImage, timestamp)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        }?: {
                            didErrorOccurred = true
                        faceDetectorListener?.onError(
                            "ResultsBundle could not be returned in detect video file"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    faceDetectorListener?.onError(
                        "Frame at specified time could not be retrieved when detecting video"
                    )
                }
        }
        retriever.release()
        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOffFrameToRead)

        return if (didErrorOccurred) {
            null
        } else {
            ResultBundle(resultList, inferenceTimePerFrameMs, height, width)
        }
    }

    fun detectLiveStreamFrame(imageProxy: ImageProxy) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLivestreamFrame" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }

        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer = imageProxy.toBitMap()

        // Copy out RGB bits from the frame to a bitmap buffer
        //val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)

        //imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        //imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
        }

        val rotatedBitmap =
            Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        detectAsync(mpImage, frameTime)
        imageProxy.close()
    }

    private fun ImageProxy.toBitMap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        uBuffer.get(nv21, ySize, uSize)
        vBuffer.get(nv21, ySize + uSize, vSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0,0,width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        faceDetector?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: FaceDetectorResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        faceDetectorListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        faceDetectorListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    // Accepted a Bitmap and runs face detection inference on it to return results back
    // to the caller
    fun detectImage(image: Bitmap): ResultBundle? {

        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }

        if (faceDetector == null) return null

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap face to an MPImage face to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run face detection using MediaPipe Face Detector API
        faceDetector?.detect(mpImage)?.also { detectionResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(detectionResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        // If faceDetector?.detect() returns null, this is likely an error. Returning null
        // to indicate this.
        return null
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val THRESHOLD_DEFAULT = 0.5F
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        const val TAG = "FaceDetectorHelper"
    }

    interface DetectorListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }

    data class ResultBundle(
        val results: List<FaceDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

}