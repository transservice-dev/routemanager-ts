package ru.transservice.routemanager

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.google.android.material.button.MaterialButton
import ru.transservice.routemanager.databinding.ActivityErrorBinding
import ru.transservice.routemanager.service.ReportLog

private lateinit var binding: ActivityErrorBinding

class ErrorActivity : AppCompatActivity() {

    var mSavedInstanceState : Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mSavedInstanceState = savedInstanceState
        val config = CustomActivityOnCrash.getConfigFromIntent(intent)

        if(config == null){
            finish()
            return
        }
        val error = CustomActivityOnCrash.getStackTraceFromIntent(intent)
        if(error != null){
            Log.d(AppClass.TAG,error)
        }

        with(binding.btnRestartApp) {
            if (config.isShowRestartButton && config.restartActivityClass != null) {
                text = "Перезагрузить"
                setOnClickListener {
                    CustomActivityOnCrash.restartApplication(
                        this@ErrorActivity,
                        config
                    )
                }
            } else {
                text = "Закрыть приложение"
                setOnClickListener {
                    CustomActivityOnCrash.closeApplication(
                        this@ErrorActivity,
                        config
                    )
                }
            }
        }

        binding.btnSendLogFile.setOnClickListener {
            val sendLog = ReportLog(this)
            sendLog.sendLogInFile()
        }
    }

}