package ru.transservice.routemanager.workmanager

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import java.util.concurrent.TimeUnit

class UploadFilesWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    private val repository = RootRepository

    companion object {
        const val workerTag = "uploadFiles"
        const val workerPeriodicTag = "uploadFilesPeriodic"
        const val fileId = "fileId"

        fun requestOneTimeWork(inputData: Data? = null): WorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val builder = OneTimeWorkRequestBuilder<UploadFilesWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(workerTag)

            inputData?.let {
                builder.setInputData(it)
            }

            return builder.build()
        }

        fun requestPeriodicTimeWork(minutes_interval: Long,inputData: Data? = null): PeriodicWorkRequest {
            // Work manager: configure schedule and rules for periodic files upload
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val builder = PeriodicWorkRequestBuilder<UploadFilesWorker>(minutes_interval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(workerPeriodicTag)

            inputData?.let {
                builder.setInputData(it)
            }

            return builder.build()

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val result = withContext(Dispatchers.IO){
            val fileId = inputData.getLong(fileId, -1)
            val pointFile = if (fileId != -1L) repository.getPointFileById(fileId) else null
            repository.uploadFiles(this@UploadFilesWorker, false, pointFile)
        }
        return when (result) {
            is LoadResult.Success -> {
                Result.success()
            }
            else -> {
                if (runAttemptCount > 3) {
                    Result.failure()
                }else{
                    Result.retry()
                }

            }
        }
    }


}