package ru.transservice.routemanager.repositories

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.data.local.entities.SearchType
import ru.transservice.routemanager.data.local.entities.Task
import java.text.SimpleDateFormat
import java.util.*

object PreferencesRepository {

    private const val URL_NAME = "URL_NAME"
    private const val URL_PORT = "URL_PORT"
    private const val URL_AUTHPASS = "URL_AUTHPASS"
    private const val VEHICLE = "VEHICLE"
    private const val ROUTE = "ROUTE"
    private const val REGION = "REGION"
    private const val ROUTE_DATE = "ROUTE_DATE"
    private const val SEARCH_BY_ROUTE = "SEARCH_BY_ROUTE"
    private const val USE_GOOGLE_NAV = "USE_GOOGLE_NAV"

    private val prefs: SharedPreferences by lazy {
        val ctx = AppClass.appliactionContext()
        PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    fun getUrlName(): String {
        return prefs.getString(URL_NAME,"") as String
    }

    fun getUrlPort(): String {
        return prefs.getString(URL_PORT,"") as String
    }

    fun getUrlPass(): String {
        return prefs.getString(URL_AUTHPASS,"") as String
    }

    fun getVehicle(): VehicleItem? {
        val currentValue = prefs.getString(VEHICLE,null)
        return currentValue?.let {
            try {
                Gson().fromJson(currentValue,VehicleItem::class.java)
            }catch (e: Exception){
                null
            }
        }

    }

    fun getRegion(): RegionItem? {
        val currentValue = prefs.getString(REGION,null)
        return currentValue?.let {
            try {
                Gson().fromJson(currentValue,RegionItem::class.java)
            }catch (e: Exception){
                null
            }
        }
    }

    fun getRoute(): RouteItem? {
        val currentValue = prefs.getString(ROUTE,null)
        return currentValue?.let {
            try {
                Gson().fromJson(currentValue,RouteItem::class.java)
            }catch (e: Exception){
                null
            }
        }
    }

    fun getSearchBYRoute(): Boolean {
        return prefs.getBoolean(SEARCH_BY_ROUTE,false)
    }

    fun getUseNavGoogle(): Boolean {
        return prefs.getBoolean(USE_GOOGLE_NAV,false)
    }

    fun getDate(): Date? {
        val currentValue = prefs.getString(ROUTE_DATE,null)
        return currentValue?.let {
          try { SimpleDateFormat("yyyy.MM.dd",
                Locale("ru")).parse(it) }
          catch (e: Exception){
              null
          }
        }
    }

    fun getTask(): Task {
        return Task(
            "",
            getVehicle(),
            getRoute(),
            getDate() ?: Date(),
            if (getSearchBYRoute()) SearchType.BY_ROUTE else SearchType.BY_VEHICLE
        )
    }

    private fun putValue(pair: Pair<String,Any>) = with(prefs.edit()) {
        val key = pair.first
        val value = pair.second
        when (value){
            is String -> putString(key,value)
            is Int -> putInt(key,value)
            is Boolean -> putBoolean(key,value)
            is Float -> putFloat(key,value)
            is Long -> putLong(key,value)
            else -> error("Only primitive types can be stored in Shared Preferences")
        }
        apply()
    }

    fun saveVehicle(vehicleItem: VehicleItem){
        val gson = Gson()
        putValue(VEHICLE to gson.toJson(vehicleItem) )
    }

    fun saveRegion(regionItem: RegionItem){
        val gson = Gson()
        putValue(REGION to gson.toJson(regionItem) )
    }

    fun saveRoute(routeItem: RouteItem){
        val gson = Gson()
        putValue(ROUTE to gson.toJson(routeItem) )
    }

    fun saveDate(routeDate: Date){
        putValue(ROUTE_DATE to SimpleDateFormat(
                "yyyy.MM.dd",
            Locale("ru")
        ).format(routeDate) )
    }

}