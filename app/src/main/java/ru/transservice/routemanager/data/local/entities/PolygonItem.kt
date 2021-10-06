package ru.transservice.routemanager.data.local.entities

import androidx.room.Entity
import org.jetbrains.annotations.NotNull
import java.io.Serializable

@Entity(tableName = "polygon_table",primaryKeys = ["pk"])
data class PolygonItem(
    @NotNull
    val pk: Int,
    @NotNull
    val docUID: String,
    val uid: String,
    val name: String,
    val tripNumber: Int,
    val by_default: Boolean,
    val done: Boolean
): Serializable