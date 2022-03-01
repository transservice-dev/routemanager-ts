package ru.transservice.routemanager.ui.point

import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.utils.ImageFileProcessing
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import java.io.File
import java.util.*

class PointItemViewModel(pointId: String) : ViewModel() {

    private val repository = RootRepository
    val state: LiveData<PointWithData> = repository.observePointItemState(pointId).asLiveData()
    var pointStatus: PointStatuses = PointStatuses.NOT_VISITED
    var reasonComment: String = ""

    companion object {
        private const val TAG = "${AppClass.TAG}: TaskList_View_Model"
    }

    class Factory(val pointId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PointItemViewModel(pointId) as T
        }
    }

    init {
        setGeoDataForGeolessFiles()
    }

    fun initPointData() {
        if (reasonComment.isEmpty()) {
            reasonComment = state.value?.point?.reasonComment ?: ""
        }
    }

    //TODO move to data layer
    fun savePointFile(file: File, location: Location?, fileOrder: PhotoOrder){

        location?.let {
            ImageFileProcessing().setGeoTag(location, file.absolutePath)
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
            //Toast.makeText(getApplication(), "Предупреждение, местоположение не определено", Toast.LENGTH_LONG).show()
        }

        //TODO check for null
        val pointFile = PointFile(
            state.value!!.point.docUID, state.value!!.point.lineUID, Date(file.lastModified()), fileOrder,
            lat,
            lon,
            file.absolutePath, file.name, file.extension
        )

        repository.insertPointFile(pointFile) {
            WorkManager.getInstance(AppClass.appliactionContext())
                .enqueue(UploadFilesWorker.requestOneTimeWork(workDataOf(UploadFilesWorker.fileId to pointFile.id)))

            if (fileOrder == PhotoOrder.PHOTO_AFTER || fileOrder == PhotoOrder.PHOTO_CANTDONE) {
                state.value?.let { it ->
                    val resultPoint = it.point.copy()
                    updatePointAndDoneStatus(resultPoint)
                }
            }
        }
    }

    //TODO move to data layer
    private fun setGeoDataForGeolessFiles() {
        viewModelScope.launch {
            NavigationServiceConnection.getLocationFlow().collect { location ->
                if (location == null || state.value == null) return@collect
                repository.getGeolessPointFiles(state.value!!.point) { list ->
                    list.forEach {
                        if (it.filePath.isNotEmpty()) {
                            val lon = location.longitude
                            val lat = location.latitude
                            val imageProcessing = ImageFileProcessing()
                            imageProcessing.createResultImageFile(
                                it.filePath,
                                lat,
                                lon,
                                state.value!!.toPointFileParams(it.photoOrder),
                                AppClass.appliactionContext()
                            )
                            imageProcessing.setGeoTag(location, it.filePath)
                            repository.updatePointFileLocation(it, lat, lon) {
                                Log.d(
                                    TAG,
                                    "update point file location, point file: ${it.filePath}, lat: $lat, lon: $lon"
                                )
                            }
                        }
                    }
                }

            }
        }
    }

    private fun updateCurrentPoint(pointItem: PointItem){
        repository.updatePoint(pointItem)
        repository.updatePointOnServer(pointItem)
    }

    fun updateUndonePoint(){
        /*currentPoint.value?.let { pointItem ->
            val resultPoint = pointItem.copy(reasonComment =  reasonComment, tripNumberFact = 2000)
            updatePointAndDoneStatus(resultPoint)
        }*/
        state.value?.let{ pointState ->
            val resultPoint =  pointState.point
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

    fun setFact(fact: Double){
        state.value?.let { pointState ->
            val resultPoint = pointState.point.copy(countFact = fact)
            resultPoint.setCountOverFromPlanAndFact()
            updatePointAndDoneStatus(resultPoint)
        }
    }

    fun setPolygonForPoint(polygon: PolygonItem) {
        state.value?.let { pointState ->
            val resultPoint = pointState.point.copy(polygonUID = polygon.uid, polygonName = polygon.name)
            updatePointAndDoneStatus(resultPoint)
        }
    }

    fun getPhoneNumber() : String{
        return state.value?.point?.getPhoneFromComment() ?: ""
    }

    //TODO move to data layer
    private fun updatePointAndDoneStatus(point: PointItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkPointForCompletion(point) { canBeDone ->
                val statusChanged = point.done != canBeDone
                point.done = canBeDone
                if (statusChanged) {
                    point.timestamp = Date()
                    if (reasonComment != "") {
                        point.reasonComment = reasonComment
                    }
                    pointStatus = if (canBeDone) PointStatuses.DONE else PointStatuses.CANNOT_DONE
                }
                when {
                    point.done ->
                        point.status = PointStatuses.DONE
                    !point.done && point.countFact != 0.0 && point.countFact != -1.0 ->
                        point.status = PointStatuses.NOT_VISITED
                    !point.done && point.countFact == 0.0 ->
                        point.status = PointStatuses.CANNOT_DONE
                    !point.done && point.reasonComment != "" ->
                        point.status = PointStatuses.CANNOT_DONE
                }

                if (point.done && point.tripNumberFact == 2000)
                    point.tripNumberFact = 1000
                if (point.done && point.polygonByRow && point.tripNumberFact >= 1000)
                    repository.getTaskValue().let {
                        point.tripNumberFact = it.lastTripNumber + 1
                    }
                updateCurrentPoint(point)
            }
        }
    }

}
