package com.example.contacts.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contacts.R
import com.example.contacts.databinding.FragmentListeContactsBinding
import com.example.contacts.prefs.PreferencesManager
import com.example.contacts.util.IntentActions
import kotlinx.coroutines.launch

/** Liste des contacts : filtre, bouton "+", bouton Appeler par ligne (selon la preference). */
class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter

    private val navigator: Navigator get() = requireActivity() as Navigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListeContactsBinding.bind(view)

        adapter = ContactAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            onContactClick = { navigator.openDetail(it.id) },
            onCallClick = { IntentActions.dial(requireContext(), it.telephone) }
        )
        adapter.updatePrefs(PreferencesManager(requireContext()).load())

        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListeContactsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        binding.fabAjouter.setOnClickListener { navigator.openDetail(0L) }

        binding.etFiltre.setText(viewModel.currentQuery)
        binding.etFiltre.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.contacts.collect { list ->
                        if (list == null) return@collect          // premier chargement en cours
                        adapter.submitList(list)
                        binding.tvVide.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvVide.setText(
                            if (viewModel.currentQuery.isBlank()) R.string.liste_vide else R.string.liste_aucun_resultat
                        )
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        if (event is ContactEvent.Error) IntentActions.toast(requireContext(), event.messageRes)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()   // la liste est relue en base a chaque affichage
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.app_name)
        // Preferences modifiees dans l'ecran Parametres : appliquees des le retour sur la liste
        adapter.updatePrefs(PreferencesManager(requireContext()).load())
    }

    override fun onDestroyView() {
        binding.recyclerContacts.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
