package ru.transservice.routemanager

import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

abstract class BaseFragment() : Fragment(), LifecycleObserver {

    protected val root
        get() = requireActivity() as MainActivity

    private val callbackExit = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleExit()
        }
    }

    open fun handleExit() {
        // overwrite if needed
    }


    override fun onResume() {
        super.onResume()
        root.onBackPressedDispatcher.addCallback(callbackExit)
    }

    /*@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun addOnBackPressedDispatcher(){
        root.onBackPressedDispatcher.addCallback(callbackExit)
    }*/
}