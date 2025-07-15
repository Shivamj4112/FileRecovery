package com.example.filerecovery.data.viewmodel.local

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.filerecovery.data.datasource.ContactDao
import com.example.filerecovery.data.model.Contact
import com.example.filerecovery.data.model.ContactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.property.StructuredName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactDao: ContactDao
) : ViewModel() {

    private val _allContactsCount = MutableLiveData<Int>()
    val allContactsCount: LiveData<Int> = _allContactsCount

    private val _deletedContactsCount = MutableLiveData<Int>()
    val deletedContactsCount: LiveData<Int> = _deletedContactsCount

    private val _contactsList = MutableLiveData<List<Contact>>()
    val contactsList: LiveData<List<Contact>> = _contactsList

    private val _deletedContactsList = MutableLiveData<List<Contact>>()
    val deletedContactsList: LiveData<List<Contact>> = _deletedContactsList

    private val _selectedContactsSize = MutableLiveData<Int>()
    val selectedContactsSize: LiveData<Int> = _selectedContactsSize

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _backupFilesList = MutableLiveData<List<File>>()
    val backupFilesList: LiveData<List<File>> = _backupFilesList

    private val _backupFileCount = MutableLiveData<Int>()
    val backupFileCount: LiveData<Int> = _backupFileCount

    private val _vcfContactsList = MutableLiveData<List<Contact>>()
    val vcfContactsList: LiveData<List<Contact>> = _vcfContactsList

    private val selectedContacts = mutableSetOf<Contact>()
    private val selectedBackupFiles = mutableSetOf<File>()
    private var allContacts = listOf<Contact>()
    private var deletedContacts = listOf<Contact>()

    // Function to fetch all contacts

    suspend fun fetchAllContacts(contentResolver: ContentResolver) {
        withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            val contacts = mutableListOf<Contact>()
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    if (id != null && name != null) {
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        val phoneNumber = phoneCursor?.use { pCursor ->
                            val numberIndex =
                                pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (pCursor.moveToFirst()) {
                                pCursor.getString(numberIndex)
                            } else null
                        }
                        contacts.add(Contact(id, name, phoneNumber))
                        phoneCursor?.close()
                    }
                }
            }

            val existingContacts = contactDao.getAllContacts()
            val existingContactIds = existingContacts.map { it.id }.toSet()
            val newContacts = contacts.filter { it.id !in existingContactIds }
            contactDao.insertContacts(newContacts.map {
                ContactEntity(
                    it.id,
                    it.name,
                    it.phoneNumber
                )
            })

            allContacts = contacts
            _allContactsCount.postValue(contacts.size)
            _contactsList.postValue(contacts)
            _isLoading.postValue(false)

            fetchDeletedContacts()
        }
    }

    // Function to fetch deleted contacts
    suspend fun fetchDeletedContacts() {
        withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            val dbContacts = contactDao.getAllContacts()
            val currentContactIds = allContacts.map { it.id }.toSet()
            val deleted = dbContacts.filter { it.id !in currentContactIds }
            deletedContacts =
                deleted.map { Contact(it.id, it.name, it.phoneNumber, isDeleted = true) }
            _deletedContactsCount.postValue(deleted.size)
            _deletedContactsList.postValue(deletedContacts)
            _isLoading.postValue(false)
        }
    }

    suspend fun fetchBackupFiles(storagePath: String?) {
        withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            val backupDir = File(storagePath, "ContactsBackup")
            val files =
                backupDir.listFiles { _, name -> name.endsWith(".vcf") }?.toList() ?: emptyList()
            _backupFilesList.postValue(files)
            _backupFileCount.postValue(files.size)
            _isLoading.postValue(false)
        }
    }

    fun loadAllContacts() {
        _contactsList.postValue(allContacts)
    }

    fun loadDeletedContacts() {
        _deletedContactsList.postValue(deletedContacts)
    }

    fun toggleContactSelection(contact: Contact, isSelected: Boolean) {
        if (isSelected) {
            selectedContacts.add(contact.copy(isSelected = true))
        } else {
            selectedContacts.removeAll { it.id == contact.id }
        }
        allContacts = allContacts.map { if (it.id == contact.id) it.copy(isSelected = isSelected) else it }
        deletedContacts = deletedContacts.map { if (it.id == contact.id) it.copy(isSelected = isSelected) else it }
        _selectedContactsSize.postValue(selectedContacts.size)
        _contactsList.postValue(allContacts)
        _deletedContactsList.postValue(deletedContacts)
    }

    fun selectAllContacts(select: Boolean) {
        val vcfContacts = _contactsList.value?.map { contact ->
            if (select) {
                selectedContacts.add(contact.copy(isSelected = true))
                contact.copy(isSelected = true)
            } else {
                selectedContacts.removeAll { it.id == contact.id }
                contact.copy(isSelected = false)
            }
        } ?: emptyList()
        _contactsList.postValue(vcfContacts)
        _selectedContactsSize.postValue(selectedContacts.size)
    }

    fun selectAllVcfContacts(select: Boolean) {
        val vcfContacts = _vcfContactsList.value?.map { contact ->
            if (select) {
                selectedContacts.add(contact.copy(isSelected = true))
                contact.copy(isSelected = true)
            } else {
                selectedContacts.removeAll { it.id == contact.id }
                contact.copy(isSelected = false)
            }
        } ?: emptyList()
        _vcfContactsList.postValue(vcfContacts)
        _selectedContactsSize.postValue(selectedContacts.size)
    }

    fun getSelectedContacts(): Set<Contact> = selectedContacts

    fun getSelectedBackupFiles(): Set<File> = selectedBackupFiles

    fun clearSelections() {
        selectedContacts.clear()
        selectedBackupFiles.clear()
        allContacts = allContacts.map { it.copy(isSelected = false) }
        deletedContacts = deletedContacts.map { it.copy(isSelected = false) }
        _selectedContactsSize.postValue(0)
        _contactsList.postValue(allContacts)
        _deletedContactsList.postValue(deletedContacts)
    }

    //  Functions to handle contact recovery
    suspend fun recoverSelectedContacts(contentResolver: ContentResolver): String {
        return withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            val ops = ArrayList<ContentProviderOperation>()
            val contactIdMapping = mutableMapOf<String, Contact>() // Map temp ID to Contact for matching

            selectedContacts.forEach { contact ->
                val tempId = "temp_${System.currentTimeMillis()}_${contact.name.hashCode()}"
                contactIdMapping[tempId] = contact

                // Insert Raw Contact
                val rawContactInsertIndex = ops.size
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )

                // Insert Contact Name
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                        .build()
                )

                // Insert Phone Number
                if (contact.phoneNumber != null) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                            .build()
                    )
                }
            }

            try {
                // Apply batch operations to restore contacts
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                Log.d("ContactsViewModel", "Batch applied, restored ${selectedContacts.size} contacts")

                // Retrieve new contact IDs from device
                val deviceContacts = mutableListOf<Contact>()
                contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex)
                        if (id != null && name != null) {
                            val phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            val phoneNumber = phoneCursor?.use { pCursor ->
                                val numberIndex = pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pCursor.moveToFirst()) pCursor.getString(numberIndex) else null
                            }
                            deviceContacts.add(Contact(id, name, phoneNumber))
                            phoneCursor?.close()
                        }
                    }
                }

                // Update database with new contact IDs
                selectedContacts.forEach { contact ->
                    val deviceContact = deviceContacts.find {
                        it.name == contact.name && it.phoneNumber == contact.phoneNumber
                    }
                    if (deviceContact != null) {
                        // Update existing contact ID in database to match device ID
                        contactDao.updateContactId(contact.id, deviceContact.id)
                        Log.d("ContactsViewModel", "Updated contact ID from ${contact.id} to ${deviceContact.id}")
                    } else {
                        // This should not happen if restoration was successful
                        Log.w("ContactsViewModel", "Could not find restored contact: ${contact.name}")
                    }
                }

                // Clear selections and refresh lists
                clearSelections()
                fetchAllContacts(contentResolver) // Refresh all contacts and deleted contacts
                _isLoading.postValue(false)
                "Contacts recovered successfully"
            } catch (e: Exception) {
                _isLoading.postValue(false)
                Log.e("ContactsViewModel", "Error recovering contacts: ${e.message}", e)
                "Error recovering contacts: ${e.message}"
            }
        }
    }

    // Function to restore contacts from a backup file
    suspend fun backupSelectedContacts(fileName: String, storagePath: String?): String {
        return withContext(Dispatchers.IO) {
            if (selectedContacts.isEmpty()) {
                return@withContext "No contacts selected"
            }
            try {
                val backupDir = File(storagePath, "ContactsBackup")
                if (!backupDir.exists()) backupDir.mkdirs()

                val backupFile = File(backupDir, "$fileName.vcf")
                FileOutputStream(backupFile).use { fos ->
                    selectedContacts.forEach { contact ->
                        val vcard = contact.toVCard()
                        Log.d("ContactsViewModel", "Writing vCard for ${contact.name}: $vcard")
                        fos.write(vcard.toByteArray(Charsets.UTF_8))
                        fos.write("\n\n".toByteArray(Charsets.UTF_8)) // Add double newline for clear separation
                    }
                }
                clearSelections()
                fetchBackupFiles(storagePath)
                "Backup created at ${backupFile.absolutePath}"
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error backing up contacts", e)
                "Error backing up contacts: ${e.message}"
            }
        }
    }


    // Function to recover contacts from a VCF file
    suspend fun recoverFromVcfFile(contentResolver: ContentResolver): String {
        return withContext(Dispatchers.IO) {
            if (selectedBackupFiles.isEmpty()) {
                Log.d("ContactsViewModel", "No backup files selected")
                return@withContext "No backup files selected"
            }
            _isLoading.postValue(true)
            val results = mutableListOf<String>()

            selectedBackupFiles.forEach { file ->
                try {
                    Log.d("ContactsViewModel", "Processing VCF file: ${file.name}")
                    val vCards: List<VCard> = FileInputStream(file).use { fis ->
                        Ezvcard.parse(fis).all()
                    }
                    Log.d("ContactsViewModel", "Found ${vCards.size} vCard entries in ${file.name}")

                    if (vCards.isEmpty()) {
                        results.add("No valid contacts found in ${file.name}")
                        Log.w("ContactsViewModel", "No valid contacts in ${file.name}")
                        return@forEach
                    }

                    val ops = ArrayList<ContentProviderOperation>()
                    val restoredContacts = mutableListOf<Contact>()
                    val contactIdMapping = mutableMapOf<String, Pair<String, ContactEntity?>>()

                    vCards.forEach { vCard ->
                        val name = vCard.structuredName?.let { structuredName ->
                            structuredName.given ?: structuredName.family ?: ""
                        } ?: vCard.formattedName?.value ?: ""
                        val phone = vCard.telephoneNumbers.firstOrNull()?.text ?: ""

                        if (name.isNotBlank()) {
                            // Check if contact exists in the database
                            val existingContact = contactDao.getContactByNameAndPhone(name, phone)
                            val tempId = "temp_${System.currentTimeMillis()}_${name.hashCode()}"

                            val contact = Contact(
                                id = tempId, // Temporary ID for batch operation
                                name = name,
                                phoneNumber = if (phone.isNotBlank()) phone else null
                            )
                            restoredContacts.add(contact)
                            contactIdMapping[tempId] = Pair(name, existingContact)
                            Log.d(
                                "ContactsViewModel",
                                "Parsed contact: $name, $phone, Temp ID: $tempId"
                            )

                            // Add ContentProviderOperations to restore contact
                            val rawContactInsertIndex = ops.size
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                    .build()
                            )
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(
                                        ContactsContract.Data.RAW_CONTACT_ID,
                                        rawContactInsertIndex
                                    )
                                    .withValue(
                                        ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                                    )
                                    .withValue(
                                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                        name
                                    )
                                    .build()
                            )
                            if (phone.isNotBlank()) {
                                ops.add(
                                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(
                                            ContactsContract.Data.RAW_CONTACT_ID,
                                            rawContactInsertIndex
                                        )
                                        .withValue(
                                            ContactsContract.Data.MIMETYPE,
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                        )
                                        .withValue(
                                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                                            phone
                                        )
                                        .withValue(
                                            ContactsContract.CommonDataKinds.Phone.TYPE,
                                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                        )
                                        .build()
                                )
                            }
                        } else {
                            Log.w(
                                "ContactsViewModel",
                                "Skipping vCard with blank name in ${file.name}"
                            )
                        }
                    }

                    if (restoredContacts.isEmpty()) {
                        results.add("No valid contacts found in ${file.name}")
                        Log.w("ContactsViewModel", "No valid contacts in ${file.name}")
                        return@forEach
                    }

                    // Apply batch operations to restore contacts
                    val batchResults = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    Log.d(
                        "ContactsViewModel",
                        "Batch applied for ${file.name}, results: ${batchResults.size}"
                    )

                    // Retrieve new contact IDs from device
                    val deviceContacts = mutableListOf<Contact>()
                    contentResolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                        val nameIndex =
                            cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            val id = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            if (id != null && name != null) {
                                val phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id),
                                    null
                                )
                                val phoneNumber = phoneCursor?.use { pCursor ->
                                    val numberIndex =
                                        pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (pCursor.moveToFirst()) pCursor.getString(numberIndex) else null
                                }
                                deviceContacts.add(Contact(id, name, phoneNumber))
                                phoneCursor?.close()
                            }
                        }
                    }

                    // Update database with new contact IDs
                    restoredContacts.forEach { restoredContact ->
                        val (name, existingContact) = contactIdMapping[restoredContact.id]
                            ?: return@forEach
                        val deviceContact = deviceContacts.find {
                            it.name == name && it.phoneNumber == restoredContact.phoneNumber
                        }
                        if (deviceContact != null) {
                            if (existingContact != null) {
                                // Update existing contact ID in database to match device ID
                                contactDao.updateContactId(existingContact.id, deviceContact.id)
                            } else {
                                // Insert new contact into database
                                contactDao.insertContacts(
                                    listOf(
                                        ContactEntity(
                                            deviceContact.id,
                                            name,
                                            restoredContact.phoneNumber
                                        )
                                    )
                                )
                            }
                        }
                    }

                    results.add("Recovered ${restoredContacts.size} contacts from ${file.name}")
                } catch (e: Exception) {
                    Log.e(
                        "ContactsViewModel",
                        "Error recovering from ${file.name}: ${e.message}",
                        e
                    )
                    results.add("Error recovering from ${file.name}: ${e.message}")
                }
            }

            clearSelections()
            fetchAllContacts(contentResolver) // Refresh all contacts and deleted contacts
            _isLoading.postValue(false)
            Log.d("ContactsViewModel", "Recovery complete, results: $results")
            results.joinToString("\n")
        }
    }

    suspend fun fetchContactsFromVcf(file: File) {
        withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val vCards: List<VCard> = FileInputStream(file).use { fis ->
                    Ezvcard.parse(fis).all()
                }
                val contacts = vCards.mapNotNull { vCard ->
                    val name = vCard.structuredName?.let { structuredName ->
                        structuredName.given ?: structuredName.family ?: ""
                    } ?: vCard.formattedName?.value ?: ""
                    val phone = vCard.telephoneNumbers.firstOrNull()?.text
                    if (name.isNotBlank()) {
                        Contact(
                            id = "vcf_${System.currentTimeMillis()}_${name.hashCode()}",
                            name = name,
                            phoneNumber = phone,
                            isSelected = false
                        )
                    } else null
                }
                _vcfContactsList.postValue(contacts)
                Log.d("ContactsViewModel", "Fetched ${contacts.size} contacts from ${file.name}")
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error parsing VCF file ${file.name}: ${e.message}", e)
                _vcfContactsList.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}