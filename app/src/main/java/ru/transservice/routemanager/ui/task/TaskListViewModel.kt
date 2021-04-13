package ru.transservice.routemanager.ui.task

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.utils.ImageFileProcessing
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.FlexibleTypeDeserializer

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RootRepository
    private var pointList: MutableLiveData<List<PointItem>> = MutableLiveData()
    private var currentPoint: MutableLiveData<PointItem> = MutableLiveData()
    var currentFileOrder: PhotoOrder = PhotoOrder.DONT_SET
    var reasonComment: String = ""
    var geoIsRequired = MutableLiveData(false)
    val fileBeforeIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    val fileAfterIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    val fileCantDoneIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    private val query = MutableLiveData("")
    private val fullList = MutableLiveData(true)

    val mediatorListResult = MediatorLiveData<List<PointItem>>()

    class TaskListViewModelFactory(private val application: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
           if (modelClass.isAssignableFrom(TaskListViewModel::class.java)) {
               return TaskListViewModel(application) as T
           }else{
               throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
           }
        }

    }

    init {
        Log.d("RouteManager", "${this::class.java}:init loadPointList()")
        loadPointList()
    }

    fun initPointData() {
        setFilesInfo()
        updateGeoIsRequired()
        if (reasonComment.isEmpty()) {
            reasonComment = currentPoint.value?.reasonComment ?: ""
        }
    }

    fun loadPointList() : MutableLiveData<List<PointItem>>{
        repository.getPointList {taskList->
            pointList.postValue(taskList)
        }
        return pointList
    }

    fun getCurrentPoint(): MutableLiveData<PointItem>{
        return currentPoint
    }

    fun updateCurrentPointDB(){
        currentPoint.value?.let {
            repository.updatePoint(currentPoint.value!!)
        }
    }

    fun setCurrentPoint(point: PointItem){
        currentPoint.value = point
        reasonComment = point.reasonComment
    }

    fun setFilesInfo(){
        currentPoint.value?.let {
            getFileAfterIsDone()
            getFileBeforeIsDone()
            getFileCantDoneIsDone()
        }
    }

    fun getFileBeforeIsDone(): LiveData<Boolean> {
        repository.getPointFilesByOrder(currentPoint.value!! , PhotoOrder.PHOTO_BEFORE){
            fileBeforeIsDone.postValue(it.isNotEmpty())
        }
        return fileBeforeIsDone
    }

    fun getFileAfterIsDone(): LiveData<Boolean> {
        repository.getPointFilesByOrder(currentPoint.value!!, PhotoOrder.PHOTO_AFTER){
            fileAfterIsDone.postValue(it.isNotEmpty())
        }
        return fileAfterIsDone
    }

    fun getFileCantDoneIsDone(): LiveData<Boolean> {
        repository.getPointFilesByOrder(currentPoint.value!!, PhotoOrder.PHOTO_CANTDONE){
            fileCantDoneIsDone.postValue(it.isNotEmpty())
        }
        return fileCantDoneIsDone
    }

    fun setPointFilesGeodata(location: Location) {
        repository.getGeolessPointFiles(currentPoint.value!!){ list ->
            list.forEach{
                if (it.filePath.isNotEmpty()){
                    val lon = location.longitude
                    val lat = location.latitude
                    ImageFileProcessing.createResultImageFile(it.filePath,lat,lon,currentPoint.value!!,AppClass.appliactionContext())
                    ImageFileProcessing.setGeoTag(location, it.filePath)
                    repository.updatePointFileLocation(it, lat, lon){
                        Log.d(TAG, "update point file location, point file: ${it.filePath}, lat: $lat, lon: $lon")
                    }
                }
            }
            updateGeoIsRequired()
        }
    }

    fun updateGeoIsRequired(){
        repository.getGeolessPointFiles(currentPoint.value!!) { list ->
            geoIsRequired.postValue(list.isNotEmpty())
        }
    }

    fun savePointFile(file: File, location: Location?){

        location?.let {
           ImageFileProcessing.setGeoTag(location, file.absolutePath)
        }

        val exifInterface = androidx.exifinterface.media.ExifInterface(file.absoluteFile)
        val latLon = exifInterface.latLong
        var lat = 0.0
        var lon = 0.0
        if (latLon != null) {
            lat = latLon[0]
            lon = latLon[1]
        } else {
            Log.d(TAG, "location is not defined ${file.absolutePath}")
            Toast.makeText(getApplication(), "Предупреждение, местоположение не определено", Toast.LENGTH_LONG).show()
        }

        val pointFile = PointFile(
                currentPoint.value!!.docUID, currentPoint.value!!.lineUID, Date(file.lastModified()), currentFileOrder,
                lat,
                lon,
                file.absolutePath, file.name, file.extension
        )

        repository.insertPointFile(pointFile) {

            val resultPoint = currentPoint.value!!.copy()
            if (currentFileOrder == PhotoOrder.PHOTO_AFTER && !currentPoint.value!!.done) {
                resultPoint.done = true
                resultPoint.timestamp = Date()
                updateCurrentPoint(resultPoint)
                Toast.makeText(getApplication(), "Точка выполнена!", Toast.LENGTH_LONG)
                    .show()
            }

            if (currentFileOrder == PhotoOrder.PHOTO_CANTDONE) {
                resultPoint.timestamp = Date()
                updateCurrentPoint(resultPoint)
            }

            uploadPointFiles()
        }
    }


    fun updateCurrentPoint(pointItem: PointItem){
        currentPoint.value = pointItem
        currentPoint.value?.let {
            repository.updatePoint(currentPoint.value!!)
        }
    }

    fun uploadPointFiles(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (geoIsRequired.value == false) repository.uploadFilesOnSchedule()
        }
    }

    fun getPhoneNumber() : String{
        return currentPoint.value!!.getPhoneFromComment()
    }

    fun handleSearchQuery(text: String) {
        query.value = text
    }

    fun setFullList(value: Boolean){
        fullList.value = value
    }

    fun addSources(){
        val filterF = {
            val queryStr = query.value!!
            val points: List<PointItem> =
                    if (pointList.value == null) listOf() else pointList.value!!
            //if (points.isNotEmpty()) {
                mediatorListResult.value = when {
                    queryStr.isNotEmpty() && fullList.value == false -> points
                            .filter { it.addressName.contains(queryStr,true) }
                            .filter { !it.done }
                    queryStr.isNotEmpty() && fullList.value == true -> points
                            .filter { it.addressName.contains(queryStr,true) }
                    queryStr.isEmpty() && fullList.value == false -> points
                            .filter { !it.done }
                    else -> points
                }
            //}
        }

        mediatorListResult.addSource(pointList) { filterF.invoke() }
        mediatorListResult.addSource(query) { filterF.invoke() }
        mediatorListResult.addSource(fullList) { filterF.invoke() }
    }

    fun removeSources(){
        mediatorListResult.removeSource(pointList)
        mediatorListResult.removeSource(query)
        mediatorListResult.removeSource(fullList)
    }

    companion object {
        private const val TAG = "${AppClass.TAG}: TaskList_View_Model"
    }
}