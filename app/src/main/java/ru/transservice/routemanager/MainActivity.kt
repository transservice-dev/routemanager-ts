package ru.transservice.routemanager

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.transservice.routemanager.animation.AnimateView
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.databinding.ActivityMainBinding
import ru.transservice.routemanager.repositories.RootRepository
import java.util.*


const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

class MainActivity : AppCompatActivity() {

    lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    lateinit var swipeLayout: SwipeRefreshLayout
    lateinit var navMenu: BottomNavigationView
    var backPressedBlock = false
    private var doubleBackClick = false
    private lateinit var channel: NotificationChannel

    private val mPrefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == "URL_NAME" || key == "URL_PORT" || key == "URL_AUTHPASS" ) {
                    RootRepository.setPreferences()
                }

                if (key == "SEARCH_BY_ROUTE") {
                    RootRepository.updateCurrentTask()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navMenu = binding.bottomMenu
        binding.swipe.isEnabled = false
        val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        swipeLayout = binding.swipe
        createNotificationChannel()
        initNavMenuButtons()
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
        RootRepository.setPreferences()

        /*onBackPressedDispatcher.addCallback(this){
            if(backPressedBlock){
            }
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }

            if (doubleBackClick) {
                super.onBackPressed()
            }

            doubleBackClick = true
            Toast.makeText( applicationContext, "Два раза нажмите для выхода", Toast.LENGTH_SHORT).show()

            Handler().postDelayed({ doubleBackClick = false }, 2000)
        }*/

    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPrefsListener)
    }

    override fun onStop() {
        super.onStop()
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

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = NotificationChannel(
                "UPLOAD_ROUTE_DATA",
                "upload route data",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {  description = "Выгрузка данных и фотографий на сервер" }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationChannel() = channel

    fun getDisplayWidth(): Int{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    /*@SuppressLint("RestrictedApi")
    override fun onBackPressed() {
        if(backPressedBlock){
            return
        }
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
            return
        }

        if (doubleBackClick) {
            super.onBackPressed()
            return
        }

        doubleBackClick = true
        Toast.makeText(this, "Два раза нажмите для выхода", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackClick = false }, 2000)
    }*/

    @SuppressLint("RestrictedApi")
    fun initNavMenuButtons(){
        with(binding){
            bottomMenu.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.home -> {
                        navController.navigate(R.id.startScreenFragment)
                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.list -> {
                        navController.navigate(R.id.taskListFragment)
                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.photos -> {
                        val bundle = bundleOf(
                            "point" to null,
                            "photoOrder" to PhotoOrder.DONT_SET
                        )
                        navController.navigate(R.id.photoListFragment, bundle)
                        return@OnNavigationItemSelectedListener false
                    }
                    R.id.settings -> {
                        try {
                            navController.navigate(R.id.settingsFragment)
                        } catch (e: Exception) {
                            Log.d(TAG, "nav error: $e")
                            return@OnNavigationItemSelectedListener false
                        }

                        return@OnNavigationItemSelectedListener true
                    }
                }
                false
            })
        }

        navController.addOnDestinationChangedListener { _, destanation, _ ->
            //log
            Log.i(TAG, "nav destination " + destanation.displayName)

            findViewById<View>(R.id.bottom_menu).visibility = View.VISIBLE

            navController.backStack.removeIf {
                it.destination.id == destanation.id
                        && navController.backStack.last != it && navController.backStack.first != it
            }

            when (destanation.id) {
                /*R.id.route_list -> {
                    bottomMenu.menu.findItem(R.id.list).isChecked = true
                    val animateView = AnimateView(guideLine, this, true)
                    animateView.showHeight()
                    return@addOnDestinationChangedListener
                }*/
                R.id.startScreenFragment -> {
                    binding.bottomMenu.menu.findItem(R.id.home).isChecked = true
                    val animateView = AnimateView(binding.guidelineMain, this, true)
                    animateView.showHeight()
                    return@addOnDestinationChangedListener

                }
                R.id.settingsFragment -> {
                    binding.bottomMenu.menu.findItem(R.id.settings).isChecked = true
                    val animateView = AnimateView(binding.guidelineMain, this, true)
                    animateView.showHeight()
                    return@addOnDestinationChangedListener

                }
                R.id.photoListFragment -> {
                    binding.bottomMenu.menu.findItem(R.id.photos).isChecked = true
                    val animateView = AnimateView(binding.guidelineMain, this, true)
                    animateView.showHeight()
                    return@addOnDestinationChangedListener

                }
                else -> {
                    val animateView = AnimateView(binding.guidelineMain, this, true)
                    animateView.hideHeight()
                    //bottomMenu.visibility = View.GONE
                }
            }

        }

    }

    companion object {
        const val TAG = "${AppClass.TAG}: MainActivity"
    }

}