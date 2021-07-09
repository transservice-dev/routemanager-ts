package ru.transservice.routemanager.database

import androidx.room.TypeConverter
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointStatuses
import ru.transservice.routemanager.data.local.entities.SearchType
import java.util.*

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
            return date?.time
        }

        @TypeConverter
        @JvmStatic
        fun fromSearchType(value: SearchType): Int {
            return value.id
        }

        @TypeConverter
        @JvmStatic
        fun IntToSearchType(value: Int): SearchType {
            return when (value){
                0 -> SearchType.BY_VEHICLE
                else -> SearchType.BY_ROUTE
            }
        }

        @TypeConverter
        @JvmStatic
        fun pointStatusToString(pointStatuses: PointStatuses): String {
            return pointStatuses.toString()
        }

        @TypeConverter
        @JvmStatic
        fun stringToPointStatus(value: String): PointStatuses {
            return PointStatuses.NOT_VISITED
        }


        @TypeConverter
        @JvmStatic
        fun fromPhotoOrder(photoOrder: PhotoOrder): Int {
            return when (photoOrder) {
                PhotoOrder.PHOTO_BEFORE -> 0
                PhotoOrder.PHOTO_AFTER -> 1
                PhotoOrder.PHOTO_CANTDONE ->2
                else -> -1
            }
        }

        @TypeConverter
        @JvmStatic
        fun toPhotoOrder(data: Int): PhotoOrder {
            return when (data) {
                0 -> PhotoOrder.PHOTO_BEFORE
                1 -> PhotoOrder.PHOTO_AFTER
                2 -> PhotoOrder.PHOTO_CANTDONE
                else -> PhotoOrder.DONT_SET
            }
        }
    }
}