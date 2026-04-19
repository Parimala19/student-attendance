package com.attendance.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmbeddingGenerator(private val context: Context) {

    private var interpreter: Interpreter? = null

    companion object {
        private const val MODEL_INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 512
        private const val PIXEL_SIZE = 3
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // NOTE: Place the MobileFaceNet TFLite model file in app/src/main/assets/mobilefacenet.tflite
            // For now, this is a placeholder that will fail at runtime if model is not present
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            // Uncomment when model file is available:
            // val modelFile = loadModelFile("mobilefacenet.tflite")
            // interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Returns a face embedding using the TFLite model when available, or falls
     * back to a normalised pixel descriptor so the full flow works without a model.
     */
    fun generateEmbedding(faceBitmap: Bitmap): FloatArray =
        generateEmbeddingWithModel(faceBitmap) ?: generatePixelEmbedding(faceBitmap)

    private fun generateEmbeddingWithModel(faceBitmap: Bitmap): FloatArray? {
        if (interpreter == null) return null

        val resizedBitmap = Bitmap.createScaledBitmap(
            faceBitmap,
            MODEL_INPUT_SIZE,
            MODEL_INPUT_SIZE,
            true
        )

        val inputBuffer = preprocessImage(resizedBitmap)
        val outputArray = Array(1) { FloatArray(EMBEDDING_SIZE) }

        interpreter?.run(inputBuffer, outputArray)

        return outputArray[0]
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(
            MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * PIXEL_SIZE * 4
        )
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            val normalizedR = (r - IMAGE_MEAN) / IMAGE_STD
            val normalizedG = (g - IMAGE_MEAN) / IMAGE_STD
            val normalizedB = (b - IMAGE_MEAN) / IMAGE_STD

            inputBuffer.putFloat(normalizedR)
            inputBuffer.putFloat(normalizedG)
            inputBuffer.putFloat(normalizedB)
        }

        return inputBuffer
    }

    /**
     * 16×16 grayscale + colour-axis pixel descriptor, L2-normalised → 512 floats.
     * Used as a stand-in when the TFLite face embedding model is not present.
     * Both enrolment and check-in must use the same function so embeddings are comparable.
     */
    fun generatePixelEmbedding(bitmap: Bitmap): FloatArray {
        val size = 16
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val raw    = FloatArray(size * size * 2)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16 and 0xFF) / 255f
            val g = (pixels[i] shr 8  and 0xFF) / 255f
            val b = (pixels[i]        and 0xFF) / 255f
            raw[i * 2]     = (r + g + b) / 3f   // luminance
            raw[i * 2 + 1] = r - b               // colour axis
        }
        val mag = kotlin.math.sqrt(raw.fold(0f) { acc, v -> acc + v * v })
        val result = FloatArray(512)
        if (mag > 0f) raw.forEachIndexed { i, v -> result[i] = v / mag }
        else          raw.forEachIndexed { i, v -> result[i] = v }
        return result
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
