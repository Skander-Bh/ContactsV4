package com.example.contacts.util

import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.DrawableRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mini chargeur d'images : decodage hors thread principal, miniatures en cache memoire.
 * Si le fichier est absent ou illisible, le placeholder est affiche (aucun plantage).
 */
object ImageLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun load(
        view: ImageView,
        path: String?,
        targetPx: Int,
        scope: CoroutineScope,
        @DrawableRes placeholder: Int
    ) {
        view.tag = path
        if (path.isNullOrBlank()) {
            view.setImageResource(placeholder); return
        }
        val key = "$path@$targetPx"
        cache.get(key)?.let { view.setImageBitmap(it); return }

        view.setImageResource(placeholder)
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { PhotoStorage.decodeSampled(path, targetPx) }
            if (bmp != null) {
                cache.put(key, bmp)
                if (view.tag == path) view.setImageBitmap(bmp)   // la vue a pu etre recyclee entre-temps
            }
        }
    }
}
