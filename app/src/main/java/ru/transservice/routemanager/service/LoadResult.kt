package ru.transservice.routemanager.service

import java.lang.Exception

sealed class LoadResult<T>(
    val data: T?,
    val errorMessage: String?=null,
    val e: Exception? = null
){
    class Success<T>(data:T): LoadResult<T>(data)
    class Loading<T>(data:T?=null): LoadResult<T>(data)
    class Error<T>(message:String, e: Exception? = null, data: T? = null): LoadResult<T>(data,message,e)
}