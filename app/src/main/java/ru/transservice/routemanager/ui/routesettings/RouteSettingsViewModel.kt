package ru.transservice.routemanager.ui.routesettings

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.repositories.PreferencesRepository
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.util.*

class RouteSettingsViewModel : ViewModel() {

    private val prefRepository = PreferencesRepository
    private val repository = RootRepository
    private var currentRegion = MutableLiveData(prefRepository.getRegion())
    private var currentVehicle = MutableLiveData(prefRepository.getVehicle())
    private var currentRoute = MutableLiveData(prefRepository.getRoute())
    private var currentDate = MutableLiveData(prefRepository.getDate())
    private val query = MutableLiveData("")
    val mediatorListRegionResult = MediatorLiveData<LoadResult<List<RegionItem>>>()
    val mediatorListVehicleResult = MediatorLiveData<LoadResult<List<VehicleItem>>>()
    val mediatorListRouteResult = MediatorLiveData<LoadResult<List<RouteItem>>>()
    private var vehicleList: MutableLiveData<LoadResult<List<VehicleItem>>> = MutableLiveData()
    private var regionList: MutableLiveData<LoadResult<List<RegionItem>>> = MutableLiveData()
    private var routeList: MutableLiveData<LoadResult<List<RouteItem>>> = MutableLiveData()

    private var editingIsAvailable = MutableLiveData(false)
    val searchByRoute get() = prefRepository.getSearchBYRoute()


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
        return currentDate
    }

    fun getEditingIsAvailable(): MutableLiveData<Boolean>{
        editingIsAvailable.value = !repository.getTaskData().isLoaded
        return editingIsAvailable
    }

    fun loadRegions(): MutableLiveData<LoadResult<List<RegionItem>>> {
        regionList.value = LoadResult.Loading()
        repository.loadRegions { regionResList ->
            val list = regionResList
                    .map { it.toRegionItem() }
                    .sortedBy { it.name }
            regionList.postValue(LoadResult.Success(list))
        }
        return regionList
    }

    fun loadVehicle(): MutableLiveData<LoadResult<List<VehicleItem>>> {
        vehicleList.value = LoadResult.Loading()
        currentRegion.value?.let{
            repository.loadVehiclesByRegion(currentRegion.value!!) { vehicleResList ->
                val list = vehicleResList
                        .map { it.toVehicleItem() }
                        .sortedBy { it.number }
                vehicleList.postValue(LoadResult.Success(list))
            }
        }
        return vehicleList
    }

    fun loadRoutes(): MutableLiveData<LoadResult<List<RouteItem>>> {
        routeList.value = LoadResult.Loading()
        currentRegion.value?.let{
            repository.loadRoutesByRegion(currentRegion.value!!) { routeResList ->
                val list = routeResList
                    .map { it.toRouteItem() }
                    .sortedBy { it.name }
                routeList.postValue(LoadResult.Success(list))
            }
        }
        return routeList
    }

    fun setRegion(regionItem: RegionItem){
        currentRegion.value = regionItem
        prefRepository.saveRegion(regionItem)
    }

    fun setVehicle(vehicleItem: VehicleItem){
        currentVehicle.value = vehicleItem
        prefRepository.saveVehicle(vehicleItem)
    }

    fun setRoute(routeItem: RouteItem){
        currentRoute.value = routeItem
        prefRepository.saveRoute(routeItem)
    }

    fun setDate(date: Date){
        currentDate.value = date
        prefRepository.saveDate(date)
    }

    fun handleSearchQuery(text: String) {
        query.value = text
    }

    fun addSourcesVehicle(){
        val filterF = {
            val queryStr = query.value!!
            var vehicles: List<VehicleItem> = listOf()
            vehicleList.value?.let{
                it.data?.let { list ->
                   vehicles = list
                }
            }
            mediatorListVehicleResult.value = if (queryStr.isNotEmpty())
                LoadResult.Success(vehicles
                    .filter { it.number.contains(queryStr, true) })
            else LoadResult.Success(vehicles)

        }

        mediatorListVehicleResult.addSource(vehicleList) { filterF.invoke() }
        mediatorListVehicleResult.addSource(query) { filterF.invoke() }
    }

    fun addSourcesRoute() {
        val filterF = {
            val queryStr = query.value!!
            var routes: List<RouteItem> = listOf()
            routeList.value?.let {
                it.data?.let { list ->
                    routes = list
                }
            }
            mediatorListRouteResult.value = if (queryStr.isNotEmpty())
                LoadResult.Success(routes
                    .filter { it.name.contains(queryStr, true) })
            else LoadResult.Success(routes)
        }
        mediatorListRouteResult.addSource(routeList) { filterF.invoke() }
        mediatorListRouteResult.addSource(query) { filterF.invoke() }
    }

    fun addSourcesRegion() {
        val filterF = {
            val queryStr = query.value!!
            var regions: List<RegionItem> = listOf()
            regionList.value?.let {
                it.data?.let { list ->
                    regions = list
                }
            }

            mediatorListRegionResult.value = if (queryStr.isNotEmpty())
                LoadResult.Success(regions
                    .filter { it.name.contains(queryStr, true) })
            else LoadResult.Success(regions)

        }

        mediatorListRegionResult.addSource(regionList) { filterF.invoke() }
        mediatorListRegionResult.addSource(query) { filterF.invoke() }
    }

    fun removeSources(){
        mediatorListVehicleResult.removeSource(vehicleList)
        mediatorListVehicleResult.removeSource(query)

        mediatorListRegionResult.removeSource(regionList)
        mediatorListRegionResult.removeSource(query)

        mediatorListRouteResult.removeSource(routeList)
        mediatorListRouteResult.removeSource(query)
    }

}