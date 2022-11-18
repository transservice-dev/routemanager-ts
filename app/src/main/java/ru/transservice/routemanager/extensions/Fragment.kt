package ru.transservice.routemanager.extension

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T : ViewBinding> Fragment.viewBinding(viewBinder: (View) -> T): ReadOnlyProperty<Fragment, T> {
    return FragmentViewBindingDelegate(viewBinder)
}

private class FragmentViewBindingDelegate<T: ViewBinding> (
    val viewBinder: (View) -> T
) : ReadOnlyProperty<Fragment, T>, LifecycleEventObserver {

    private var binding: T? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        binding?.let { return it }

        val lifecycle = thisRef.viewLifecycleOwner.lifecycle
        val viewBinding = viewBinder(thisRef.requireView())
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            binding = viewBinding
            lifecycle.addObserver(this)
        }

        return viewBinding
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            binding = null
            source.lifecycle.removeObserver(this)
        }
    }
}