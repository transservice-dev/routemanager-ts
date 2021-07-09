package ru.transservice.routemanager.data.local

import java.io.Serializable

data class VehicleItem(
    val name: String,
    val uid: String,
    val number: String
): Serializable
