package ru.transservice.routemanager.service

import android.util.Log
import androidx.activity.OnBackPressedCallback

class BackPressedCallback {

    companion object {

        val callbackBlock = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
               Log.d("RouteManager", "Back button press was blocked")
            }
        }

    }


}