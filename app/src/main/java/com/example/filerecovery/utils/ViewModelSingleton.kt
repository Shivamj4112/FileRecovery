package com.example.filerecovery.utils

import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.data.viewmodel.local.FileRecoveryViewModel

object ViewModelSingleton {
    lateinit var fileRecoveryModel: FileRecoveryViewModel
    lateinit var contactsViewModel: ContactsViewModel

    fun init(fileRecoveryModel: FileRecoveryViewModel, contactsViewModel: ContactsViewModel) {
        this.fileRecoveryModel = fileRecoveryModel
        this.contactsViewModel = contactsViewModel
    }
}