package ru.transservice.routemanager.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.location.Geocoder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.work.WorkManager
import androidx.work.workDataOf
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.photoDir
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.utils.TextUtils
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService

class PhotoViewModel : ViewModel() {
    lateinit var photo: Photo
}

class Photo(val point: PointItem,val photoType:PhotoOrder) {
    //Собственные поля
    lateinit var date: Date
    lateinit var file: File

    var hasLocation:Boolean = false
    var lat: Double = 0.0
    var lon: Double = 0.0
    var address:String = ""

    val dateText: String
        get() = SimpleDateFormat("yyyy-MM-dd (EEE) HH:mm:ss",Locale("ru")).format(date)
    val locText:String
        get() = if (lat != 0.0 && lon != 0.0) {String.format("%.6f", lat) + "   " +  String.format("%.6f", lon)} else ""

    fun asPointFile(): PointFile {
            return PointFile(
                point.docUID,
                point.lineUID,
                date,
                photoType,
                lat,
                lon,
                file.absolutePath,
                file.name,
                file.extension
            )
        }
}

class PhotoProcessing(val ctx: Context, val photo:Photo) {
    val file = photo.file
    val exif = ExifInterface(file)
    var bitmap = BitmapFactory.decodeFile(file.absolutePath)

    companion object {
        private const val TAG = "Photo.PhotoProcessing"
        private val EXIF_TAGS = arrayOf(
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_RW2_ISO,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_WHITE_BALANCE
        )
    }

    fun prepare(): Bitmap {
        resizeBitmap()
        rotateByExif()
        save()
        Log.d(TAG,"prepare(): finished / ${photo.file.absolutePath}")
        return bitmap
    }

    fun execute():Bitmap {
        setAddress()
        applyStamp()
        save()
        setNewExif()

        Log.d(TAG,"execute(): finished / ${photo.file.absolutePath}")
        return bitmap
    }


    private fun resizeBitmap() {
        val maxDimension = 1024
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return

        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        val oldBitmap = bitmap
        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
        oldBitmap.recycle()
    }

    private fun rotateByExif() {
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> 0
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_UNDEFINED -> {
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL.toString())
                0
            }
            else ->0
        }
        if (degrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true)
        }
    }

    private fun setAddress() {
        if (photo.address != "") return
        if (!photo.hasLocation) return
        val geocoder = Geocoder(ctx, Locale("ru"))
        try {
            val addresses = geocoder.getFromLocation(photo.lat, photo.lon, 1)
            photo.address = addresses[0].getAddressLine(0)
        } catch (e: Exception) {
            Log.e(TAG,"setAddress(): raise $e")
        }
    }

    private fun applyStamp() {
        val stamp = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

        // Вывод в холст
        val canvas = Canvas(stamp)

        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawBitmap(bitmap,0F,0F,paint)

        val startX = 20
        val endX = bitmap.width - startX
        val baseY = (0.75*bitmap.height).toInt()
        val lineHeight = (0.25*bitmap.height/40).toInt()

        val rtGray = Rect(0,baseY,bitmap.width,bitmap.height)
        val rtAddress = Rect(startX,baseY + lineHeight,endX,baseY + 28*lineHeight)
        val rtLoc = Rect(startX,baseY + 29*lineHeight,endX,baseY + 34*lineHeight)
        val rtDate = Rect(startX,baseY + 35*lineHeight,endX,baseY + 40*lineHeight)

        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(ctx, R.color.colorGrayBack)
        canvas.drawRect(rtGray, paint)

        paint.color = Color.WHITE
        canvas.drawTextInRect(photo.address,rtAddress, paint,4)
        canvas.drawTextInRect(photo.dateText,rtDate, paint)
        canvas.drawTextInRect(photo.locText,rtLoc, paint)

        bitmap = stamp
    }

    fun save() {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos)
        //write the bytes in file
        val fos = BufferedOutputStream(FileOutputStream(photo.file))
        fos.write(bos.toByteArray())
        fos.flush()
        fos.close()
    }

    fun setNewExif() {
        val newExif = ExifInterface(file)
        for (tag in EXIF_TAGS) {
            val value = exif.getAttribute(tag)
            if (value != null) { newExif.setAttribute(tag, value) }
        }
        newExif.saveAttributes()
    }
}

object PhotoFacade {
    private const val TAG = "Photo.PhotoRepository"
    private const val PHOTO_EXTENSION = ".jpg"

    private val repo = RootRepository

    fun create(point:PointItem, photoType: PhotoOrder):Photo {
        val photo = Photo(point,photoType)
        photo.date = Date()

        val timeCreated = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("RU")).format(photo.date)

        var nameFirst = "${photo.point.routeName}__${timeCreated}__${photo.point.addressName}"
        nameFirst = nameFirst.filter { it.isLetterOrDigit() || it.isWhitespace() || it.toString() == "_" }

        val nameLast = "_${photo.photoType.string}${PHOTO_EXTENSION}"
        val nameLastSize = nameLast.toByteArray().size
        while (nameFirst.toByteArray().size + nameLastSize > 255) {
            nameFirst = nameFirst.dropLast(1)
        }

        val outputDirectory = AppClass.instance.photoDir

        photo.file = File(outputDirectory, "$nameFirst$nameLast")
        if (photo.file.exists()) { photo.file.delete() }

        val location = NavigationServiceConnection.getLocation()
        if (location != null) {
            photo.hasLocation = true
            photo.lat = location.latitude
            photo.lon = location.longitude
        } else {
            photo.hasLocation = false
            photo.lat = 0.0
            photo.lon = 0.0
        }
        Log.d(TAG,"Location: ${photo.lat} x ${photo.lon}")
        return photo
    }

    fun make(
        photo:Photo,
        imageCapture: ImageCapture,
        cameraExecutor: ExecutorService,
        currentLens:Int,
        onOk:()->Unit
    ) {
        //Получаем изображение с камеры
        val metadata = Metadata()
        metadata.isReversedHorizontal = (currentLens == CameraSelector.LENS_FACING_FRONT)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photo.file)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) { onOk()}
            }
        )
    }

    fun saveInDb(photo: Photo, callback: () -> Unit) {
        val pf = photo.asPointFile()
        repo.insertPointFile(pf,callback)
    }
}

fun Canvas.drawTextInRect(
    text: String,
    rect: Rect,
    paint: Paint,
    minLines:Int = 1
) {
    if (text.isEmpty() || rect.width() <= 0 || rect.height() <= 0) return

    var minLinesInner = minLines
    val fitRatio = 0.92F
    val originalTextSize = paint.textSize

    paint.textSize = fitRatio*rect.height().toFloat()/minLinesInner
    var lines = TextUtils.splitToLines(text, rect.width().toFloat(), paint)
    while (lines.size>minLinesInner) {
        minLinesInner += 1
        paint.textSize = fitRatio*rect.height()/minLinesInner
        lines = TextUtils.splitToLines(text, rect.width().toFloat(), paint)
    }

    val lineHeight = (-paint.ascent() + paint.descent()).toInt()
    val ascent = paint.ascent().toInt()

    var currentY = rect.top - ascent

    lines.forEach { line ->
        val x = rect.left
        drawText(line, x.toFloat(), currentY.toFloat(), paint)
        currentY += lineHeight
    }

    paint.textSize = originalTextSize
}
