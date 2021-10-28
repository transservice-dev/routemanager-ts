package ru.transservice.routemanager.ui.startscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.lang.IllegalArgumentException

class StartScreenViewModel : ViewModel() {

    private val repository = RootRepository
    private val currentTask = Transformations.map(repository.getTask()) {
        it
    }
    val version get() = AppClass.appVersion

    private val uploadResult: MutableLiveData<LoadResult<Boolean>> = MutableLiveData()

    /*private val selfObserver = Observer<LoadResult<Boolean>> { _ ->  }

    init {
        uploadResult.observeForever { selfObserver }
    }*/

    class StartScreenViewModelFactory : ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(StartScreenViewModel::class.java)){
                return StartScreenViewModel() as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    fun syncTaskData(): MutableLiveData<LoadResult<Int>>{
        val result: MutableLiveData<LoadResult<Int>> =
            MutableLiveData(LoadResult.Loading())
        repository.syncData { loadResult ->
            result.postValue(loadResult)
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finishRoute(): MutableLiveData<LoadResult<Boolean>> {
        uploadResult.value = LoadResult.Loading()
        repository.uploadResult { loadResult ->
            uploadResult.postValue(loadResult)
        }
        return uploadResult
    }

    fun getTaskParams(): LiveData<Task> {
        return currentTask
    }

    /*fun updateTaskParams() {
        repository.updateCurrentTask()
    }*/

    fun getUploadResult(): MutableLiveData<LoadResult<Boolean>> {
        return uploadResult
    }

}