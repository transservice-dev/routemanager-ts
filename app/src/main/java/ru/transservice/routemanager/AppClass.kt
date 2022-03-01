package ru.transservice.routemanager

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.work.*
import cat.ereza.customactivityoncrash.config.CaocConfig
import ru.transservice.routemanager.database.AppDatabase
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import java.io.File

class AppClass: Application(), Configuration.Provider  {

    companion object {
        var appVersion: String = ""
        private var instance:AppClass? = null

        const val TAG ="RouteManager"

        fun appliactionContext(): Context {
            return instance!!.applicationContext
        }


        var db: AppDatabase? = null
        /*var gps: GPSTracker? = null
        var gpsGoogle: GoogleLocationClient? = null*/

        fun getDatabase(): AppDatabase? {
            return db
        }

        fun getOutputDirectory(): File {
            val appContext = instance!!.applicationContext
            // appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val mediaDir = instance!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            //val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            //    File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        fun setupWorkManager(){
            WorkManager.getInstance(appliactionContext())
                .enqueueUniquePeriodicWork(
                    UploadFilesWorker.workerPeriodicTag,
                    ExistingPeriodicWorkPolicy.KEEP,
                    UploadFilesWorker.requestPeriodicTimeWork(45L)
                )
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        appVersion = BuildConfig.VERSION_NAME
        db= AppDatabase.getDatabase(applicationContext)

        CaocConfig.Builder.create()
            .enabled(true)
            .trackActivities(true)
            .errorActivity(ErrorActivity::class.java)
            .logErrorOnRestart(true)
            .apply()

        //region WorkManager
        // Work manager: configure schedule and rules for periodic files upload
        setupWorkManager()
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .build()

}