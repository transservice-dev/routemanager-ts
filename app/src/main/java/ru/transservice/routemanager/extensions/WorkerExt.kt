package ru.transservice.routemanager.extensions

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.workDataOf

suspend fun CoroutineWorker.updateProgressValue(key: String, value: Any?) {
    try {
        val workData = WorkManager.getInstance(applicationContext).getWorkInfoById(id).get()
        workData?.let { info ->
            val newData = mutableListOf<Pair<String, Any?>>()
            info.progress.keyValueMap.forEach { (k, v) ->
                newData.add(Pair(k,if (k==key) value else v))
            }
            setProgress(workDataOf(*newData.toTypedArray()))
        }
    }catch (e: Exception) {
        Log.d(tag(), "Error while setting progress, $e")
    }
}

class WorkInfoKeys() {
    companion object {
        const val Progress = "Progress"
        const val CountAttempts = "CountAttempts"
        const val Stage = "Stage"
        const val Description = "Description"
        const val Error = "Error"
    }
}