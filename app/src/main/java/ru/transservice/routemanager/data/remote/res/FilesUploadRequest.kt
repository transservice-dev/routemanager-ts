package ru.transservice.routemanager.data.remote.res

class FilesUploadRequest(
    val files: ArrayList<FilesRequestBody>
)

class FilesRequestBody(
    val docUID: String,
    val lineUID:String,
    val lat: Double,
    val lon: Double,
    val fileName: String,
    val fileExtension:String,
    val timestamp: String,
    val photoOrder: Int,
    val fileBase64: String
)