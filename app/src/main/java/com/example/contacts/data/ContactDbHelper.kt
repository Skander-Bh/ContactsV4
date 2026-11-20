package com.example.contacts.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.contacts.data.ContactContract.DATABASE_NAME
import com.example.contacts.data.ContactContract.DATABASE_VERSION
import com.example.contacts.data.ContactContract.SQL_CREATE_TABLE

/**
 * Gere la creation et la migration de contacts.db.
 *
 * - onCreate : appele UNE seule fois, quand le fichier contacts.db n'existe pas encore.
 * - onUpgrade : appele quand DATABASE_VERSION (code) > version stockee dans le fichier.
 * - Singleton construit avec applicationContext : une seule connexion partagee, pas de fuite d'Activity.
 */
class ContactDbHelper private constructor(
    context: Context,
    name: String,
    private val seedDemoData: Boolean
) : SQLiteOpenHelper(context.applicationContext, name, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
        if (seedDemoData) {
            DEMO_CONTACTS.forEach { ContactDao.insertWith(db, it) }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations incrementales : on n'efface JAMAIS les contacts de l'utilisateur.
        // Exemple pour le bonus (version 2) :
        //   if (oldVersion < 2) db.execSQL("ALTER TABLE contacts ADD COLUMN favori INTEGER NOT NULL DEFAULT 0")
        // Version 1 est la premiere version du schema : rien a migrer pour l'instant.
    }

    companion object {
        @Volatile
        private var INSTANCE: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactDbHelper(context.applicationContext, DATABASE_NAME, true)
                    .also { INSTANCE = it }
            }

        /** Helper independant (base de test), sans toucher au singleton ni a contacts.db. */
        fun createForTest(context: Context, name: String, seedDemoData: Boolean = false) =
            ContactDbHelper(context, name, seedDemoData)

        /** 7 contacts de demonstration, dont 3 sans coordonnees GPS (>= 2 exige). */
        val DEMO_CONTACTS = listOf(
            Contact(nom = "Ben Salah", prenom = "Ahmed", telephone = "+216 20 123 456",
                email = "ahmed.bensalah@example.com", latitude = 36.8065, longitude = 10.1815), // Tunis
            Contact(nom = "Trabelsi", prenom = "Sarra", telephone = "22 345 678",
                email = "sarra.trabelsi@example.com", latitude = 34.7406, longitude = 10.7603),  // Sfax
            Contact(nom = "Gharbi", prenom = "Amine", telephone = "+216 55 987 654",
                email = "amine.gharbi@example.com", latitude = 35.8256, longitude = 10.6084),    // Sousse
            Contact(nom = "O'Neil", prenom = "Sean", telephone = "+353 87 123 4567",
                email = "sean.oneil@example.com"),                                                // sans GPS
            Contact(nom = "Jlassi", prenom = "Yasmine", telephone = "98 765 432",
                email = "yasmine.jlassi@example.com", latitude = 37.2744, longitude = 9.8739),   // Bizerte
            Contact(nom = "Haddad", prenom = "Karim", telephone = "0021655111222"),               // sans GPS, sans e-mail
            Contact(nom = "Bouazizi", prenom = "Nour", telephone = "29 456 123",
                email = "nour.bouazizi@example.com")                                              // sans GPS
        )
    }
}
