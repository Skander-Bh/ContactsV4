package com.example.contacts.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.contacts.R

/** Intents implicites : toujours proteges contre l'absence d'application capable de les traiter. */
object IntentActions {

    /** ACTION_DIAL : ouvre le composeur, AUCUNE permission requise (contrairement a ACTION_CALL). */
    fun dial(context: Context, number: String?) {
        val n = number?.trim().orEmpty()
        if (n.isEmpty()) {
            toast(context, R.string.err_numero_vide); return
        }
        start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", n, null)), R.string.err_pas_app_telephone)
    }

    fun sendEmail(context: Context, email: String?) {
        val e = email?.trim().orEmpty()
        if (e.isEmpty()) {
            toast(context, R.string.err_email_vide); return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(e))
        }
        start(context, intent, R.string.err_pas_app_email)
    }

    /** Ouvre une page web dans le navigateur. */
    fun openUrl(context: Context, url: String) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), R.string.err_pas_navigateur)
    }

    private fun start(context: Context, intent: Intent, errorRes: Int) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, errorRes)
        }
    }

    fun toast(context: Context, resId: Int) =
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
}
