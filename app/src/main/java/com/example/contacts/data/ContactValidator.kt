package com.example.contacts.data

import androidx.annotation.StringRes
import com.example.contacts.R

/**
 * Validation pure (sans API Android) des champs d'un contact : testable en test unitaire local.
 * Un champ valide a une erreur null.
 */
object ContactValidator {

    data class Result(
        @StringRes val nomError: Int? = null,
        @StringRes val prenomError: Int? = null,
        @StringRes val telephoneError: Int? = null,
        @StringRes val emailError: Int? = null,
        @StringRes val latitudeError: Int? = null,
        @StringRes val longitudeError: Int? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) {
        val isValid: Boolean
            get() = nomError == null && prenomError == null && telephoneError == null &&
                emailError == null && latitudeError == null && longitudeError == null
    }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    // Accepte 0 / +216, espaces, points, tirets et parentheses : au moins 3 chiffres
    private val PHONE_REGEX = Regex("^\\+?[0-9 .()\\-]+$")

    const val LAT_MAX = 90.0
    const val LON_MAX = 180.0

    fun isValidLatitude(v: Double) = v.isFinite() && v in -LAT_MAX..LAT_MAX
    fun isValidLongitude(v: Double) = v.isFinite() && v in -LON_MAX..LON_MAX

    fun validate(
        nom: String,
        prenom: String,
        telephone: String,
        email: String,
        latitudeText: String,
        longitudeText: String
    ): Result {
        val nomErr = if (nom.isBlank()) R.string.err_nom_requis else null
        val prenomErr = if (prenom.isBlank()) R.string.err_prenom_requis else null

        val tel = telephone.trim()
        val telErr = when {
            tel.isEmpty() -> R.string.err_telephone_requis
            !PHONE_REGEX.matches(tel) || tel.count { it.isDigit() } < 3 -> R.string.err_telephone_invalide
            else -> null
        }

        val mail = email.trim()
        val emailErr = if (mail.isNotEmpty() && !EMAIL_REGEX.matches(mail)) R.string.err_email_invalide else null

        // --- GPS : soit les deux champs vides, soit les deux valides ---
        val latT = latitudeText.trim()
        val lonT = longitudeText.trim()
        var latErr: Int? = null
        var lonErr: Int? = null
        var lat: Double? = null
        var lon: Double? = null

        if (latT.isNotEmpty() || lonT.isNotEmpty()) {
            if (latT.isEmpty()) latErr = R.string.err_gps_paire
            if (lonT.isEmpty()) lonErr = R.string.err_gps_paire
            if (latT.isNotEmpty()) {
                val v = latT.replace(',', '.').toDoubleOrNull()
                when {
                    v == null || !v.isFinite() -> latErr = R.string.err_gps_non_numerique
                    !isValidLatitude(v) -> latErr = R.string.err_latitude_limites
                    else -> lat = v
                }
            }
            if (lonT.isNotEmpty()) {
                val v = lonT.replace(',', '.').toDoubleOrNull()
                when {
                    v == null || !v.isFinite() -> lonErr = R.string.err_gps_non_numerique
                    !isValidLongitude(v) -> lonErr = R.string.err_longitude_limites
                    else -> lon = v
                }
            }
        }

        return Result(
            nomError = nomErr,
            prenomError = prenomErr,
            telephoneError = telErr,
            emailError = emailErr,
            latitudeError = latErr,
            longitudeError = lonErr,
            latitude = lat,
            longitude = lon
        )
    }

    /** Derniere barriere avant ecriture en base (utilisee par le ViewModel). */
    fun isStorable(c: Contact): Boolean {
        if (c.nom.isBlank() || c.prenom.isBlank() || c.telephone.isBlank()) return false
        val lat = c.latitude
        val lon = c.longitude
        if ((lat == null) != (lon == null)) return false
        if (lat != null && !isValidLatitude(lat)) return false
        if (lon != null && !isValidLongitude(lon)) return false
        return true
    }
}
