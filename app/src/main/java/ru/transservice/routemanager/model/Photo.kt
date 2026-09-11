package ru.transservice.routemanager.model

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import com.google.gson.Gson
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.photoDir
import ru.transservice.routemanager.utils.Utils
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService

class Photo
{
    lateinit var routeName:String
    lateinit var addressName:String
    lateinit var fileOrder:String
    var currentLens:Int = CameraSelector.LENS_FACING_BACK

    lateinit var file: File

    lateinit var date:Date

    lateinit var onOk:(Photo) -> Unit

    fun make(imageCapture: ImageCapture,cameraExecutor: ExecutorService) {
        //Подготавливаем файл
        date = Date()
        val timeCreated = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("RU")).format(date)

        var fileName = "${routeName}__${timeCreated}__${addressName}"
        fileName = fileName.filter { it.isLetterOrDigit() || it.isWhitespace() || it.toString() == "_" }

        val filePostfixSize = "_${fileOrder}${PHOTO_EXTENSION}".toByteArray().size
        while (fileName.toByteArray().size + filePostfixSize > 255) {
            fileName = fileName.dropLast(1)
        }

        val outputDirectory = AppClass.instance.photoDir

        file = File(outputDirectory, "$fileName$PHOTO_EXTENSION")
        if (file.exists()) { file.delete() }

        //Получаем изображение с камеры
        val metadata = Metadata()
        metadata.isReversedHorizontal = (currentLens == CameraSelector.LENS_FACING_FRONT)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) = makeOnError(exc)
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = makeOnOk(output)
            }
        )
    }

    private fun makeOnOk(output: ImageCapture.OutputFileResults) {
        Log.d(TAG, "Photo capture succeeded: ${this.file.absolutePath}")
        onOk(this)
    }

    private fun makeOnError(exc: ImageCaptureException) {
        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
    }

    fun pack(): String {
        val data = PhotoData(routeName,addressName,fileOrder,file.absolutePath)
        val json = Utils.toJson(data)
        return json
    }

    companion object {
        private const val TAG = "Photo.make"
        private const val PHOTO_EXTENSION = ".jpg"

        fun unpack(json:String):Photo {
            val photoData: PhotoData = Utils.fromJson(json, PhotoData::class.java)
            val photo = Photo()
            photo.routeName = photoData.routeName
            photo.addressName = photoData.addressName
            photo.fileOrder = photoData.fileOrder
            photo.file = File(photoData.filePath)
            return photo
        }
    }
}

class PhotoData(
    val routeName: String,
    val addressName: String,
    val fileOrder: String,
    val filePath: String
)