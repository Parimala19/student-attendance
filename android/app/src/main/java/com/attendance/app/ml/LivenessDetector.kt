package com.attendance.app.ml

import com.google.mlkit.vision.face.Face

class LivenessDetector {

    private val eyeOpenHistory = mutableListOf<Boolean>()
    private val headMovementHistory = mutableListOf<Triple<Float, Float, Float>>()
    private var lastBlinkTime = 0L

    companion object {
        private const val EYE_CLOSED_THRESHOLD = 0.3f
        private const val BLINK_DURATION_MS = 300L
        private const val MIN_BLINKS_FOR_LIVENESS = 1
        private const val HEAD_MOVEMENT_THRESHOLD = 10f
        private const val HISTORY_SIZE = 30
    }

    data class LivenessResult(
        val isLive: Boolean,
        val blinkDetected: Boolean,
        val headMovementDetected: Boolean,
        val confidence: Float
    )

    fun analyze(face: Face): LivenessResult {
        val currentTime = System.currentTimeMillis()

        // Analyze eye openness for blink detection
        val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
        val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2f

        val eyesOpen = avgEyeOpen > EYE_CLOSED_THRESHOLD
        eyeOpenHistory.add(eyesOpen)
        if (eyeOpenHistory.size > HISTORY_SIZE) {
            eyeOpenHistory.removeAt(0)
        }

        // Detect blinks (eyes closed then open)
        val blinkDetected = detectBlink(currentTime)

        // Analyze head movement
        val headEulerX = face.headEulerAngleX
        val headEulerY = face.headEulerAngleY
        val headEulerZ = face.headEulerAngleZ

        headMovementHistory.add(Triple(headEulerX, headEulerY, headEulerZ))
        if (headMovementHistory.size > HISTORY_SIZE) {
            headMovementHistory.removeAt(0)
        }

        val headMovementDetected = detectHeadMovement()

        // Calculate confidence based on multiple factors
        val confidence = calculateLivenessConfidence(blinkDetected, headMovementDetected)

        val isLive = confidence > 0.5f

        return LivenessResult(
            isLive = isLive,
            blinkDetected = blinkDetected,
            headMovementDetected = headMovementDetected,
            confidence = confidence
        )
    }

    private fun detectBlink(currentTime: Long): Boolean {
        if (eyeOpenHistory.size < 5) return false

        val recentHistory = eyeOpenHistory.takeLast(10)
        var blinkFound = false

        for (i in 1 until recentHistory.size - 1) {
            val wasClosed = !recentHistory[i]
            val wasOpenBefore = recentHistory[i - 1]
            val wasOpenAfter = recentHistory[i + 1]

            if (wasClosed && wasOpenBefore && wasOpenAfter) {
                if (currentTime - lastBlinkTime > BLINK_DURATION_MS) {
                    lastBlinkTime = currentTime
                    blinkFound = true
                }
            }
        }

        return blinkFound
    }

    private fun detectHeadMovement(): Boolean {
        if (headMovementHistory.size < 2) return false

        val first = headMovementHistory.first()
        val last = headMovementHistory.last()

        val xDiff = kotlin.math.abs(last.first - first.first)
        val yDiff = kotlin.math.abs(last.second - first.second)
        val zDiff = kotlin.math.abs(last.third - first.third)

        return xDiff > HEAD_MOVEMENT_THRESHOLD ||
                yDiff > HEAD_MOVEMENT_THRESHOLD ||
                zDiff > HEAD_MOVEMENT_THRESHOLD
    }

    private fun calculateLivenessConfidence(
        blinkDetected: Boolean,
        headMovementDetected: Boolean
    ): Float {
        var confidence = 0f

        if (blinkDetected) confidence += 0.5f
        if (headMovementDetected) confidence += 0.3f

        // Bonus for having both indicators
        if (blinkDetected && headMovementDetected) confidence += 0.2f

        return confidence.coerceIn(0f, 1f)
    }

    fun reset() {
        eyeOpenHistory.clear()
        headMovementHistory.clear()
        lastBlinkTime = 0L
    }
}
