package ru.transservice.routemanager

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.work.*
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.material.tabs.TabLayout
import ru.transservice.routemanager.database.AppDatabase
import ru.transservice.routemanager.workmanager.UploadFilesWorker
import java.io.File
import java.util.concurrent.TimeUnit

class AppClass: Application(), Configuration.Provider {

    companion object {
        var appVersion: String = ""
        private var instance:AppClass? = null

        fun appliactionContext(): Context {
            return instance!!.applicationContext
        }


        var db: AppDatabase? = null
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

        fun getDisplayWidth(): Int {
            //return (appliactionContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds.width()
            return appliactionContext().display?.width?.toInt() ?: 0
            //return (appliactionContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.width?.toInt() ?: 0
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
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val uploadWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<UploadFilesWorker>(45, TimeUnit.MINUTES)
                .addTag("uploadFiles")
                .setConstraints(constraints)
                .build()
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueueUniquePeriodicWork("uploadFiles",
            ExistingPeriodicWorkPolicy.KEEP,uploadWorkRequest)

    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.INFO)
        .build()


}