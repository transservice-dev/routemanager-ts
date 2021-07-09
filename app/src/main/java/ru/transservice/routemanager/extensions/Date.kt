package ru.transservice.routemanager.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Date.shortFormat():String {
    return SimpleDateFormat(
        "yyyy.MM.dd",
        Locale("ru")
    ).format(this)
}

fun Date.longFormat():String {
    return SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale("ru")
    ).format(this)
}