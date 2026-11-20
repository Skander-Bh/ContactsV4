package com.example.contacts.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.contacts.R

/**
 * Ecran Parametres decrit dans res/xml/preferences.xml. Les choix sont ecrits automatiquement
 * dans les SharedPreferences par defaut ; la liste les relit dans onResume.
 */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.titre_parametres)
    }
}
