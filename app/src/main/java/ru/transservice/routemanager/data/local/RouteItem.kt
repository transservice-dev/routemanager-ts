package ru.transservice.routemanager.data.local

import java.io.Serializable

data class RouteItem(
    val name: String,
    val uid: String
): Serializable
