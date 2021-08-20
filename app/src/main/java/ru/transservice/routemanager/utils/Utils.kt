package ru.transservice.routemanager.utils

import java.util.*

object Utils {
    private val translitarationDictionary = mapOf(
        "а" to "a",
        "б" to "b",
        "в" to "v",
        "г" to "g",
        "д" to "d",
        "е" to "e",
        "ё" to "e",
        "ж" to "zh",
        "з" to "z",
        "и" to "i",
        "й" to "i",
        "к" to "k",
        "л" to "l",
        "м" to "m",
        "н" to "n",
        "о" to "o",
        "п" to "p",
        "р" to "r",
        "с" to "s",
        "т" to "t",
        "у" to "u",
        "ф" to "f",
        "х" to "h",
        "ц" to "c",
        "ч" to "ch",
        "ш" to "sh",
        "щ" to "sh'",
        "ъ" to "",
        "ы" to "i",
        "ь" to "",
        "э" to "e",
        "ю" to "yu",
        "я" to "ya",
        "№" to "#"
    )

    fun transliteration(payload:String, divider:String = " "):String{
        var transliterationText = ""
        payload.forEach {
            if (it.toString()==" ") {
                transliterationText += divider
            }else{
                val mapKey = it.lowercaseChar().toString()
                var newValue: String = it.toString()
                if (translitarationDictionary.containsKey(mapKey)) {
                    newValue = translitarationDictionary[mapKey].toString()
                    if (it.isUpperCase()) {
                        newValue = newValue.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }
                    }
                }
                transliterationText += newValue
            }
        }
        return transliterationText
    }

    fun vehicleNumToLatin(number:String):String{
        return number.replace(Regex("[АВЕКМНОРСТУХ]")) {
            when (it.value) {
                "А" -> "A"
                "В" -> "B"
                "Е" -> "E"
                "К" -> "K"
                "М" -> "M"
                "Н" -> "H"
                "О" -> "O"
                "Р" -> "P"
                "С" -> "C"
                "Т" -> "T"
                "У" -> "Y"
                "Х" -> "X"
                else -> it.value
            }
        }
    }

}