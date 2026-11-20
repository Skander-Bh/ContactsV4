package com.example.contacts.data

/**
 * Modele d'un contact.
 * [id] vaut 0 tant que le contact n'est pas enregistre, puis prend la valeur de la colonne _id.
 */
data class Contact(
    val id: Long = 0L,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,   // chemin absolu d'une copie dans filesDir/photos/
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val hasPosition: Boolean get() = latitude != null && longitude != null
    val nomComplet: String get() = "$prenom $nom".trim()
}
