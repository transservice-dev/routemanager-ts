package ru.transservice.routemanager.data.remote.res

class StatusUploadRequest(
    val dList: ArrayList<StatusUploadBody>
)

class StatusUploadBody(
    val docUID: String,
    val docStatus: Int,
    val deviceID: String,
    val vehicleUID: String,
    val routeUID: String,
    val dateStart: String,
    val dateEnd: String,
    val app_version: String,
    val vehicle_defects: String = ""
)
