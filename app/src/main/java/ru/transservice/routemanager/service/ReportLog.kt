package ru.transservice.routemanager.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import ru.transservice.routemanager.AppClass
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportLog(val context: Context) {

    private fun createLogFile () : File? {
        val storage = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "log" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale("RU"))
                .format(Date())

        try {
            return File.createTempFile(
                    fileName,
                    ".txt",
                    storage
            )
        }catch (e: Exception){
            Toast.makeText(context, "Неудалось записать файл", Toast.LENGTH_LONG).show()
        }
        return null
    }

    private fun setLogInFile (file: File){
        val command = "logcat " + AppClass.TAG + ":* -f " + file.absoluteFile
        Runtime.getRuntime().exec(command)
    }

    fun sendLogInFile (){
        val file = createLogFile()
        if(file != null){
            setLogInFile(file)
            val imageUris : ArrayList<Uri> = arrayListOf()
            imageUris.add(Uri.parse(file.absolutePath))

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM,imageUris)
                type = "*/*"
            }

            context.startActivity(Intent.createChooser(shareIntent,"Отправка лога"))
        }
    }

}