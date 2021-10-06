package ru.transservice.routemanager.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PolygonItem
import ru.transservice.routemanager.data.local.entities.Task

@Database(
    entities = [PointItem::class, Task::class, PointFile::class, PolygonItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dbDao(): DaoInterface

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS 'polygon_table' (
                                'pk' INTEGER NOT NULL,
                                'docUID' TEXT NOT NULL,
                                'uid' TEXT NOT NULL,
                                'name' TEXT NOT NULL,
                                'tripNumber' INTEGER NOT NULL,
                                'by_default' INTEGER NOT NULL,
                                'done' INTEGER NOT NULL,
                                PRIMARY KEY('pk'))""".trimIndent())
                database.execSQL("ALTER TABLE currentRoute_table ADD COLUMN lastTripNumber INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE currentRoute_table ADD COLUMN polygonByRow INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pointList_table ADD COLUMN polygonUID TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE pointList_table ADD COLUMN tripNumberFact INTEGER NOT NULL DEFAULT 1000")
                database.execSQL("ALTER TABLE pointList_table ADD COLUMN polygonName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE pointList_table ADD COLUMN polygonByRow INTEGER NOT NULL DEFAULT 0")
            }
        }


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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}