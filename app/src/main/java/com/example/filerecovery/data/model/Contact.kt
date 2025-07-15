package com.example.filerecovery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    var isSelected: Boolean = false,
    val isDeleted: Boolean = false
) : Parcelable{
    fun toVCard(): String {
        return """
        BEGIN:VCARD
        VERSION:3.0
        N:$name;;;;
        FN:$name
        ${phoneNumber?.let { "TEL;CELL:$it" } ?: ""}
        END:VCARD
        """.trimIndent()
    }
}