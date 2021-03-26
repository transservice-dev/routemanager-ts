package ru.transservice.routemanager.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T, A, B> LiveData<A>.combineAndCompute(other: LiveData<B>, onChange: (A, B) -> T): MediatorLiveData<T>{
    var source1emitted = false
    var source2emitted = false

    val result = MediatorLiveData<T>()

    val mergeF = {
        val source1value: A? = this.value
        val source2value: B? = other.value

        if (source1emitted && source2emitted) {
            result.value = onChange.invoke(source1value!!, source2value!!)
        }
    }

    result.addSource(this){source1emitted = true; mergeF.invoke()}
    result.addSource(other) {source2emitted = true; mergeF.invoke()}

    return result
}