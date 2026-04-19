package com.attendance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "embeddings")
@TypeConverters(FloatArrayConverter::class)
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val embedding: FloatArray,
    val captureAngle: String,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingEntity

        if (id != other.id) return false
        if (studentId != other.studentId) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (captureAngle != other.captureAngle) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + captureAngle.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

class FloatArrayConverter {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
    private val adapter: JsonAdapter<List<Float>> = moshi.adapter(listType)

    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return adapter.toJson(value.toList())
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        return adapter.fromJson(value)?.toFloatArray() ?: floatArrayOf()
    }
}
