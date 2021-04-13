package ru.transservice.routemanager.data.remote.res

import ru.transservice.routemanager.data.local.VehicleItem

data class VehicleRes(
    val name: String,
    val uid: String,
    val PK: String,
    val number: String,
    val garageNum: String,
    val regionUID: String
){
    fun toVehicleItem(): VehicleItem{
        return VehicleItem(name.trim(),uid, number.trim())
    }
}
