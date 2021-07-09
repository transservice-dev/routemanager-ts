package ru.transservice.routemanager.data.remote.res.task

class TaskRequestBody(
    val dateTask: String,
    val deviceId:String,
    val vehicleId: String,
    val routeId: String,
    val search_type: Int
)