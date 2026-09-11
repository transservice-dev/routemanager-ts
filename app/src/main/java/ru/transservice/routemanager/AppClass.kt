package ru.transservice.routemanager

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.work.*
import cat.ereza.customactivityoncrash.config.CaocConfig
import ru.transservice.routemanager.database.AppDatabase
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import ru.transservice.routemanager.workmanager.SyncTasksUnloaded
import java.io.File

class AppClass: Application(), Configuration.Provider  {

    companion object {
        const val TAG ="RouteManager"

        lateinit var instance: AppClass
        lateinit var db: AppDatabase
        val appVersion: String get() = BuildConfig.VERSION_NAME

        fun setupWorkManager(){
            WorkManager.getInstance(instance)
                .enqueueUniquePeriodicWork(
                    UploadFilesWorker.workerPeriodicTag,
                    ExistingPeriodicWorkPolicy.KEEP,
                    UploadFilesWorker.requestPeriodicTimeWork(45L)
                )
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDatabase.getDatabase(applicationContext)

        CaocConfig.Builder.create()
            .enabled(true)
            .trackActivities(true)
            .errorActivity(ErrorActivity::class.java)
            .logErrorOnRestart(true)
            .apply()

        setupWorkManager()
        SyncTasksUnloaded.start(instance)
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .build()

}

val Context.photoDir: File
    get() {
        val mediaDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return if (mediaDir?.exists() == true) mediaDir else filesDir
    }