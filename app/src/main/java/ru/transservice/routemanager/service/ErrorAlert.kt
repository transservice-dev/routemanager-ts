package ru.transservice.routemanager.service

import android.content.Context
import ru.transservice.routemanager.R

class ErrorAlert {

    companion object {
        fun showAlert(errorMessage: String, context: Context, title: String = "Ошибка") {
            val alertBuilder = android.app.AlertDialog.Builder(context).apply {
                setTitle(title)
                setMessage(errorMessage)
                setIcon(R.drawable.ic_error_24)
                setPositiveButton("Отправить") { _, _ ->
                    val sendLog = ReportLog(context)
                    sendLog.sendLogInFile()
                }
                setNegativeButton("Не отправлять") { _, _ ->
                }
            }
            val alert = alertBuilder.create()
            alert.show()
        }
    }
}