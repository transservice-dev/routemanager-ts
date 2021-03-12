package ru.transservice.routemanager.data.remote.res

import ru.transservice.routemanager.data.local.RegionItem

data class RegionRes(
    val name: String,
    val uid: String,
    val PK: String
){
    fun toRegionItem(): RegionItem{
        return RegionItem(name.trim(),uid)
    }
}