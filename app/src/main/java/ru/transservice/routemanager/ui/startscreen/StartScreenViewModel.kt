package ru.transservice.routemanager.ui.startscreen

import androidx.lifecycle.*
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass

import ru.transservice.routemanager.data.local.entities.TaskWithData
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import java.lang.IllegalArgumentException

class StartScreenViewModel : ViewModel() {

    private val repository = RootRepository
    private val currentTask = repository.observeTask().asLiveData()
    val version get() = AppClass.appVersion

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

    fun startUploadWorker(request: WorkRequest){
        // cancel all previous work for uploading files
        WorkManager.getInstance(AppClass.appliactionContext()).cancelAllWorkByTag(UploadFilesWorker.workerTag)
        WorkManager.getInstance(AppClass.appliactionContext())
            .enqueue(request)
    }

    fun getTaskParams(): LiveData<TaskWithData> {
        return currentTask
    }

}