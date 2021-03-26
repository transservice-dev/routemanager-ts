package ru.transservice.routemanager.ui.point

import android.location.Location
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.utils.ImageFileProcessing
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

class PointViewModel(var pointItem: PointItem) : ViewModel() {

    private val repository = RootRepository

    var currentFileOrder: PhotoOrder = PhotoOrder.DONT_SET
    var reasonComment: String = ""
    var geoIsRequired: Boolean = false
    val fileBeforeIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    val fileAfterIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentPoint: MutableLiveData<PointItem> = MutableLiveData()

    class PointViewModelFactory(val pointItem: PointItem) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PointViewModel::class.java)) {
                return PointViewModel(pointItem) as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    fun setViewModelData(point: PointItem){
        currentPoint.value = point
        getFileAfterIsDone()
        getFileBeforeIsDone()
    }

    fun getFileBeforeIsDone(): LiveData<Boolean>{
        repository.getPointFilesByOrder(pointItem, PhotoOrder.PHOTO_BEFORE){
            fileBeforeIsDone.postValue(it.isNotEmpty())
        }
        return fileBeforeIsDone
    }

    fun getFileAfterIsDone(): LiveData<Boolean>{
        repository.getPointFilesByOrder(pointItem, PhotoOrder.PHOTO_AFTER){
            fileAfterIsDone.postValue(it.isNotEmpty())
        }
        return fileAfterIsDone
    }

    fun savePointFile(file: File, location: Location){

        ImageFileProcessing.setGeoTag(location, file.absolutePath)

        val exifInterface = androidx.exifinterface.media.ExifInterface(file.absoluteFile)
        val latLon = exifInterface.latLong
        var lat = 0.0
        var lon = 0.0
        if (latLon!=null) {
            lat = latLon[0]
            lon = latLon[1]
        }

        val pointFile = PointFile(
            currentPoint.value!!.docUID, currentPoint.value!!.lineUID, Date(file.lastModified()), currentFileOrder,
            lat,
            lon,
            file.absolutePath, file.name, file.extension
        )

        repository.insertPointFile(pointFile) {
            updatePointAfterPicture()
        }
    }

    fun updatePointAfterPicture(){

        if (currentFileOrder == PhotoOrder.PHOTO_AFTER && !currentPoint.value!!.done) {
            currentPoint.value!!.done = true
            currentPoint.value!!.timestamp = Date()
            repository.updatePoint(currentPoint.value!!)
            /*Toast.makeText(requireContext(), "Точка выполнена!", Toast.LENGTH_LONG)
                .show()*/
        }

        if (currentFileOrder == PhotoOrder.PHOTO_CANTDONE) {
            if (currentPoint.value!!.reasonComment.isEmpty()) {
                currentPoint.value!!.reasonComment = (reasonComment)
            }
            currentPoint.value!!.timestamp = Date()
            repository.updatePoint(currentPoint.value!!)
        }

    }

    fun updateCurrentPoint(pointItem: PointItem){
        currentPoint.value = pointItem
        currentPoint.value?.let {
            repository.updatePoint(currentPoint.value!!)
        }

    }

}