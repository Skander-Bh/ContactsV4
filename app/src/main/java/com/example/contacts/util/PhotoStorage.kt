package com.example.contacts.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

/**
 * Gestion des photos : chaque image choisie (galerie ou camera) est COPIEE dans le stockage interne
 * (filesDir/photos), redimensionnee, puis son chemin absolu est enregistre en base.
 * Une URI de galerie (content://) peut devenir illisible apres redemarrage ; un fichier prive reste valide.
 *
 * Toutes les fonctions font de l'I/O : a appeler sur Dispatchers.IO.
 */
object PhotoStorage {
    private const val DIR = "photos"
    private const val MAX_SIDE = 1024

    fun photosDir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    fun newPhotoFile(context: Context): File {
        val dir = photosDir(context)
        var f: File
        do {
            f = File(dir, "photo_${System.currentTimeMillis()}_${(0..99999).random()}.jpg")
        } while (f.exists())
        f.createNewFile()
        return f
    }

    /** Copie l'image designee par [uri] dans filesDir. @return le chemin absolu, ou null en cas d'echec. */
    fun importFromUri(context: Context, uri: Uri): String? {
        val dest = newPhotoFile(context)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("flux nul")
            input.use { src -> dest.outputStream().use { src.copyTo(it) } }
            shrinkInPlace(dest)
            dest.absolutePath
        } catch (e: Exception) {
            dest.delete()
            null
        }
    }

    /** Redimensionne (cote max 1024 px), applique l'orientation EXIF puis recompresse en JPEG. */
    @Throws(IOException::class)
    fun shrinkInPlace(file: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Image illisible")

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE)
        }
        var bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: throw IOException("Decodage impossible")

        val orientation = try {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (!matrix.isIdentity) {
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bmp.recycle()
    }

    /** Decode une miniature pour l'affichage (sans charger l'image pleine taille). */
    fun decodeSampled(path: String, targetPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx)
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    /** Supprime la photo, uniquement si elle se trouve bien dans filesDir/photos. */
    fun deleteQuietly(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val f = File(path)
            if (f.parentFile?.canonicalPath == photosDir(context).canonicalPath) f.delete()
        } catch (e: Exception) {
            // rien a faire : un fichier orphelin n'est pas critique
        }
    }

    private fun sampleSize(w: Int, h: Int, target: Int): Int {
        var s = 1
        while (w / (s * 2) >= target && h / (s * 2) >= target) s *= 2
        return s
    }
}
