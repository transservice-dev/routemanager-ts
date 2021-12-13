package ru.transservice.routemanager.ui.task

import android.app.Application
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.utils.ImageFileProcessing
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RootRepository
    //private var pointList: MutableLiveData<List<PointItem>> = MutableLiveData()

    private var pointList = Transformations.map(
        repository.getPointListData()
    ){
        it
    }

    var unloadingAvailable = Transformations.map(
        repository.getUnloadingAvailable()
    ){
        it
    }

    private var currentPoint: MutableLiveData<PointItem> = MutableLiveData()
    var currentFileOrder: PhotoOrder = PhotoOrder.DONT_SET
    var reasonComment: String = ""
    var geoIsRequired = MutableLiveData(false)
    val fileBeforeIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    private val fileAfterIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    private val fileCantDoneIsDone: MutableLiveData<Boolean> = MutableLiveData(false)
    private val query = MutableLiveData("")
    private val fullList = MutableLiveData(true)

    val mediatorListResult = MediatorLiveData<List<PointItem>>()

    @Suppress("UNCHECKED_CAST")
    class TaskListViewModelFactory(private val application: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(TaskListViewModel::class.java)) {
                TaskListViewModel(application) as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    /*init {
        Log.d("RouteManager", "${this::class.java}:init loadPointList()")
        loadPointList()
    }*/

    fun initPointData() {
        setFilesInfo()
        updateGeoIsRequired()
        if (reasonComment.isEmpty()) {
            reasonComment = currentPoint.value?.reasonComment ?: ""
        }
    }

    fun deletePolygonFromList(pointItem: PointItem) {
        repository.deletePolygon(pointItem)
    }

    /*fun loadPointList() : MutableLiveData<List<PointItem>>{
        /*repository.getPointList {taskList->
            pointList.postValue(taskList)
        }
        return pointList*/
        return MutableLiveData()
    }*/

    fun getCurrentPoint(): MutableLiveData<PointItem>{
        return currentPoint
    }

    fun setCurrentPoint(point: PointItem){
        currentPoint.value = point
        reasonComment = point.reasonComment
        Log.d(TAG,"Current point changed ${currentPoint.value}")
    }

    private fun setFilesInfo(){
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

    private fun updateGeoIsRequired(){
        currentPoint.value?.let {
            repository.getGeolessPointFiles(it) { list ->
                geoIsRequired.postValue(list.isNotEmpty())
            }
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
            repository.uploadFile(pointFile)

            if (currentFileOrder == PhotoOrder.PHOTO_AFTER || currentFileOrder == PhotoOrder.PHOTO_CANTDONE) {
                currentPoint.value?.let { pointItem ->
                    val resultPoint = pointItem.copy()
                    updatePointAndDoneStatus(resultPoint)
                }
            }

            /*val resultPoint = currentPoint.value!!.copy()
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
            }*/
        }
    }

    fun setPolygonForPoint(polygon: PolygonItem) {
        currentPoint.value?.let { pointItem ->
            val resultPoint = pointItem.copy(polygonUID = polygon.uid, polygonName = polygon.name)
            updatePointAndDoneStatus(resultPoint)
        }
    }

    fun setFactForPoint(fact: Double){
        currentPoint.value?.let { pointItem ->
            val resultPoint = pointItem.copy(countFact = fact)
            resultPoint.setCountOverFromPlanAndFact()
            updatePointAndDoneStatus(resultPoint)
            //updateCurrentPoint(resultPoint)
        }
    }

    private fun updatePointAndDoneStatus(point: PointItem) {
        repository.checkPointForCompletion(point) { canBeDone ->
            val statusChanged = point.done != canBeDone
            point.done = canBeDone
            if (statusChanged) {
                point.timestamp = Date()

                when {
                    point.done -> point.status = PointStatuses.DONE
                    reasonComment != "" ->  {
                        point.status = PointStatuses.CANNOT_DONE
                        point.reasonComment = reasonComment
                    }
                }

                if (point.done) {
                    point.status = PointStatuses.DONE
                }
            }
            if (point.done && point.tripNumberFact == 2000)
                point.tripNumberFact = 1000
            if (point.done && point.polygonByRow && point.tripNumberFact >= 1000)
                repository.getTask().value?.let {
                    point.tripNumberFact = it.lastTripNumber + 1
                }
            updateCurrentPoint(point)
        }
    }

    fun updateUndonePoint(){
        /*currentPoint.value?.let { pointItem ->
            val resultPoint = pointItem.copy(reasonComment =  reasonComment, tripNumberFact = 2000)
            updatePointAndDoneStatus(resultPoint)
        }*/
        currentPoint.value?.let{ point ->
        val resultPoint =  point
            .copy(reasonComment =  reasonComment, tripNumberFact = 2000)
            .also {
                if (it.timestamp == null){
                    it.timestamp = Date()
                }
                it.status = PointStatuses.CANNOT_DONE
            }
            updateCurrentPoint(resultPoint)
        }
    }

    fun updateCurrentPoint(pointItem: PointItem){
        currentPoint.postValue(pointItem)
        repository.updatePoint(pointItem)
        repository.updatePointOnServer(pointItem)
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

    fun changeFullList(): Boolean {
        fullList.value = ! (fullList.value?: false)
        return fullList.value ?: true
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
        //mediatorListResult.addSource(currentPoint) {filterF.invoke()}
    }

    fun removeSources(){
        mediatorListResult.removeSource(pointList)
        mediatorListResult.removeSource(query)
        mediatorListResult.removeSource(fullList)
        //mediatorListResult.removeSource(currentPoint)
    }

    companion object {
        private const val TAG = "${AppClass.TAG}: TaskList_View_Model"
    }
}