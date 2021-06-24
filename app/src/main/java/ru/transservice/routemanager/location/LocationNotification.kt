package ru.transservice.routemanager.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R

class LocationNotification(context: MainActivity) {

    private val pendingIntent: PendingIntent =
        Intent(context, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(context, 0, notificationIntent, 0)
        }

    /*@RequiresApi(Build.VERSION_CODES.O)
    private val notification: Notification = Notification.Builder(context,context.navNotificationChannel.id)
        .setContentTitle(context.getText(R.string.notification_title))
        .setContentText(context.getText(R.string.notification_message))
        .setSmallIcon(R.drawable.ic_navigation_24)
        .setContentIntent(pendingIntent)
        .setTicker(context.getText(R.string.ticker_text))
        .build()

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotification(): Notification{
        return notification
    }*/
}
