package com.example.contacts.ui

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.ContactDao
import com.example.contacts.data.ContactDbHelper
import com.example.contacts.data.ContactValidator
import com.example.contacts.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Evenements ponctuels (consommes une seule fois) envoyes aux ecrans. */
sealed interface ContactEvent {
    data class Saved(val created: Boolean) : ContactEvent
    data object Deleted : ContactEvent
    data class Error(@StringRes val messageRes: Int) : ContactEvent
}

/**
 * ViewModel partage (scope Activity) :
 *  - expose la liste des contacts (filtree) sous forme de StateFlow ;
 *  - execute TOUS les acces au DAO dans viewModelScope + Dispatchers.IO ;
 *  - recharge la liste apres chaque modification.
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))

    private val query = MutableStateFlow("")
    private val reloadTrigger = MutableStateFlow(0)

    val currentQuery: String get() = query.value

    /** null = premier chargement pas encore termine. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val contacts: StateFlow<List<Contact>?> =
        combine(query.debounce(FILTER_DEBOUNCE_MS), reloadTrigger) { q, _ -> q }
            .mapLatest { q -> loadContacts(q) }
            .stateIn<List<Contact>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<ContactEvent>(Channel.BUFFERED)
    val events: Flow<ContactEvent> = _events.receiveAsFlow()

    fun setQuery(text: String) {
        query.value = text
    }

    /** Relit la base (appele a chaque affichage de la liste). */
    fun refresh() {
        reloadTrigger.value = reloadTrigger.value + 1
    }

    private suspend fun loadContacts(q: String): List<Contact> =
        try {
            withContext(Dispatchers.IO) { if (q.isBlank()) dao.getAll() else dao.search(q) }
        } catch (e: SQLiteException) {
            _events.trySend(ContactEvent.Error(R.string.err_chargement))
            emptyList()
        }

    /** Lecture d'un contact par _id (hors thread principal). */
    suspend fun getContact(id: Long): Contact? =
        try {
            withContext(Dispatchers.IO) { dao.getById(id) }
        } catch (e: SQLiteException) {
            null
        }

    /**
     * Cree (id == 0) ou met a jour le contact.
     * [obsoletePhotos] : anciennes photos a supprimer du stockage si l'ecriture reussit.
     */
    fun save(contact: Contact, obsoletePhotos: List<String?> = emptyList()) {
        if (!ContactValidator.isStorable(contact)) {
            _events.trySend(ContactEvent.Error(R.string.err_contact_invalide)); return
        }
        viewModelScope.launch {
            val creation = contact.id == 0L
            val event: ContactEvent = try {
                withContext(Dispatchers.IO) {
                    val ok = if (creation) dao.insert(contact) != -1L else dao.update(contact) > 0
                    when {
                        ok -> {
                            obsoletePhotos.forEach { PhotoStorage.deleteQuietly(getApplication<Application>(), it) }
                            ContactEvent.Saved(creation)
                        }
                        creation -> ContactEvent.Error(R.string.err_insertion)
                        else -> ContactEvent.Error(R.string.err_contact_introuvable)
                    }
                }
            } catch (e: SQLiteConstraintException) {
                ContactEvent.Error(R.string.err_contrainte)
            } catch (e: SQLiteException) {
                ContactEvent.Error(R.string.err_insertion)
            }
            _events.send(event)
            if (event is ContactEvent.Saved) refresh()
        }
    }

    /** Supprime le contact puis ses fichiers photo ([photos] : photo enregistree + eventuelle photo non enregistree). */
    fun delete(id: Long, photos: List<String?>) {
        viewModelScope.launch {
            val event: ContactEvent = try {
                withContext(Dispatchers.IO) {
                    if (dao.delete(id) > 0) {
                        photos.forEach { PhotoStorage.deleteQuietly(getApplication<Application>(), it) }
                        ContactEvent.Deleted
                    } else {
                        ContactEvent.Error(R.string.err_contact_introuvable)
                    }
                }
            } catch (e: SQLiteException) {
                ContactEvent.Error(R.string.err_suppression)
            }
            _events.send(event)
            if (event is ContactEvent.Deleted) refresh()
        }
    }

    companion object {
        private const val FILTER_DEBOUNCE_MS = 150L
    }
}
