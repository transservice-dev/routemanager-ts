package ru.transservice.routemanager.data.remote.res

import org.jetbrains.annotations.NotNull
import ru.transservice.routemanager.data.local.entities.PolygonItem

data class PolygonRes(
    @NotNull
    val pk: Int,
    @NotNull
    val docUID: String,
    val uid: String,
    val name: String,
    val trip_number: Int,
    val default: Boolean
){
    fun toPolygonItem(): PolygonItem{
        return PolygonItem(
            pk = pk,
            docUID = docUID,
            uid = uid,
            name = name,
            tripNumber = trip_number,
            by_default = default,
            false
        )
    }
}
