package ru.transservice.routemanager.ui.polygon

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.repositories.RootRepository
import java.util.*

class PolygonViewModel : ViewModel()  {
    private val repository = RootRepository
    private val query = MutableLiveData("")
    val fullList = MutableLiveData(false)
    private val currentItem: MutableLiveData<PolygonItem> = MutableLiveData()
    val mediatorListResult = MediatorLiveData<List<PolygonItem>>()
    private var polygonList: MutableLiveData<List<PolygonItem>> = MutableLiveData()
    private var docPolygonList: MutableLiveData<List<PolygonItem>> = MutableLiveData()

    class Factory: ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PolygonViewModel::class.java)){
                return PolygonViewModel() as T
            }else
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
        }
    }

    fun loadAvailablePolygons(): MutableLiveData<List<PolygonItem>> {
        repository.getPolygonList {pList->
            polygonList.postValue(pList)
            if (pList.isEmpty() and !fullList.value!!)
                fullList.postValue(true)
        }
        return polygonList
    }

    fun loadCurrentPolygons(): MutableLiveData<List<PolygonItem>> {
        repository.getPolygonCurrentList {pList->
            docPolygonList.postValue(pList)
        }
        return docPolygonList
    }

    fun getDocPolygonList(): MutableLiveData<List<PolygonItem>> {
        return docPolygonList
    }

    fun setPolygonByDefault() {
        repository.getNextPolygon { polygon ->
            polygon?.let {
                currentItem.postValue(it)
            }
        }
    }

    fun getPolygon() : MutableLiveData<PolygonItem>{
        return currentItem
    }

    fun addNewPolygonToPointList() {
        viewModelScope.launch {
            val currentTask = repository.getTaskValue()
            if (!currentTask.isEmpty() && currentItem.value != null) {
                currentTask.lastTripNumber += 1
                val newPoint = PointItem(
                    docUID = currentTask.docUid,
                    lineUID = UUID.randomUUID().toString(),
                    addressName = currentItem.value!!.name,
                    polygonUID = currentItem.value!!.uid,
                    polygon = true,
                    tripNumber = currentTask.lastTripNumber
                )
                repository.addPolygon(newPoint, currentTask)
            }
        }
    }

    fun handleSearchQuery(text: String) {
        query.value = text
    }

    fun setFullList(value: Boolean){
        fullList.value = value
    }

    fun setPolygon(item: PolygonItem){
        currentItem.postValue(item)
    }

    fun changePolygonRow(polygonRowValue: PolygonItem, newValue: PolygonItem) {
        repository.updatePolygon(polygonRowValue.copy(uid = newValue.uid, name = newValue.name)) {
            loadCurrentPolygons()
        }
    }

    fun addSources(){
        val filterF = {
            val queryStr = query.value!!
            val polygons: List<PolygonItem> =
                if (polygonList.value == null) listOf() else polygonList.value!!
            mediatorListResult.value = when {
                queryStr.isNotEmpty() && fullList.value == false -> polygons
                    .filter { it.name.contains(queryStr,true) }
                    .filter { it.by_default }
                queryStr.isNotEmpty() && fullList.value == true -> polygons
                    .filter { it.name.contains(queryStr,true) }
                    .filter { !it.by_default}
                queryStr.isEmpty() && fullList.value == false -> polygons
                    .filter { it.by_default }
                queryStr.isEmpty() && fullList.value == true -> polygons
                    .filter { !it.by_default }
                else -> polygons
            }
        }

        mediatorListResult.addSource(polygonList) { filterF.invoke() }
        mediatorListResult.addSource(query) { filterF.invoke() }
        mediatorListResult.addSource(fullList) { filterF.invoke() }
    }

    fun removeSources(){
        mediatorListResult.removeSource(polygonList)
        mediatorListResult.removeSource(query)
        mediatorListResult.removeSource(fullList)
    }

    companion object {
        private const val TAG = "${AppClass.TAG}: PolygonViewModel"
    }
}