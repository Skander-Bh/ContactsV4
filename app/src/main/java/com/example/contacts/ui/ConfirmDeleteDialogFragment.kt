package com.example.contacts.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.contacts.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Confirmation de suppression. Implemente en DialogFragment pour survivre a une rotation
 * (un AlertDialog simple disparaitrait a la recreation de l'activite).
 */
class ConfirmDeleteDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val name = requireArguments().getString(ARG_NAME).orEmpty()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_suppression_titre)
            .setMessage(getString(R.string.dialog_suppression_message, name))
            .setPositiveButton(R.string.action_supprimer) { _, _ ->
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf())
            }
            .setNegativeButton(R.string.action_annuler, null)
            .create()
    }

    companion object {
        const val REQUEST_KEY = "confirm_delete_request"
        private const val ARG_NAME = "name"

        fun newInstance(contactName: String) = ConfirmDeleteDialogFragment().apply {
            arguments = bundleOf(ARG_NAME to contactName)
        }
    }
}
