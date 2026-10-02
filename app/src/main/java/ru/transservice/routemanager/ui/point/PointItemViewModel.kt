package ru.transservice.routemanager.ui.point

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.repositories.RootRepository
import java.util.*

class PointItemViewModel(lineUID: String) : ViewModel() {

    private val repository = RootRepository
    val state: LiveData<PointWithData> = repository.observePointItemState(lineUID).asLiveData()
    var pointStatus: PointStatuses = PointStatuses.NOT_VISITED
    var reasonComment: String = ""

    companion object {
        private const val TAG = "${AppClass.TAG}: TaskList_View_Model"
    }

    class Factory(val lineUID: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PointItemViewModel(lineUID) as T
        }
    }

    fun initPointData() {
        if (reasonComment.isEmpty()) {
            reasonComment = state.value?.point?.reasonComment ?: ""
        }
    }

    private fun updateCurrentPoint(pointItem: PointItem){
        repository.updatePoint(pointItem)
        repository.updatePointOnServer(pointItem)
    }

    fun updateUndonePoint(){
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
    fun updatePointAndDoneStatus(point: PointItem) {
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
