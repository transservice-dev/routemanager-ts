package ru.transservice.routemanager.service

import android.util.Log
import androidx.activity.OnBackPressedCallback
import ru.transservice.routemanager.AppClass

class BackPressedCallback {

    companion object {

        val callbackBlock = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
               Log.d(AppClass.TAG, "Back button press was blocked")
            }
        }

    }


}