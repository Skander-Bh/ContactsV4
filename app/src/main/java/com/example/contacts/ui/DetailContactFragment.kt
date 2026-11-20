package com.example.contacts.ui

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.ContactValidator
import com.example.contacts.databinding.FragmentDetailContactBinding
import com.example.contacts.prefs.PreferencesManager
import com.example.contacts.util.ImageLoader
import com.example.contacts.util.IntentActions
import com.example.contacts.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Creation (contactId == 0), consultation, modification et suppression d'un contact.
 *
 * Rotation : les champs texte (EditText avec id) sont restaures automatiquement par Android ;
 * [formFilled], les chemins de photo et le fichier camera en attente sont sauvegardes dans
 * onSaveInstanceState. On ne remplit donc le formulaire depuis la base qu'UNE fois, pour ne jamais
 * ecraser une saisie en cours.
 */
class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId = 0L
    private var formFilled = false
    private var originalPhotoPath: String? = null   // photo enregistree en base
    private var currentPhotoPath: String? = null    // photo affichee (peut etre non enregistree)
    private var pendingCameraPath: String? = null   // fichier en cours de capture
    private var finished = false                    // true apres enregistrement / suppression reussi(e)

    private val navigator: Navigator get() = requireActivity() as Navigator

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult          // selection annulee : rien a faire
            val appContext = requireContext().applicationContext
            lifecycleScope.launch {
                val path = withContext(Dispatchers.IO) { PhotoStorage.importFromUri(appContext, uri) }
                if (path == null) IntentActions.toast(requireContext(), R.string.err_photo_import) else setPhoto(path)
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = pendingCameraPath
            pendingCameraPath = null
            if (path == null) return@registerForActivityResult
            val appContext = requireContext().applicationContext
            lifecycleScope.launch {
                var ok = false
                if (success) {
                    ok = withContext(Dispatchers.IO) {
                        try { PhotoStorage.shrinkInPlace(File(path)); true } catch (e: Exception) { false }
                    }
                }
                if (ok) {
                    setPhoto(path)
                } else {
                    withContext(Dispatchers.IO) { PhotoStorage.deleteQuietly(appContext, path) }
                    if (success) IntentActions.toast(requireContext(), R.string.err_photo_import)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_ID)
        savedInstanceState?.let {
            formFilled = it.getBoolean(STATE_FILLED)
            originalPhotoPath = it.getString(STATE_ORIGINAL_PHOTO)
            currentPhotoPath = it.getString(STATE_CURRENT_PHOTO)
            pendingCameraPath = it.getString(STATE_PENDING_CAMERA)
            finished = it.getBoolean(STATE_FINISHED)
        }
        parentFragmentManager.setFragmentResultListener(
            ConfirmDeleteDialogFragment.REQUEST_KEY, this
        ) { _, _ ->
            viewModel.delete(contactId, listOf(originalPhotoPath, currentPhotoPath).distinct())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FILLED, formFilled)
        outState.putString(STATE_ORIGINAL_PHOTO, originalPhotoPath)
        outState.putString(STATE_CURRENT_PHOTO, currentPhotoPath)
        outState.putString(STATE_PENDING_CAMERA, pendingCameraPath)
        outState.putBoolean(STATE_FINISHED, finished)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailContactBinding.bind(view)

        applyPreferences()
        showPhoto()

        val creation = contactId == 0L
        binding.btnSupprimer.visibility = if (creation) View.GONE else View.VISIBLE
        binding.btnCarte.visibility = if (creation) View.GONE else View.VISIBLE

        binding.btnGalerie.setOnClickListener { pickImage.launch("image/*") }
        binding.btnCamera.setOnClickListener { launchCamera() }
        binding.btnAppeler.setOnClickListener {
            IntentActions.dial(requireContext(), binding.etTelephone.text?.toString())
        }
        binding.btnEmail.setOnClickListener {
            IntentActions.sendEmail(requireContext(), binding.etEmail.text?.toString())
        }
        binding.btnCarte.setOnClickListener { navigator.openCarte(contactId) }
        binding.btnEnregistrer.setOnClickListener { save() }
        binding.btnAnnuler.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSupprimer.setOnClickListener {
            val name = "${binding.etPrenom.text} ${binding.etNom.text}".trim()
            ConfirmDeleteDialogFragment.newInstance(name)
                .show(parentFragmentManager, "confirm_delete")
        }

        // Remplissage initial (une seule fois)
        if (creation) {
            formFilled = true
        } else if (!formFilled) {
            viewLifecycleOwner.lifecycleScope.launch {
                val contact = viewModel.getContact(contactId)
                if (contact == null) {
                    IntentActions.toast(requireContext(), R.string.err_contact_introuvable)
                    parentFragmentManager.popBackStack()
                } else {
                    fill(contact)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleEvent(it) }
            }
        }
    }

    private fun fill(c: Contact) {
        binding.etNom.setText(c.nom)
        binding.etPrenom.setText(c.prenom)
        binding.etTelephone.setText(c.telephone)
        binding.etEmail.setText(c.email.orEmpty())
        binding.etLatitude.setText(c.latitude?.toString().orEmpty())
        binding.etLongitude.setText(c.longitude?.toString().orEmpty())
        originalPhotoPath = c.photoUri
        currentPhotoPath = c.photoUri
        formFilled = true
        showPhoto()
    }

    /** Taille (nom, prenom, telephone) et couleur (nom, prenom) choisies dans les Parametres. */
    private fun applyPreferences() {
        val p = PreferencesManager(requireContext()).load()
        listOf(binding.etNom, binding.etPrenom, binding.etTelephone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, p.tailleSp)
        }
        binding.etNom.setTextColor(p.couleur)
        binding.etPrenom.setTextColor(p.couleur)
    }

    // ---------------- Enregistrement ----------------

    private fun save() {
        val r = ContactValidator.validate(
            nom = binding.etNom.text?.toString().orEmpty(),
            prenom = binding.etPrenom.text?.toString().orEmpty(),
            telephone = binding.etTelephone.text?.toString().orEmpty(),
            email = binding.etEmail.text?.toString().orEmpty(),
            latitudeText = binding.etLatitude.text?.toString().orEmpty(),
            longitudeText = binding.etLongitude.text?.toString().orEmpty()
        )
        showError(binding.tilNom, r.nomError)
        showError(binding.tilPrenom, r.prenomError)
        showError(binding.tilTelephone, r.telephoneError)
        showError(binding.tilEmail, r.emailError)
        showError(binding.tilLatitude, r.latitudeError)
        showError(binding.tilLongitude, r.longitudeError)

        if (!r.isValid) {
            IntentActions.toast(requireContext(), R.string.err_formulaire_invalide)
            return   // rien n'est ecrit en base
        }

        val contact = Contact(
            id = contactId,
            nom = binding.etNom.text.toString().trim(),
            prenom = binding.etPrenom.text.toString().trim(),
            telephone = binding.etTelephone.text.toString().trim(),
            email = binding.etEmail.text?.toString()?.trim()?.ifEmpty { null },
            photoUri = currentPhotoPath,
            latitude = r.latitude,
            longitude = r.longitude
        )
        val obsolete = if (originalPhotoPath != null && originalPhotoPath != currentPhotoPath)
            listOf(originalPhotoPath) else emptyList()

        binding.btnEnregistrer.isEnabled = false      // evite le double enregistrement
        viewModel.save(contact, obsolete)
    }

    private fun showError(layout: com.google.android.material.textfield.TextInputLayout, resId: Int?) {
        layout.error = resId?.let { getString(it) }
    }

    private fun handleEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.Saved -> {
                finished = true
                IntentActions.toast(
                    requireContext(),
                    if (event.created) R.string.msg_contact_ajoute else R.string.msg_contact_modifie
                )
                parentFragmentManager.popBackStack()
            }
            ContactEvent.Deleted -> {
                finished = true
                IntentActions.toast(requireContext(), R.string.msg_contact_supprime)
                parentFragmentManager.popBackStack()
            }
            is ContactEvent.Error -> {
                binding.btnEnregistrer.isEnabled = true
                IntentActions.toast(requireContext(), event.messageRes)
            }
        }
    }

    // ---------------- Photo ----------------

    private fun showPhoto() {
        val sizePx = resources.getDimensionPixelSize(R.dimen.photo_detail)
        ImageLoader.load(binding.ivPhoto, currentPhotoPath, sizePx, viewLifecycleOwner.lifecycleScope, R.drawable.ic_person)
    }

    /** Nouvelle photo choisie : l'eventuelle photo precedente NON enregistree est supprimee. */
    private fun setPhoto(path: String) {
        val previous = currentPhotoPath
        if (previous != null && previous != originalPhotoPath) {
            PhotoStorage.deleteQuietly(requireContext().applicationContext, previous)
        }
        currentPhotoPath = path
        if (_binding != null) showPhoto()
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val file = PhotoStorage.newPhotoFile(ctx)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        pendingCameraPath = file.absolutePath
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingCameraPath = null
            file.delete()
            IntentActions.toast(ctx, R.string.err_pas_app_camera)
        }
    }

    // ---------------- Cycle de vie ----------------

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(if (contactId == 0L) R.string.titre_nouveau_contact else R.string.titre_detail)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        // Fragment retire de la pile sans enregistrement : on supprime une photo non enregistree
        if (isRemoving && !finished && currentPhotoPath != originalPhotoPath) {
            PhotoStorage.deleteQuietly(requireContext().applicationContext, currentPhotoPath)
        }
        super.onDestroy()
    }

    companion object {
        private const val ARG_ID = "contact_id"
        private const val STATE_FILLED = "state_filled"
        private const val STATE_ORIGINAL_PHOTO = "state_original_photo"
        private const val STATE_CURRENT_PHOTO = "state_current_photo"
        private const val STATE_PENDING_CAMERA = "state_pending_camera"
        private const val STATE_FINISHED = "state_finished"

        fun newInstance(contactId: Long) = DetailContactFragment().apply {
            arguments = bundleOf(ARG_ID to contactId)
        }
    }
}
