package ru.transservice.routemanager.ui.startscreen

import androidx.lifecycle.*
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.entities.TaskWithData
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import ru.transservice.routemanager.workmanager.UploadResultWorker
import java.util.*

class StartScreenViewModel : ViewModel() {

    private val repository = RootRepository
    private val currentTask = repository.observeTask().asLiveData()

    private val uploadWorkerId: MutableLiveData<UUID?> = MutableLiveData()

    val version get() = AppClass.appVersion

    private val uploadingIsNotFinished = MutableLiveData(false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        checkForIncompleteWork()
    }

    fun getUploadingIsNotFinished() = uploadingIsNotFinished

    fun getUploadWorkerId() = uploadWorkerId

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

    fun finishRoute(defectsInfo: String) = viewModelScope.launch {
        repository.updateTask(
            currentTask.value!!.task.copy(defects = defectsInfo)
        )
        startUploadWorker()
    }

    fun startUploadWorker(){
        // cancel all previous work for uploading files
        WorkManager.getInstance(AppClass.appliactionContext()).cancelAllWorkByTag(UploadFilesWorker.workerTag)
        val request = UploadResultWorker.requestOneTimeWorkExpedited()
        uploadWorkerId.value = request.id
        WorkManager.getInstance(AppClass.appliactionContext())
            .enqueueUniqueWork(
                UploadResultWorker.workerTag,
                ExistingWorkPolicy.KEEP,
                request)
    }

    fun cancelUploadWorker() {
        WorkManager.getInstance(AppClass.appliactionContext()).cancelAllWorkByTag(UploadResultWorker.workerTag)
        uploadWorkerId.value = null
        checkForIncompleteWork()
    }

    //Check if there is any incomplete work
    fun checkForIncompleteWork()  {
        viewModelScope.launch {
            val workInfo =
                WorkManager.getInstance(AppClass.appliactionContext())
                    .getWorkInfosByTag(UploadResultWorker.workerTag)
                    .get()

            workInfo?.let {
                uploadingIsNotFinished.postValue(
                    workInfo.any {
                        !it.state.isFinished
                    }
                )
                uploadWorkerId.value = workInfo.lastOrNull { !it.state.isFinished }?.id
            }
            _isLoading.value = false

        }
    }

    fun testCoroutines(){
        repository.testCoroutines()
    }

    fun getTaskParams(): LiveData<TaskWithData> {
        return currentTask
    }

}