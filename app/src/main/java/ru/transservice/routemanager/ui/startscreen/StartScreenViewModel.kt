package ru.transservice.routemanager.ui.startscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.data.local.entities.TaskWithData
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.lang.IllegalArgumentException

class StartScreenViewModel : ViewModel() {

    private val repository = RootRepository
    private val currentTask = repository.observeTask().asLiveData()

    val version get() = AppClass.appVersion

    private val uploadResult: MutableLiveData<LoadResult<Boolean>> = MutableLiveData()


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
        viewModelScope.launch {
            repository.syncData(repository.getTaskValue()) { loadResult ->
                result.postValue(loadResult)
            }
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finishRoute(): MutableLiveData<LoadResult<Boolean>> {
        uploadResult.value = LoadResult.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            repository.uploadResult { loadResult ->
                uploadResult.postValue(loadResult)
            }
        }
        return uploadResult
    }

    fun getTaskParams(): LiveData<TaskWithData> {
        return currentTask
    }


    fun getUploadResult(): MutableLiveData<LoadResult<Boolean>> {
        return uploadResult
    }

}