package ru.transservice.routemanager.ui.startscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.repositories.PreferencesRepository
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.lang.Exception
import java.lang.IllegalArgumentException

class StartScreenViewModel(): ViewModel() {

    private val repository = RootRepository
    private val currentTask = Transformations.map(repository.getCurrentTask()) {
        return@map it
    }

    private val uploadResult: MutableLiveData<LoadResult<Boolean>> = MutableLiveData()

    class StartScreenViewModelFactory(): ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(StartScreenViewModel::class.java)){
                return StartScreenViewModel() as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    fun syncTaskData(reload: Boolean = false): MutableLiveData<LoadResult<Task>>{
        val result: MutableLiveData<LoadResult<Task>> =
            MutableLiveData(LoadResult.Loading())
        repository.syncData { loadResult ->
            result.postValue(loadResult)
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finishRoute(): MutableLiveData<LoadResult<Boolean>> {
        try {
            uploadResult.value = LoadResult.Loading()
            repository.uploadResult { loadResult ->
                uploadResult.postValue(loadResult)
            }
        }catch (e: Exception){
            uploadResult.value = LoadResult.Error("Ошибка при выгрузке данных ;{e.message ?: \"Неизвестная ошибка\"}")
        }

        return uploadResult
    }

    fun getTaskParams(): LiveData<Task> {
        return currentTask
    }

    fun getUploadResult(): MutableLiveData<LoadResult<Boolean>> {
        return uploadResult
    }
}