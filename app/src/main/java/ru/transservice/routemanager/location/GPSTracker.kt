package com.muslimcompanion.utills

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.database.AppDatabase
import java.util.*

class GPSTracker(private val mContext: Context) : Service() {

    // flag for GPS status
    var isGPSEnabled = false
    // flag for network status
    var isNetworkEnabled = false
    // flag for GPS status
    var canGetLocation = false

    var trackingIsOn = false

    var locationGPS: Location? = null
    var locationNetwork: Location? = null

    val locationListenerGPS: LocationListener = object : LocationListener {
        override fun onLocationChanged(newLocation: Location) {
            locationGPS = newLocation
            Log.d(TAG, "Location changed: $newLocation ${Date()}")
        }
    }

    val locationListenerNetwork: LocationListener = object : LocationListener {
        override fun onLocationChanged(newLocation: Location) {
            locationNetwork = newLocation
            Log.d(TAG, "Location changed: $newLocation ${Date()}")
        }
    }

    // Declaring a Location Manager
    var locationManager: LocationManager? = null

    @SuppressLint("MissingPermission")
    fun findLocation() {
        try {
            locationManager = mContext
                .getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // getting network status
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                canGetLocation = false
            } else {
                canGetLocation = true
                // First if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerGPS
                    )
                    trackingIsOn = true
                    Log.d(TAG, "Start tracking GPS location")

                }
                // Then get location from Network Provider
                if (isNetworkEnabled) {

                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerNetwork
                        )
                        trackingIsOn = true
                        Log.d(TAG, "Start tracking Network location")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocation(): Location? {
        var lastLocation: Location? = null
        if (locationManager != null) {
            lastLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation == null) {
                lastLocation = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                Log.d(TAG,"Returned Network Location")
            }
        }
        return lastLocation
    }

    fun stopUsingGPS() {
        if (locationManager != null) {
            locationManager!!.removeUpdates(locationListenerGPS)
            locationManager!!.removeUpdates(locationListenerNetwork)
            trackingIsOn = false
            locationGPS = null
            locationNetwork = null
            Log.d(TAG, "Stop using GPS")
        }
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(mContext)

        // Setting Dialog Title
        alertDialog.setTitle("Настройки GPS")

        // Setting Dialog Message
        alertDialog.setMessage("Обнаружение местоположения отключено.Перейти к настройке?")

        // On pressing Settings button
        alertDialog.setPositiveButton(
            "Настройки"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mContext.startActivity(intent)
            dialog.cancel()
        }
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    companion object {
        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 5 // 5 meter
        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES = (1000 * 30  // 30 seconds
                ).toLong()
        private const val TAG = "${AppClass.TAG}: GPSTracker"

        private var INSTANCE: GPSTracker? = null

        fun getGPSTracker(context: Context): GPSTracker {
            if (INSTANCE == null) {
                INSTANCE = GPSTracker(context)
            }
            return INSTANCE!!
            /*return if (INSTANCE == null) {
               //INSTANCE = GPSTracker(context)
                   GPSTracker(context)
            }else
                return   INSTANCE!!*/
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}