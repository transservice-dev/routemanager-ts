package ru.transservice.routemanager.workmanager

import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.transservice.routemanager.repositories.RootRepository

class UploadFilesWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    private val repository = RootRepository

    @RequiresApi(Build.VERSION_CODES.O)

    override suspend fun doWork(): Result {
        // Do the work here--in this case, upload the images.
        repository.uploadFilesOnSchedule()
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}