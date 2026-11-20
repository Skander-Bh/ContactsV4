package com.example.contacts.ui

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.databinding.ItemContactBinding
import com.example.contacts.prefs.DisplayPrefs
import com.example.contacts.prefs.PreferencesManager
import com.example.contacts.util.ImageLoader
import kotlinx.coroutines.CoroutineScope

/**
 * Adapter de la liste. Il ne lit jamais les preferences lui-meme : il recoit un [DisplayPrefs]
 * deja converti (taille en sp, couleur en Int, booleen). Le contact est identifie par son _id.
 */
class ContactAdapter(
    private val scope: CoroutineScope,
    private val onContactClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ViewHolder>(DIFF) {

    private var prefs = DisplayPrefs(
        tailleSp = PreferencesManager.DEFAULT_TAILLE.toFloat(),
        couleur = PreferencesManager.parseColor(PreferencesManager.DEFAULT_COULEUR),
        appelDepuisListe = PreferencesManager.DEFAULT_APPEL
    )

    /** Applique de nouvelles preferences et redessine les lignes visibles (sans redemarrage). */
    fun updatePrefs(newPrefs: DisplayPrefs) {
        if (newPrefs == prefs) return
        prefs = newPrefs
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        private var contact: Contact? = null

        init {
            // Clic sur la ligne -> detail ; le bouton Appeler a son propre listener et consomme son clic
            b.root.setOnClickListener { contact?.let(onContactClick) }
            b.btnAppeler.setOnClickListener { contact?.let(onCallClick) }
        }

        fun bind(c: Contact) {
            contact = c
            b.tvNomPrenom.apply {
                text = "${c.nom} ${c.prenom}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp)
                setTextColor(prefs.couleur)
            }
            b.tvTelephone.apply {
                text = c.telephone
                setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp)
            }
            b.btnAppeler.visibility = if (prefs.appelDepuisListe) View.VISIBLE else View.GONE

            val sizePx = b.ivPhoto.resources.getDimensionPixelSize(R.dimen.photo_liste)
            ImageLoader.load(b.ivPhoto, c.photoUri, sizePx, scope, R.drawable.ic_person)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
