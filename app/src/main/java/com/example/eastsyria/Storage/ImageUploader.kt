package com.example.eastsyria.Storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.eastsyria.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Uploads photos to the `images` bucket in Supabase Storage (Firebase Storage needs the Blaze plan).
 *
 * The request carries the signed-in user's Firebase ID token. The Supabase project trusts the
 * `east-syria` Firebase project (Third-Party Auth), and the bucket's policies only accept writes
 * from those tokens: `landmarks/...` for any signed-in user, `avatars/<uid>/...` for that user only.
 */
object ImageUploader {

    private const val BUCKET = "images"
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 84

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** A new landmark photo; returns its public URL. */
    fun uploadLandmarkPhoto(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) = upload(context, uri, "landmarks/${UUID.randomUUID()}.jpg", upsert = false, onSuccess, onError)

    /**
     * The user's profile picture. Each upload gets a new file name so Glide and other phones
     * never show a cached old picture under the same URL.
     */
    fun uploadAvatar(
        context: Context,
        uid: String,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) = upload(context, uri, "avatars/$uid/${System.currentTimeMillis()}.jpg", upsert = true, onSuccess, onError)

    fun publicUrl(path: String): String =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET/$path"

    private fun upload(
        context: Context,
        uri: Uri,
        path: String,
        upsert: Boolean,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val appContext = context.applicationContext
        io.execute {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("Sign in to upload photos")
                val token = Tasks.await(user.getIdToken(false)).token
                    ?: throw IllegalStateException("No Firebase ID token")
                val bytes = compress(appContext, uri)
                put(path, bytes, token, upsert)
                val url = publicUrl(path)
                main.post { onSuccess(url) }
            } catch (e: Exception) {
                main.post { onError(e) }
            }
        }
    }

    /** Decodes, shrinks the long edge to [MAX_EDGE] and re-encodes as JPEG (also drops EXIF/GPS). */
    private fun compress(context: Context, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val longEdge = maxOf(w, h)
            if (longEdge > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longEdge
                decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            }
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun put(path: String, bytes: ByteArray, token: String, upsert: Boolean) {
        val url = URL("${BuildConfig.SUPABASE_URL}/storage/v1/object/$BUCKET/$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.setRequestProperty("Cache-Control", "max-age=31536000")
            conn.setRequestProperty("x-upsert", upsert.toString())
            conn.outputStream.use { it.write(bytes) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw UploadException(code, body)
            }
        } finally {
            conn.disconnect()
        }
    }

    class UploadException(val code: Int, body: String) :
        Exception("Upload failed ($code): ${body.take(200)}")
}
