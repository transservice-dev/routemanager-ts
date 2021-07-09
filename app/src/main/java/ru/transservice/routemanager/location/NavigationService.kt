package ru.transservice.routemanager.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.repositories.PreferencesRepository
import java.util.*
import ru.transservice.routemanager.R

class NavigationService : Service() {

    private val googlePlayServicesAvailable: Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            AppClass.appliactionContext()
        ) == ConnectionResult.SUCCESS

    private lateinit var channel: NotificationChannel
    private var location: Location? = null
    var locationGPS: Location? = null
    var locationNetwork: Location? = null
    private var locationAvailable: Boolean = false
    private lateinit var trackerClient: BaseTracker
    var isActive = false

    private val binder = ServiceBinder()

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "${AppClass.TAG}: NavigationService"
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startTracking() {
        Log.d(TAG, "Start tracking location")
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::lock").apply {
                    acquire(10*60*1000L /*10 minutes*/)
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1,createNotification())
        }
       // Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        CoroutineScope(Dispatchers.Default).launch {
            trackerClient.startTrackingLocation()
        }
        isActive = true
    }

    fun stopTracking() {
        Log.d(TAG, "Stop tracking location")
        trackerClient.stopTrackingLocation()
        location = null
        locationGPS = null
        locationNetwork = null
        isActive = false
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            }
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG,"Service stopped without being started: ${e.message}")
        }

    }

    fun setNavClient() {
        trackerClient =
            if (googlePlayServicesAvailable && PreferencesRepository.getUseNavGoogle()) {
                GoogleLocationTracker(AppClass.appliactionContext())
            } else {
                LocationManagerTracker(AppClass.appliactionContext())
            }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startTracking()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    override fun onCreate() {
        Log.d(TAG, "OnCreate")
        setNavClient()
        super.onCreate()
    }

    fun getLocation(): Location? {
        return if (locationGPS == null) {
            if (locationNetwork == null) {
                location
            } else {
                locationNetwork
            }
        } else {
            locationGPS
        }
    }

    fun getlocationAvailable(): Boolean {
        return locationAvailable
    }

    inner class ServiceBinder : Binder() {
        fun getService(): NavigationService = this@NavigationService
    }

    private abstract class BaseTracker : ILocationClient

    @SuppressLint("MissingPermission")
    private inner class LocationManagerTracker(context: Context) : BaseTracker(),
        ILocationClient {
        // flag for GPS status
        var isGPSEnabled = false

        // flag for network status
        var isNetworkEnabled = false

        private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 1 // 1 meter

        // The minimum time between updates in milliseconds
        private val MIN_TIME_BW_UPDATES = (1000 * 1  // 1 second
                ).toLong()

        val locationListenerGPS: LocationListener = LocationListener { newLocation ->
            locationGPS = newLocation
            location = newLocation
            Log.d(TAG, "Location changed: $newLocation ${Date()}")
        }

        val locationListenerNetwork: LocationListener =
            LocationListener { newLocation ->
                locationNetwork = newLocation
                location = newLocation
                Log.d(TAG, "Location changed: $newLocation ${Date()}")
            }

        // Declaring a Location Manager
        var locationManager: LocationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

        override fun startTrackingLocation() {
            //locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            // getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                locationAvailable = false
            } else {
                locationAvailable = true
                // First if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerGPS, Looper.getMainLooper()
                    )
                    Log.d(TAG, "Start tracking GPS location")
                }
                // Then get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), locationListenerNetwork, Looper.getMainLooper()
                    )
                    Log.d(TAG, "Start tracking Network location")
                }
            }
        }

        override fun stopTrackingLocation() {
            locationManager.removeUpdates(locationListenerGPS)
            locationManager.removeUpdates(locationListenerNetwork)
            Log.d(TAG, "Stop using GPS")
        }

    }

    @SuppressLint("MissingPermission")
    private inner class GoogleLocationTracker(context: Context) : BaseTracker(),
        ILocationClient {

        private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        private val locationRequest = LocationRequest.create().apply {
            interval = 1000
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        channel = NotificationChannel(
            "UPLOAD_ROUTE_DATA",
            "upload route data",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Выгрузка данных и фотографий на сервер" }

        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(this, channel.id)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_navigation_24)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .build()
    }

}

