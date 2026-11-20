package com.example.contacts.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactValidatorTest {

    private fun validate(
        nom: String = "Ben Salah", prenom: String = "Ahmed", tel: String = "+216 20 123 456",
        email: String = "", lat: String = "", lon: String = ""
    ) = ContactValidator.validate(nom, prenom, tel, email, lat, lon)

    @Test fun contactComplet_estValide() {
        val r = validate(email = "a@b.tn", lat = "36.8", lon = "10.18")
        assertTrue(r.isValid)
        assertEquals(36.8, r.latitude!!, 0.0)
        assertEquals(10.18, r.longitude!!, 0.0)
    }

    @Test fun nomPrenomTelephoneVides_sontRefuses() {
        val r = validate(nom = "  ", prenom = "", tel = "")
        assertFalse(r.isValid)
        assertNotNull(r.nomError); assertNotNull(r.prenomError); assertNotNull(r.telephoneError)
    }

    @Test fun apostrophe_estAccepteeDansLeNom() {
        assertTrue(validate(nom = "O'Neil", prenom = "L'Ariana").isValid)
    }

    @Test fun telephone_zeroEtPlus216_acceptes() {
        assertTrue(validate(tel = "0021655111222").isValid)
        assertTrue(validate(tel = "+216 55 111 222").isValid)
        assertFalse(validate(tel = "abc").isValid)
    }

    @Test fun email_invalide_refuse_vide_accepte() {
        assertFalse(validate(email = "pas-un-mail").isValid)
        assertTrue(validate(email = "").isValid)
    }

    @Test fun gps_absent_estValide_etNull() {
        val r = validate()
        assertTrue(r.isValid); assertNull(r.latitude); assertNull(r.longitude)
    }

    @Test fun gps_incomplet_estRefuse() {
        assertFalse(validate(lat = "36.8").isValid)
        assertFalse(validate(lon = "10.1").isValid)
    }

    @Test fun gps_nonNumerique_estRefuse() {
        assertFalse(validate(lat = "abc", lon = "10").isValid)
        assertFalse(validate(lat = "NaN", lon = "10").isValid)
        assertFalse(validate(lat = "10", lon = "Infinity").isValid)
    }

    @Test fun gps_horsLimites_estRefuse() {
        assertFalse(validate(lat = "91", lon = "10").isValid)
        assertFalse(validate(lat = "-90.1", lon = "10").isValid)
        assertFalse(validate(lat = "10", lon = "181").isValid)
        assertTrue(validate(lat = "90", lon = "-180").isValid)
    }

    @Test fun gps_virguleDecimale_estAcceptee() {
        assertEquals(36.8065, validate(lat = "36,8065", lon = "10,1815").latitude!!, 0.0)
    }

    @Test fun isStorable_refuseContactsInvalides() {
        assertTrue(ContactValidator.isStorable(Contact(nom = "A", prenom = "B", telephone = "1")))
        assertFalse(ContactValidator.isStorable(Contact(nom = "", prenom = "B", telephone = "1")))
        assertFalse(ContactValidator.isStorable(Contact(nom = "A", prenom = "B", telephone = "1", latitude = 10.0)))
        assertFalse(ContactValidator.isStorable(Contact(nom = "A", prenom = "B", telephone = "1", latitude = 99.0, longitude = 0.0)))
    }
}
