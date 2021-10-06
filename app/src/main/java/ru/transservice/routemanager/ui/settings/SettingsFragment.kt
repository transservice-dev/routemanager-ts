package ru.transservice.routemanager.ui.settings

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.FileProvider
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.BuildConfig
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.repositories.RootRepository
import ru.transservice.routemanager.service.ReportLog
import java.io.File

class SettingsFragment : PreferenceFragmentCompat() {

    private val repository = RootRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val portPreference: EditTextPreference? = findPreference("URL_PORT")

        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val urlPreference: EditTextPreference? = findPreference("URL_NAME")

        urlPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        val passwordPreference: EditTextPreference? = findPreference("URL_AUTHPASS")

        passwordPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val datePreference: EditTextPreference? = findPreference("DATE")
        datePreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_DATETIME_VARIATION_DATE
        }

        val sendLog = findPreference<Preference>(getString(R.string.sendLog))

        sendLog?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val sendLog = ReportLog(requireContext())
            sendLog.sendLogInFile()
            return@OnPreferenceClickListener true
        }

        val update = findPreference<Preference>(getString(R.string.update))

        update?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            updateAppFromApk(requireActivity() as MainActivity)
            return@OnPreferenceClickListener true
        }

        val clearCache = findPreference<Preference>(getString(R.string.clearCache))

        clearCache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            prepareClearCache(requireActivity() as MainActivity)

            return@OnPreferenceClickListener true
        }

        val restartWork = findPreference<Preference>("RESTART_WORK")

        restartWork?.onPreferenceClickListener = Preference.OnPreferenceClickListener {

            doRestartWork(requireActivity() as MainActivity)

            return@OnPreferenceClickListener true
        }

    }

    private fun doRestartWork(mainActivity: MainActivity) {
        val workManager = WorkManager.getInstance(AppClass.appliactionContext())
        workManager.pruneWork()
        workManager.cancelUniqueWork("uploadFiles")
        AppClass.setupWorkManager()
    }

    private fun clearDir (dir : File, notDeleteFiles : List<String>){
        if(dir.isDirectory){
            val children = dir.list()
            if(children != null){
                for(itemFile in children) {
                    val path = dir.absolutePath + "/" + itemFile
                    if (notDeleteFiles.contains(path)) {
                        continue
                    }
                    val file = File(path)
                    if (file.isDirectory) {
                        clearDir(file, notDeleteFiles)
                    } else {
                        file.delete()
                        if(file.exists()){
                            file.canonicalFile.delete()
                            if (file.exists()){
                                context?.deleteFile(file.path)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun prepareClearCache(activity: MainActivity){
        activity.swipeLayout.isRefreshing = true
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                , WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        activity.backPressedBlock = true
        val storage = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storage?.let {
            repository.getAllFiles { filesList ->
                clearDir(storage, filesList.map { it.filePath }.filter { it.isNotEmpty() })
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                activity.swipeLayout.isRefreshing = false
                activity.backPressedBlock = false
            }
        }
        val storageD = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        storageD?.let {
            clearDir(storageD, emptyList())
        }

    }

    private fun updateAppFromApk(activity: MainActivity) {
        activity.swipeLayout.isRefreshing = true
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                , WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        activity.backPressedBlock = true
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "apk_release.apk"
        val file = File(dir,fileName)
        repository.loadApkFile(file){
            openApkFile(file)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            activity.swipeLayout.isRefreshing = false
            activity.backPressedBlock = false
        }

    }

    private fun openApkFile(file: File){
        val intent = Intent(Intent.ACTION_VIEW)
        val fileApk = file
        if (fileApk.length() == 0L) {
            return
        }
        val uri = FileProvider.getUriForFile(
                requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider",
                fileApk
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.setBackgroundColor(requireContext().getColor(R.color.backgroundWhite))
        return view
    }

}