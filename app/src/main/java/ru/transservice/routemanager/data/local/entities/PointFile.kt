package ru.transservice.routemanager.data.local.entities

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.File
import java.io.FileInputStream
import java.io.Serializable
import java.util.*

@Entity(
        tableName = "pointFiles_table",
        foreignKeys = [ForeignKey(
                entity = PointItem::class,
                parentColumns = ["docUID", "lineUID"],
                childColumns = ["docUID", "lineUID"],
                onDelete = ForeignKey.CASCADE
        )],
        indices = [Index("docUID", "lineUID", "docUID", "lineUID")]
)
data class PointFile(
        val docUID: String,
        val lineUID: String,
        val timeDate: Date,
        var photoOrder: PhotoOrder,
        val lat: Double,
        val lon: Double,
        val filePath: String,
        val fileName: String,
        val fileExtension: String,
        val uploaded: Boolean = false
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    fun exists(): Boolean {
        val f = File(this.filePath)
        return f.exists()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCompresedBase64(): String {

        /*val mBitmap = BitmapFactory.decodeFile(this.filePath)
        val stream = ByteArrayOutputStream()
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        val imageBytes = stream.toByteArray()*/

        val f = File(this.filePath)
        return if (f.exists()) {
            val imageBytes = ByteArray(f.length().toInt())
            val stream = FileInputStream(this.filePath)
            stream.read(imageBytes)
            stream.close()

            Base64.getEncoder().encodeToString(imageBytes)
        } else {
            ""
        }
    }
}

enum class PhotoOrder(val string: String, val title: String) : Serializable {
    PHOTO_BEFORE("before", "до вывоза"), PHOTO_AFTER("after", "после вывоза"), DONT_SET(
                "not set",
                "не указано"
        ),
    PHOTO_CANTDONE("cant_done", "невозможно")
}