package ru.transservice.routemanager.service

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.transservice.routemanager.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReportLog(val activity: AppCompatActivity) {

    private fun createLogFile () : File? {
        val storage = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "log" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale("RU"))
                .format(Date())

        try {
            return File.createTempFile(
                    fileName,
                    ".txt",
                    storage
            )
        }catch (e: Exception){
            Toast.makeText(activity, "Неудалось записать файл", Toast.LENGTH_LONG).show()
        }
        return null
    }

    private fun setLogInFile (file: File){
        val command = "logcat " + MainActivity.TAG + ":* -f " + file.absoluteFile
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

            activity.startActivity(Intent.createChooser(shareIntent,"Отправка лога"))
        }
    }

}