package ru.transservice.routemanager.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import java.util.*

@Entity(tableName = "pointList_table",primaryKeys = ["docUID","lineUID"], indices =arrayOf(Index("docUID","lineUID","docUID","lineUID")))
data class PointDestination(
    val docUID: String,
    val lineUID: String,
    val rowNumber: Int,
    val addressName: String,
    val addressLon: Double,
    val addressLat: Double,
    val containerName: String,
    val containerSize: Double,
    val agentName: String,
    val countPlan: Double,
    var countFact: Double,
    var countOver: Double,
    var done: Boolean,
    val tripNumber: Int,
    val polygon: Boolean,
    val routeName: String,
    val comment: String
){
    var timestamp: Date? = null
    var reasonComment: String = ""
}
