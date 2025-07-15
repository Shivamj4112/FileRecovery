package com.example.filerecovery.utils

object ContactUtils {
    fun phoneNumbersToString(phoneNumbers: List<String?>): String? {
        return phoneNumbers.filterNotNull().joinToString(",").ifEmpty { null }
    }

    fun stringToPhoneNumbers(phoneNumbers: String?): List<String> {
        return phoneNumbers?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
}