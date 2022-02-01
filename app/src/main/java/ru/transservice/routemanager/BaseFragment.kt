package ru.transservice.routemanager

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleObserver
import androidx.navigation.NavController
import androidx.navigation.Navigation

abstract class BaseFragment() : Fragment(), LifecycleObserver {

    protected val root
        get() = requireActivity() as MainActivity

    protected lateinit var navController: NavController

    private val callbackExit = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleExit()
        }
    }

    open fun handleExit() {
        navController.popBackStack()
        // overwrite if needed
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        root.onBackPressedDispatcher.addCallback(this,callbackExit)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
    }
}