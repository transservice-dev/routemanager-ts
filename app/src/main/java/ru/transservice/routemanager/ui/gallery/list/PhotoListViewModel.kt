package ru.transservice.routemanager.ui.gallery.list

import android.text.BoringLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.repositories.RootRepository
import java.lang.IllegalArgumentException

class PhotoListViewModel(var pointItem: PointItem?) : ViewModel() {

    private val repository = RootRepository
    private var pointList: MutableLiveData<List<PointItem>> = MutableLiveData()
    private var pointFilesList: MutableLiveData<List<PointFile>> = MutableLiveData()
    var selectionMode: MutableLiveData<Boolean> =  MutableLiveData(false)
    var selectedItems: MutableList<PointFile> = mutableListOf()

    class PhotoListViewModelFactory(val pointItem: PointItem?) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoListViewModel::class.java)) {
                return PhotoListViewModel(pointItem) as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    init {
        loadPointList()
    }

    fun loadPointList() : MutableLiveData<List<PointItem>>{
       if (pointItem != null){
          pointList.postValue(listOf(pointItem!!))
       }else{
           repository.getPointsWithFiles {
               pointList.postValue(it)
           }
       }

        return pointList
    }

    fun setPointFilesList(list: List<PointFile>){
        pointFilesList.value = list
    }

    fun getPointFilesList(): MutableLiveData<List<PointFile>>{
        return pointFilesList
    }


    /*fun loadPointFilesList(pointItem: PointItem) : MutableLiveData<List<PointFile>>{
        repository.getPointFiles(pointItem) {
            pointFilesList.postValue(it)
        }
        return pointFilesList
    }*/

}