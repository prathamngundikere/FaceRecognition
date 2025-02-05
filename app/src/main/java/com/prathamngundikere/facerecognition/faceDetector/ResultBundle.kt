package com.prathamngundikere.facerecognition.faceDetector

import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

data class ResultBundle(
    val results: List<FaceDetectorResult>,
    val inferenceTime: Long,
    val inputImageHeight: Int,
    val inputImageWidth: Int
)
