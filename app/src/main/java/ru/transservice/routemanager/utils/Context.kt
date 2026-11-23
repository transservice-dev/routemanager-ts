package ru.transservice.routemanager.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.TypedValue
import ru.transservice.routemanager.database.AppDatabase.Companion.dbName

fun Context.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics

    )
}

fun Context.dbVersion(dbName: String): String {
    try {
        val dbFile = this.getDatabasePath(dbName)
        if (!dbFile.exists()) return "No database file"

        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath,null,SQLiteDatabase.OPEN_READONLY)
        val version = db.version.toString()
        db.close()
        return version
    } catch (e:Exception) {
        return "Raised exception: $e"
    }
}

