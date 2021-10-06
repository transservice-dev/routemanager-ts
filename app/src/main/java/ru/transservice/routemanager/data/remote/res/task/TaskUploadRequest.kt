package ru.transservice.routemanager.data.remote.res.task

class TaskUploadRequest(
    val trackList: List<TaskUploadBody>
)

data class TaskUploadBody (
    val docUID: String,
    val lineUID: String,
    val countFact: Double,
    val countOver: Double,
    val done: Boolean,
    val reasonComment: String,
    val timestamp: String,
    val polygonUID: String,
    val polygonName: String,
    val polygon: Boolean,
    val tripNumberFact: Int
)