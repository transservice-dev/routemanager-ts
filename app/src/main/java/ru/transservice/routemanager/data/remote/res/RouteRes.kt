package ru.transservice.routemanager.data.remote.res

import ru.transservice.routemanager.data.local.RouteItem

data class RouteRes(
    val name: String, val uid: String
){
    fun toRouteItem(): RouteItem {
        return RouteItem(name.trim(),uid)
    }
}
