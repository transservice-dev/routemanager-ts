package ru.transservice.routemanager.data.remote.res.task

import ru.transservice.routemanager.data.local.entities.PointItem
import java.util.*

data class TaskRes(
    val result: TaskResultRes,
    val data: List<TaskRowRes>
)

data class TaskResultRes(
    val status: Int,
    val message: String
)

data class TaskRowRes(
    val docUID: String,
    val lineUID: String,
    val rowNumber: Int,
    val dateStart: Date,
    val dateEnd: Date,
    val driverName: String,
    val addressUID: String,
    val addressName: String,
    val addressLon: Double,
    val addressLat: Double,
    val containerUID: String,
    val containerName: String,
    val containerSize: Double,
    val agentUID: String,
    val agentName: String,
    val countPlan: Double,
    var countFact: Double,
    var countOver: Double,
    var done: Boolean,
    val tripNumber: Int,
    val polygon: Boolean,
    var timestamp: Date,
    val routeName: String,
    var reasonComment: String,
    val comment: String,
    val polygonUID: String?,
    val polygonName: String?,
    val polygonByRow: Boolean?
){
    fun toPointDestination(): PointItem{
        return PointItem(docUID.trim(),
        lineUID.trim(),
        rowNumber,
        addressName.trim(),
        addressLon,
        addressLat,
        containerName.trim(),
        containerSize,
        agentName.trim(),
        countPlan,
        countFact,
        countOver,
        done,
        tripNumber,
        polygon,
        routeName.trim(),
        comment.trim(),
            polygonUID = polygonUID ?: "",
            tripNumberFact = 1000,
            polygonName = polygonName?.trim() ?: "",
            polygonByRow = polygonByRow ?: false
        )
    }
}

