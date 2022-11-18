package ru.transservice.routemanager.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.transservice.routemanager.data.local.entities.*

@Database(
    entities = [PointItem::class, Task::class, PointFile::class, PolygonItem::class],
    views = [PointWithData::class, TaskWithData::class],
    version = 4,
    autoMigrations = [
        AutoMigration (from = 2, to = 3) ,
        AutoMigration (from = 3, to = 4)
    ],
    exportSchema = true
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

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

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