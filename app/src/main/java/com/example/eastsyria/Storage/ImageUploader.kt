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
import java.util.concurrent.Executors

/**
 * Uploads photos to the `images` bucket in Supabase Storage (Firebase Storage needs the Blaze plan).
 *
 * The request carries the signed-in user's Firebase ID token to the `upload` edge function, which
 * verifies it and stores the photo: `landmarks/...` for any signed-in user, `avatars/<uid>/...`
 * only in the caller's own folder.
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
    ) = upload(context, uri, "landmark", onSuccess, onError)

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
    ) = upload(context, uri, "avatar", onSuccess, onError)

    fun publicUrl(path: String): String =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET/$path"

    private fun upload(
        context: Context,
        uri: Uri,
        kind: String,
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
                val url = send(kind, bytes, token)
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

    /**
     * Sends the photo to the `upload` edge function, which checks the Firebase sign-in, stores the
     * file with the service role and answers with its public URL. (Storage itself would need
     * Firebase registered under Supabase Third-Party Auth to accept the token directly.)
     */
    private fun send(kind: String, bytes: ByteArray, token: String): String {
        val url = URL("${BuildConfig.SUPABASE_URL}/functions/v1/upload?kind=$kind")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("x-firebase-token", token)
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.outputStream.use { it.write(bytes) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw UploadException(code, body)
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return org.json.JSONObject(body).getString("url")
        } finally {
            conn.disconnect()
        }
    }

    class UploadException(val code: Int, body: String) :
        Exception("Upload failed ($code): ${body.take(200)}")
}
