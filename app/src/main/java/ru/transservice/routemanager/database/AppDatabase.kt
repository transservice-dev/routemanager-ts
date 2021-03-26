package ru.transservice.routemanager.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.Task

@Database(
    entities = [PointItem::class, Task::class, PointFile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dbDao(): DaoInterface

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "routemanager.db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}