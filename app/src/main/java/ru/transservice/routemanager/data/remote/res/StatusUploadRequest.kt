package ru.transservice.routemanager.data.remote.res

class StatusUploadRequest(
    val dList: ArrayList<StatusUploadBody>
)

class StatusUploadBody(
    val docUID: String,
    val docStatus: Int
)
