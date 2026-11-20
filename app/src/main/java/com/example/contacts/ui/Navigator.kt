package com.example.contacts.ui

/** Navigation entre fragments, implementee par MainActivity (replace + addToBackStack). */
interface Navigator {
    /** [contactId] == 0 : mode creation. */
    fun openDetail(contactId: Long)
    fun openCarte(contactId: Long)
    fun openParametres()
}
