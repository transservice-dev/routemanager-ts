package ru.transservice.routemanager

import android.app.Application
import android.content.Context
import ru.transservice.routemanager.database.AppDatabase

class AppClass: Application() {

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

    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        appVersion = BuildConfig.VERSION_NAME
        db= AppDatabase.getDatabase(applicationContext)
    }
}