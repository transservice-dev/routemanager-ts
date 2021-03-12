package ru.transservice.routemanager.ui.routesettings

import android.net.LinkAddress
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.repositories.PreferencesRepository
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.lang.IllegalArgumentException
import java.util.*

class RouteSettingsViewModel(): ViewModel() {

    private val prefRepository = PreferencesRepository
    private val repository = RootRepository
    private val regionList: MutableLiveData<List<RegionItem>> = MutableLiveData()
    private var currentRegion = MutableLiveData(prefRepository.getRegion())
    private var currentVehicle = MutableLiveData(prefRepository.getVehicle())
    private var currentRoute = MutableLiveData(prefRepository.getRoute())
    private var currentDate = MutableLiveData(prefRepository.getDate())

    class RouteSettingsViewModelFactory: ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RouteSettingsViewModel::class.java)){
                return RouteSettingsViewModel() as T
            }else
                throw IllegalArgumentException("Unknown class: Expected ${this::class.java} found $modelClass")
        }
    }

    fun getRegion(): MutableLiveData<RegionItem?> {
        return currentRegion
    }

    fun getVehicle(): MutableLiveData<VehicleItem?> {
        return currentVehicle
    }

    fun getRoute(): MutableLiveData<RouteItem?> {
        return currentRoute
    }

    fun getDate(): MutableLiveData<Date?> {
        return  if (currentDate == null) MutableLiveData(Date()) else currentDate!!
    }

    fun loadRegions(): MutableLiveData<LoadResult<List<RegionItem>>> {
        val result: MutableLiveData<LoadResult<List<RegionItem>>> =
            MutableLiveData(LoadResult.Loading())
        repository.getRegions { regionResList ->
            result.postValue(LoadResult.Success(regionResList.map { it.toRegionItem() }))
        }
        return result
    }

    fun loadVehicle(): MutableLiveData<LoadResult<List<VehicleItem>>> {
        val result: MutableLiveData<LoadResult<List<VehicleItem>>> =
            MutableLiveData(LoadResult.Loading())
        currentRegion.value?.let{
            repository.getVehiclesByRegion(currentRegion.value!!) { vehicleResList ->
                result.postValue(LoadResult.Success(vehicleResList.map { it.toVehicleItem() }))
            }
        }
        return result
    }

    fun setRegion(regionItem: RegionItem){
        currentRegion.value = regionItem
        prefRepository.saveRegion(regionItem)
    }

    fun setVehicle(vehicleItem: VehicleItem){
        currentVehicle.value = vehicleItem
        prefRepository.saveVehicle(vehicleItem)
    }

    fun setDate(date: Date){
        currentDate.value = date
        prefRepository.saveDate(date)
    }


}