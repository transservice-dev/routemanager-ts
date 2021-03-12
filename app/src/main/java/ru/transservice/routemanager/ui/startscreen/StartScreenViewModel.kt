package ru.transservice.routemanager.ui.startscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.repositories.RootRepository
import java.lang.IllegalArgumentException

class StartScreenViewModel(): ViewModel() {

    private val repository = RootRepository
    var currentTask: Task? = null

    class StartScreenViewModelFactory(): ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(StartScreenViewModel::class.java)){
                return StartScreenViewModel() as T
            }else{
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
            }
        }

    }

    fun loadData(reload: Boolean = false){

    }




}