package ru.transservice.routemanager.data.local.entities

import androidx.annotation.NonNull
import androidx.room.*
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
    val search_type: SearchType = SearchType.BY_VEHICLE,
    @ColumnInfo(defaultValue = "")
    val defects: String = ""
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

    fun isEmpty(): Boolean {
        return docUid.isNullOrEmpty()
    }
}

@DatabaseView("""
    SELECT task.*, points_all.count_points as taskCountPoint, points_done.count_points as taskCountPointDone, 1 as isLoaded    
    FROM currentRoute_table as task
    LEFT JOIN (SELECT COUNT(1) as count_points, docUID FROM pointList_table WHERE NOT polygon GROUP BY docUID) as points_all
    ON task.docUid = points_all.docUID
    LEFT JOIN (SELECT COUNT(1) as count_points, docUID FROM pointList_table WHERE NOT polygon AND done GROUP BY docUID) as points_done
    ON task.docUid = points_done.docUID
    
""")
data class TaskWithData(
    @Embedded()
    val task: Task,
    val taskCountPoint: Int,
    val taskCountPointDone: Int,
    val isLoaded: Boolean
)

enum class SearchType(val id: Int){
    BY_ROUTE(1),
    BY_VEHICLE(0)
}