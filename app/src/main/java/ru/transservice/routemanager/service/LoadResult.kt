package ru.transservice.routemanager.service

import java.lang.Exception
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class LoadResult<T>(
    val data: T?,
    val errorMessage: String?=null,
    val e: Exception? = null
){
    class Success<T>(data:T): LoadResult<T>(data)
    class Loading<T>(data:T?=null): LoadResult<T>(data)
    class Error<T>(message:String, e: Exception? = null, data: T? = null): LoadResult<T>(data,message,e)
}

fun <T> LoadResult<T>.errorDescription(): String {
    return when (e) {
        is UnknownHostException -> "Ошибка: неизвестное имя сервера. Проверьте наличие интернета на устройстве."
        is SocketTimeoutException -> "Ошибка соединения. Сервер не отвечает. Проверьте наличие интернета на устройстве."
        is SecurityException -> "Ошибка авторизации. Проверьте правильность ввода пароля."
        else -> "При выгрузке/загрузке данных произошла ошибка."
    }
}
