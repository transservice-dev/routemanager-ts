package ru.transservice.routemanager.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*

class GoogleLocationClient(private val mContext: Context) {

    private var googlePlayServicesAvailable: Boolean = false
    private var location: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient // location client if google play services is AVAILABLE
    //region Location
    // Настройки обновления местоположения
    private val locationRequest  = LocationRequest.create().apply {
        fastestInterval = 5000
        interval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        smallestDisplacement = 1.0f
    }

    fun getLocation(): Location?{
        return location
    }

    private val locationUpdatesCallback = object : LocationCallback() {
        override fun onLocationResult(lr: LocationResult) {
            location = lr.locations.last()
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationUpdatesCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationUpdatesCallback)
    }

    init {
        googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS
        if (googlePlayServicesAvailable) {
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(mContext)
        }
    }
}