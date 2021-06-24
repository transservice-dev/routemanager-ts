package ru.transservice.routemanager.location

import android.content.ComponentName
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder

object NavigationServiceConnection: ServiceConnection {

    private lateinit var navService: NavigationService
    private var bound: Boolean = false

    override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
        val binder = service as NavigationService.ServiceBinder
        navService = binder.getService()
        bound = true
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
       bound = false
    }

    fun getLocation(): Location?{
        return if (bound) {
            navService.getLocation()
        }else{
            null
        }
    }

    fun getlocationAvailable(): Boolean{
        return navService.getlocationAvailable()
    }

    fun isActive():Boolean{
        return navService.isActive
    }

    fun setNavClient(){
        navService.setNavClient()
    }

    fun startTracking() {
        navService.startTracking()
    }

    fun stopTracking() {
        navService.stopTracking()
    }

}