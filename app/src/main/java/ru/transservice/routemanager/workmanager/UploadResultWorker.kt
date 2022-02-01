package ru.transservice.routemanager.workmanager

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.transservice.routemanager.R
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.LoadResult
import android.os.*
import ru.transservice.routemanager.extensions.WorkInfoKeys
import ru.transservice.routemanager.service.errorDescription
import java.util.*
import java.util.concurrent.TimeUnit

class UploadResultWorker(context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {

    private val repository = RootRepository
    val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE)

    companion object {

        const val NOTIFICATION_ID = "UPLOAD_RESULT_DATA"
        const val workerTag = "UploadResult"

        fun requestOneTimeWorkExpedited(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<UploadResultWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(workerTag)
                .build()
        }
    }



    override suspend fun getForegroundInfo(): ForegroundInfo {
       return createForegroundInfo("Начало выгрузки данных")
    }

    private fun createForegroundInfo(descripton: String): ForegroundInfo {
        val id = NOTIFICATION_ID
        val title = applicationContext.getString(R.string.uploading_result)
        // Create a Notification channel if necessary
        createChannel()
        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(descripton)
            .setSmallIcon(R.drawable.ic_logo_mini)
            .setOngoing(true)
            .build()

        return ForegroundInfo(10,notification)
    }

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result  {
        val result = withContext(Dispatchers.IO){
            val startUpload = workDataOf(
                WorkInfoKeys.Progress to 0,
                WorkInfoKeys.Stage to "Начало выгрузки",
                WorkInfoKeys.Description to "Идет выгрузка данных на сервер",
                WorkInfoKeys.CountAttempts to runAttemptCount)
            setForeground(createForegroundInfo(startUpload.getString(WorkInfoKeys.Description) ?: ""))
            setProgress(startUpload)
            repository.uploadResult(this@UploadResultWorker)
        }
        return when (result) {
            is LoadResult.Success -> {
                val finishUpload = workDataOf(
                    WorkInfoKeys.Progress to 100,
                    WorkInfoKeys.Stage to "Окончание выгрузки",
                    WorkInfoKeys.Description to "Выгрузка успешно завершена" )
                setProgress(finishUpload)
                setForeground(createForegroundInfo(finishUpload.getString(WorkInfoKeys.Description) ?: ""))
                Result.success()
            }
            else -> {
                if (runAttemptCount > 2) {
                    val finishUpload = workDataOf(
                        WorkInfoKeys.Progress to 0,
                        WorkInfoKeys.Stage to "Окончание выгрузки",
                        WorkInfoKeys.Description to "Ошибка при выгрузке данных")
                    setProgress(finishUpload)
                    setForeground(createForegroundInfo(finishUpload.getString(WorkInfoKeys.Description) ?: ""))
                    Result.failure(workDataOf(WorkInfoKeys.Error to result.errorDescription() ))
                }else{
                    val finishUpload = workDataOf(
                        WorkInfoKeys.Progress to 0,
                        WorkInfoKeys.Stage to "Окончание выгрузки",
                        WorkInfoKeys.Description to "Выгрузка данных. Попытка №${runAttemptCount+1}")
                    setProgress(finishUpload)
                    setForeground(createForegroundInfo(finishUpload.getString(WorkInfoKeys.Description) ?: ""))
                    Result.retry()
                }

            }
        }
    }

    private fun createChannel(){
        val channel = NotificationChannel(
            NOTIFICATION_ID,
            "upload result data",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Выгрузка данных и фотографий на сервер" }

        // Register the channel with the system
        val notificationManager: NotificationManager =
            notificationService as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

