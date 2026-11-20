package com.example.contacts.prefs

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager

/** Valeurs deja converties, pretes a etre appliquees aux vues. */
data class DisplayPrefs(
    val tailleSp: Float,
    @androidx.annotation.ColorInt val couleur: Int,
    val appelDepuisListe: Boolean
)

/**
 * Point d'acces unique aux preferences (SharedPreferences par defaut, alimentees par ParametresFragment).
 * Les cles et valeurs par defaut sont centralisees ici et dans res/xml/preferences.xml.
 */
class PreferencesManager(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun load(): DisplayPrefs = DisplayPrefs(
        tailleSp = tailleSp(),
        couleur = couleurAffichee(),
        appelDepuisListe = appelDepuisListe()
    )

    fun tailleSp(): Float =
        prefs.getString(KEY_TAILLE, DEFAULT_TAILLE)?.toFloatOrNull()?.takeIf { it in 8f..48f }
            ?: DEFAULT_TAILLE.toFloat()

    /** Couleur brute choisie (#RRGGBB), sans adaptation au theme. */
    fun couleurBrute(): Int =
        parseColor(prefs.getString(KEY_COULEUR, DEFAULT_COULEUR))

    /** Couleur lisible sur le fond courant : eclaircie en theme sombre. */
    fun couleurAffichee(): Int = adaptToTheme(couleurBrute(), isNightMode())

    fun appelDepuisListe(): Boolean = prefs.getBoolean(KEY_APPEL, DEFAULT_APPEL)

    private fun isNightMode(): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    companion object {
        const val KEY_TAILLE = "taille_police"
        const val KEY_COULEUR = "couleur_preferee"
        const val KEY_APPEL = "appel_depuis_liste"

        const val DEFAULT_TAILLE = "16"
        const val DEFAULT_COULEUR = "#000000"
        const val DEFAULT_APPEL = false

        fun parseColor(hex: String?): Int =
            try {
                Color.parseColor(hex ?: DEFAULT_COULEUR)
            } catch (e: IllegalArgumentException) {
                Color.BLACK
            }

        /**
         * Theme sombre : le noir devient blanc, les autres couleurs sont melangees a 45 % de blanc
         * pour rester lisibles sur un fond sombre. Theme clair : couleur inchangee.
         */
        fun adaptToTheme(color: Int, night: Boolean): Int {
            if (!night) return color
            if (ColorUtils.calculateLuminance(color) < 0.05) return Color.WHITE
            return ColorUtils.blendARGB(color, Color.WHITE, 0.45f)
        }
    }
}
