package ru.transservice.routemanager.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.utils.dbVersion

@Database(
    entities = [PointItem::class, Task::class, PointFile::class, PolygonItem::class],
    views = [PointWithData::class, TaskWithData::class],
    version = 7,
    autoMigrations = [
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 6, to = 7) //Удалили повторные индексы
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dbDao(): DaoInterface

    companion object {
        const val TAG = "Db"
        const val dbName = "routemanager.db"
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG,"Migration 1 >> 2")
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

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d(TAG,"Migration 4 >> 5")
                db.execSQL("ALTER TABLE pointList_table ADD COLUMN uploaded INTEGER")
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d(TAG,"Migration 5 >> 6")
                db.execSQL("ALTER TABLE pointList_table ADD COLUMN rlat REAL")
                db.execSQL("ALTER TABLE pointList_table ADD COLUMN rlon REAL")
            }
        }

        fun getDatabase(ctx: Context): AppDatabase {
            Log.d(TAG,"Start: [${dbName}] v.${ctx.dbVersion(dbName)}")
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    dbName)
                    .addMigrations(MIGRATION_1_2,MIGRATION_4_5,MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}