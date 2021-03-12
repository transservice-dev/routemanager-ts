package ru.transservice.routemanager.database

import androidx.room.TypeConverter
import java.util.*
import kotlin.collections.ArrayList

class Converters {

    companion object {
        @TypeConverter
        @JvmStatic
        fun fromDate(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        @JvmStatic
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time?.toLong()
        }
    }
}