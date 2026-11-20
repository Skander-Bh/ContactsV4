package com.example.contacts.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.contacts.data.ContactContract.COL_EMAIL
import com.example.contacts.data.ContactContract.COL_ID
import com.example.contacts.data.ContactContract.COL_LATITUDE
import com.example.contacts.data.ContactContract.COL_LONGITUDE
import com.example.contacts.data.ContactContract.COL_NOM
import com.example.contacts.data.ContactContract.COL_PHOTO_URI
import com.example.contacts.data.ContactContract.COL_PRENOM
import com.example.contacts.data.ContactContract.COL_TELEPHONE
import com.example.contacts.data.ContactContract.ORDER_BY_NOM_PRENOM
import com.example.contacts.data.ContactContract.TABLE_CONTACTS

/**
 * Data Access Object : seule classe qui parle a SQLite (CRUD).
 *
 * - Toutes les valeurs variables passent par ContentValues ou par des arguments "?" (selectionArgs).
 * - Chaque Cursor est ferme avec use { }.
 * - Les methodes sont BLOQUANTES : l'appelant (ContactViewModel) les execute sur Dispatchers.IO.
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l'_id genere, ou -1 si l'insertion a echoue. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(TABLE_CONTACTS, null, toValues(contact))

    /** @return le nombre de lignes modifiees (0 = contact introuvable). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            TABLE_CONTACTS,
            toValues(contact),
            "$COL_ID = ?",
            arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimees (0 = contact introuvable). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(TABLE_CONTACTS, "$COL_ID = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> =
        helper.readableDatabase.query(
            TABLE_CONTACTS, null, null, null, null, null, ORDER_BY_NOM_PRENOM
        ).use { it.toContacts() }

    fun getById(id: Long): Contact? =
        helper.readableDatabase.query(
            TABLE_CONTACTS, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        ).use { if (it.moveToFirst()) it.toContact() else null }

    /**
     * Recherche (nom, prenom, telephone) par LIKE parametre.
     * Les caracteres speciaux de LIKE (% _ \) saisis par l'utilisateur sont echappes.
     */
    fun search(texte: String): List<Contact> {
        val motif = "%" + escapeLike(texte.trim()) + "%"
        val selection = "$COL_NOM LIKE ? ESCAPE '\\' OR " +
            "$COL_PRENOM LIKE ? ESCAPE '\\' OR " +
            "$COL_TELEPHONE LIKE ? ESCAPE '\\'"
        return helper.readableDatabase.query(
            TABLE_CONTACTS, null, selection, arrayOf(motif, motif, motif),
            null, null, ORDER_BY_NOM_PRENOM
        ).use { it.toContacts() }
    }

    companion object {
        /** Utilise aussi par ContactDbHelper.onCreate pour inserer les contacts de demonstration. */
        fun insertWith(db: SQLiteDatabase, contact: Contact): Long =
            db.insert(TABLE_CONTACTS, null, toValues(contact))

        private fun toValues(c: Contact) = ContentValues().apply {
            // _id n'est jamais fourni : AUTOINCREMENT s'en charge
            put(COL_NOM, c.nom.trim())
            put(COL_PRENOM, c.prenom.trim())
            put(COL_TELEPHONE, c.telephone.trim())
            put(COL_EMAIL, c.email?.trim()?.ifEmpty { null })
            put(COL_PHOTO_URI, c.photoUri)
            put(COL_LATITUDE, c.latitude)    // null -> NULL SQL
            put(COL_LONGITUDE, c.longitude)
        }

        internal fun escapeLike(s: String): String =
            s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}

// ---- Lecture d'un Cursor (index obtenus par getColumnIndexOrThrow) ----
private fun Cursor.toContacts(): List<Contact> {
    val list = ArrayList<Contact>(count)
    while (moveToNext()) list.add(toContact())
    return list
}

private fun Cursor.toContact(): Contact {
    val iId = getColumnIndexOrThrow(COL_ID)
    val iNom = getColumnIndexOrThrow(COL_NOM)
    val iPrenom = getColumnIndexOrThrow(COL_PRENOM)
    val iTel = getColumnIndexOrThrow(COL_TELEPHONE)
    val iEmail = getColumnIndexOrThrow(COL_EMAIL)
    val iPhoto = getColumnIndexOrThrow(COL_PHOTO_URI)
    val iLat = getColumnIndexOrThrow(COL_LATITUDE)
    val iLon = getColumnIndexOrThrow(COL_LONGITUDE)
    return Contact(
        id = getLong(iId),
        nom = getString(iNom),
        prenom = getString(iPrenom),
        telephone = getString(iTel),
        email = if (isNull(iEmail)) null else getString(iEmail),
        photoUri = if (isNull(iPhoto)) null else getString(iPhoto),
        latitude = if (isNull(iLat)) null else getDouble(iLat),
        longitude = if (isNull(iLon)) null else getDouble(iLon)
    )
}
