package ru.transservice.routemanager.data.local.entities

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.extensions.shortFormat
import java.util.*

@Entity(tableName = "currentRoute_table")
data class Task(
    @PrimaryKey
    @NonNull
    val docUid: String,
    @Embedded(prefix = "vehicle_")
    val vehicle:VehicleItem? = null,
    @Embedded(prefix = "route_")
    val route:RouteItem? = null,
    val routeDate: Date = Date(),
    val search_type: SearchType = SearchType.BY_VEHICLE
){
    var countPoint: Int = 0
    var countPointDone: Int = 0
    var dateStart: Date? = null
    var dateEnd: Date? = null
    val deviceId get() = "${vehicle?.number ?: ""}__${docUid}__${dateStart?.shortFormat()}"
    @ColumnInfo(defaultValue = "0")
    var lastTripNumber: Int = 0
    @ColumnInfo(defaultValue = "0")
    var polygonByRow = false
}

enum class SearchType(val id: Int){
    BY_ROUTE(1),
    BY_VEHICLE(0)
}