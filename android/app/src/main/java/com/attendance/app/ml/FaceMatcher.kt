package com.attendance.app.ml

import kotlin.math.sqrt

class FaceMatcher {

    companion object {
        private const val MATCH_THRESHOLD = 0.6f
    }

    data class MatchResult(
        val isMatch: Boolean,
        val similarity: Float,
        val matchedStudentId: String? = null
    )

    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) {
            "Embeddings must have the same size"
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val magnitude = sqrt(norm1) * sqrt(norm2)
        return if (magnitude > 0) {
            (dotProduct / magnitude).toFloat()
        } else {
            0f
        }
    }

    fun findBestMatch(
        queryEmbedding: FloatArray,
        candidateEmbeddings: Map<String, List<FloatArray>>
    ): MatchResult {
        var bestSimilarity = 0f
        var bestStudentId: String? = null

        for ((studentId, embeddings) in candidateEmbeddings) {
            for (embedding in embeddings) {
                val similarity = cosineSimilarity(queryEmbedding, embedding)
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestStudentId = studentId
                }
            }
        }

        return MatchResult(
            isMatch = bestSimilarity >= MATCH_THRESHOLD,
            similarity = bestSimilarity,
            matchedStudentId = if (bestSimilarity >= MATCH_THRESHOLD) bestStudentId else null
        )
    }

    fun matchSingleStudent(
        queryEmbedding: FloatArray,
        studentEmbeddings: List<FloatArray>
    ): MatchResult {
        var maxSimilarity = 0f

        for (embedding in studentEmbeddings) {
            val similarity = cosineSimilarity(queryEmbedding, embedding)
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
            }
        }

        return MatchResult(
            isMatch = maxSimilarity >= MATCH_THRESHOLD,
            similarity = maxSimilarity
        )
    }

    fun euclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) {
            "Embeddings must have the same size"
        }

        var sumSquaredDiff = 0.0
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sumSquaredDiff += diff * diff
        }

        return sqrt(sumSquaredDiff).toFloat()
    }
}
