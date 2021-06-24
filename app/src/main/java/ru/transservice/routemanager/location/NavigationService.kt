package ru.transservice.routemanager.location

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.muslimcompanion.utills.GPSTracker
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.repositories.PreferencesRepository
import java.util.*

class NavigationService : Service() {

    private val googlePlayServicesAvailable: Boolean = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
        AppClass.appliactionContext()) == ConnectionResult.SUCCESS
    private var location: Location? = null
    var locationGPS: Location? = null
    var locationNetwork: Location? = null
    private var locationAvailable: Boolean = false
    private lateinit var trackerClient: BaseTracker
    var isActive = false

    private val binder = ServiceBinder()

    companion object {
        private const val TAG = "${AppClass.TAG}: NavigationService"
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startTracking() {
        Log.d(TAG, "Start tracking location")
        trackerClient.startTrackingLocation()
        isActive = true
    }

    fun stopTracking(){
        Log.d(TAG, "Stop tracking location")
        trackerClient.stopTrackingLocation()
        location = null
        locationGPS = null
        location = null
        isActive = false
    }

    fun setNavClient(){
        trackerClient = if (googlePlayServicesAvailable && PreferencesRepository.getUseNavGoogle()){
            GoogleLocationTracker( AppClass.appliactionContext())
        }else{
            LocationManagerTracker( AppClass.appliactionContext())
        }
    }


    /*override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        trackerClient.startTrackingLocation()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        trackerClient.stopTrackingLocation()
        super.onDestroy()
    }
     */

    override fun onCreate() {
        Log.d(TAG, "OnCreate")
        setNavClient()
        super.onCreate()
    }

    fun getLocation(): Location?{
       return if (locationGPS== null) {
           if (locationNetwork == null) {
               location
           }else{
               locationNetwork
           }
       }else{
           locationGPS
       }
    }

    fun getlocationAvailable(): Boolean{
        return locationAvailable
    }

    inner class ServiceBinder : Binder() {
        fun getService() : NavigationService = this@NavigationService
    }

    private abstract class BaseTracker : ILocationClient

    @SuppressLint("MissingPermission")
    private inner class LocationManagerTracker(private val context: Context) : BaseTracker(), ILocationClient {
        // flag for GPS status
        var isGPSEnabled = false
        // flag for network status
        var isNetworkEnabled = false

        private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 5 // 5 meter
        // The minimum time between updates in milliseconds
        private val MIN_TIME_BW_UPDATES = (1000 * 30  // 30 seconds
                ).toLong()

        val locationListenerGPS: LocationListener = object : LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                locationGPS = newLocation
                location = newLocation
                Log.d(TAG, "Location changed: $newLocation ${Date()}")
            }
        }

        val locationListenerNetwork: LocationListener = object : LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                locationNetwork = newLocation
                location = newLocation
                Log.d(TAG, "Location changed: $newLocation ${Date()}")
            }
        }

        // Declaring a Location Manager
        var locationManager: LocationManager? = null

        override fun startTrackingLocation() {
            locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            // getting GPS status
            isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            // getting network status
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                locationAvailable = false
            } else {
                locationAvailable = true
                // First if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerGPS
                    )
                    Log.d(TAG, "Start tracking GPS location")
                }
                // Then get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerNetwork
                    )
                    Log.d(TAG, "Start tracking Network location")
                }
            }
        }

        override fun stopTrackingLocation() {
            if (locationManager != null) {
                locationManager!!.removeUpdates(locationListenerGPS)
                locationManager!!.removeUpdates(locationListenerNetwork)
                Log.d(TAG, "Stop using GPS")
            }
        }

    }

    @SuppressLint("MissingPermission")
    private inner class GoogleLocationTracker(private val context: Context) : BaseTracker(), ILocationClient{

        private var fusedLocationClient: FusedLocationProviderClient

        private val locationRequest = LocationRequest.create().apply {
            fastestInterval = 5000
            interval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1.0f
        }

        private val locationUpdatesCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                location = lr.locations.last()
                Log.d(TAG, "Location changed: $location ${Date()}")
            }
        }


        init {
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)
            locationAvailable = true
        }

        override fun startTrackingLocation() {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationUpdatesCallback,
                Looper.getMainLooper()
            )
        }

        override fun stopTrackingLocation() {
            fusedLocationClient.removeLocationUpdates(locationUpdatesCallback)
        }
    }


}

