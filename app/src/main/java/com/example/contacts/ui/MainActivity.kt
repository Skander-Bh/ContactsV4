package com.example.contacts.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.example.contacts.R
import com.example.contacts.databinding.ActivityMainBinding

/**
 * Activite unique : barre d'application + FragmentContainerView qui heberge les fragments.
 * Le fragment racine n'est ajoute que si savedInstanceState == null : apres une rotation, le
 * FragmentManager restaure lui-meme la pile (pas de fragments en double).
 */
class MainActivity : AppCompatActivity(), Navigator {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Ecrit les valeurs par defaut de preferences.xml (sans ecraser les choix existants)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        supportFragmentManager.addOnBackStackChangedListener { updateNavigationUi() }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainer, ListeContactsFragment())
            }
        }
        updateNavigationUi()
    }

    private fun updateNavigationUi() {
        val canGoBack = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        invalidateOptionsMenu()
    }

    // ---------- Menu de la barre d'application ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // L'entree "Parametres" n'est proposee que sur l'ecran de liste
        menu.findItem(R.id.action_settings)?.isVisible = supportFragmentManager.backStackEntryCount == 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> {
                openParametres(); true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ---------- Navigator ----------

    override fun openDetail(contactId: Long) = navigateTo(DetailContactFragment.newInstance(contactId))

    override fun openCarte(contactId: Long) = navigateTo(CarteContactFragment.newInstance(contactId))

    override fun openParametres() = navigateTo(ParametresFragment())

    private fun navigateTo(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
            addToBackStack(null)
        }
    }
}
