package ru.transservice.routemanager.ui.gallery.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.repositories.RootRepository

class PhotoListViewModel(var pointItem: PointItem?) : ViewModel() {

    private val repository = RootRepository
    private var pointList: MutableLiveData<List<PointItem>> = MutableLiveData()
    private var pointFilesList: MutableLiveData<List<PointFile>> = MutableLiveData()
    var selectedItems: MutableLiveData<MutableList<PointFile>> = MutableLiveData(mutableListOf())
    val state: PhotoListState = PhotoListState(pointItem, mutableListOf(), ::handleSelection, ::navRequest)

    //private val _navParams = MutableStateFlow(NavParams())
    //val navParams: StateFlow<NavParams> = _navParams.asStateFlow()

    private val _navParams = MutableSharedFlow<NavParams>()
    val navParams: SharedFlow<NavParams> = _navParams.asSharedFlow()


    class Factory(val pointItem: PointItem?) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoListViewModel::class.java)) {
                return PhotoListViewModel(pointItem) as T
            } else {
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    init {
        loadPointList()
    }


    //class Factory: ViewModelProvider.Factory


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


    //for viewpager2 Gallery Fragment
    fun getPointFilesList(): MutableLiveData<List<PointFile>>{
        return pointFilesList
    }

    fun setPointFilesList(list: List<PointFile>){
        pointFilesList.value = list
    }


    data class PhotoListState(
        val pointItem: PointItem?,
        var selectedItems: MutableList<PointFile>,
        val handleSelection: (pointFile: PointFile, remove: Boolean) -> Unit,
        val navRequest: (PointItem?, Int, List<PointFile>) -> Unit
    )

    private fun handleSelection(pointFile: PointFile, remove: Boolean = false) {
        val currentList = selectedItems.value
        if (remove) {
            currentList?.remove(pointFile)
        } else {
            currentList?.add(pointFile)
        }

        currentList?.let {
            selectedItems.value = it
            state.selectedItems = it
        }
    }

    data class NavParams(val pointItem: PointItem? = null, val position: Int = 0, var isRequired: Boolean = false)

    private fun navRequest(pointItem: PointItem?, position: Int, list: List<PointFile>) {
        pointFilesList.value = list
        viewModelScope.launch {
            _navParams.emit(NavParams(pointItem,position,true))
        }
        /*_navParams.update {
            NavParams(pointItem,position,true)
        }*/
    }

    fun navRequestComplete() {
        /*_navParams.update {
            _navParams.value.copy(isRequired = false)
        }*/
    }
}