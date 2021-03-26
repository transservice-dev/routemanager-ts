package ru.transservice.routemanager.data.remote.res.task

import android.text.BoringLayout
import java.util.*
import kotlin.collections.ArrayList

class TaskUploadRequest(
    val trackList: List<TaskUploadBody>
)

class TaskUploadBody (
    val docUID: String,
    val lineUID: String,
    val countFact: Double,
    val countOver: Double,
    val done: Boolean,
    val reasonComment: String,
    val timestamp: String,
)