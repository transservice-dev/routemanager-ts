package ru.transservice.routemanager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.window.layout.WindowMetricsCalculator
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.transservice.routemanager.databinding.ActivityMainBinding
import ru.transservice.routemanager.location.NavigationService
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.network.RetrofitClient
import ru.transservice.routemanager.repositories.PreferencesRepository
import ru.transservice.routemanager.repositories.RootRepository

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var navMenu: BottomNavigationView
    var backPressedBlock = false
    var locationServiceIntent: Intent? = null

    private val mPrefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "URL_NAME" || key == "URL_PORT" || key == "URL_AUTHPASS" ) {
                    RootRepository.setPreferences()
                    RetrofitClient.updateConnectionSettings()
                }

                if (key == "SEARCH_BY_ROUTE") {
                    PreferencesRepository.updatePrefTask()
                }

                if (key == "USE_GOOGLE_NAV") {
                    val isActive  = NavigationServiceConnection.isActive()
                    if (isActive){
                        NavigationServiceConnection.stopTracking()
                    }
                    NavigationServiceConnection.setNavClient()
                    if (isActive){
                        NavigationServiceConnection.startTracking()
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            //setKeepOnScreenCondition()
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navMenu = binding.bottomMenu
        binding.swipe.isEnabled = false
        swipeLayout = binding.swipe
        setupNavigation()
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
        RootRepository.setPreferences()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(setOf(R.id.permissionFragment, R.id.startScreenFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
        swipeLayout = binding.swipe
        binding.bottomMenu.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.startScreenFragment -> navMenu.isVisible = true
                R.id.settingsFragment -> navMenu.isVisible = true
                R.id.navGallery -> navMenu.isVisible = true
                else -> navMenu.isGone = true
            }

            when (destination.id) {
                R.id.cameraFragment -> supportActionBar?.hide()
                R.id.photoFragment -> supportActionBar?.hide()
                R.id.galleryFragment -> supportActionBar?.hide()
                R.id.splashScreenFragment -> supportActionBar?.hide()
                else -> supportActionBar?.show()
            }
        }
    }

    override fun onDestroy() {
        WorkManager.getInstance(applicationContext).pruneWork()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        locationServiceIntent = Intent(this,NavigationService::class.java).also {
                intent -> bindService(intent,NavigationServiceConnection, Context.BIND_AUTO_CREATE) }

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPrefsListener)
    }

    override fun onStop() {
        super.onStop()
        unbindService(NavigationServiceConnection)
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPrefsListener)
    }

    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    fun getDisplayWidth(): Int{
        return WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds.width()
    }

    companion object {
        const val TAG = "${AppClass.TAG}: MainActivity"
    }

}