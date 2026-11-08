package com.example.eastsyria.Social

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlin.math.roundToInt

/*
 * People nearby and chat, on the Firebase Realtime Database.
 *
 *   nearby/{uid}            name, photo, lat, lng (rounded to ~1 km), at   – only while the app is open
 *   chats/{a_b}/messages    from, text, at                                 – a_b = the two uids, sorted
 *   userChats/{uid}/{a_b}   with, name, photo, lastText, lastFrom, at, unread
 *   blocks/{uid}/{other}    true
 *
 * Nobody appears to others until they turn on "Show me to people nearby", and they disappear as
 * soon as the app goes to the background. Locations are rounded before they leave the phone, and
 * other people only ever see a distance like "~3 km", never a point on a map.
 */

data class NearbyPerson(val uid: String, val name: String, val photo: String, val km: Double, val at: Long)

data class ChatSummary(
    val chatId: String,
    val with: String,
    val name: String,
    val photo: String,
    val lastText: String,
    val lastFromMe: Boolean,
    val at: Long,
    val unread: Int,
)

data class Message(val id: String, val from: String, val text: String, val at: Long)

object Social {
    const val RADIUS_KM = 50.0
    private const val ACTIVE_MS = 15L * 60 * 1000
    private const val HEARTBEAT_MS = 3L * 60 * 1000
    private const val PREFS = "user_prefs"
    private const val KEY_VISIBLE = "nearby_visible"

    private val db get() = FirebaseDatabase.getInstance().reference
    private val me get() = FirebaseAuth.getInstance().currentUser?.uid

    /** Server clock minus phone clock, so a phone with a wrong clock still sees who is active. */
    @Volatile internal var serverOffset = 0L

    fun chatId(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"

    // ------------------------------------------------------------------ visibility

    fun isVisible(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_VISIBLE, false)

    fun setVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_VISIBLE, visible).apply()
        if (visible) Presence.publish(context) else Presence.withdraw()
    }

    fun hasLocationPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // ------------------------------------------------------------------ nearby

    /**
     * People within [RADIUS_KM] who were active in the last 15 minutes, nearest first, plus how many
     * active people are farther away (a count only, nobody's name or distance).
     */
    fun listenNearby(here: Location, onChange: (List<NearbyPerson>, Int) -> Unit): () -> Unit {
        val myId = me ?: return {}
        val ref = db.child("nearby")
        var blocked = emptySet<String>()
        var last: DataSnapshot? = null

        fun emit() {
            val snap = last ?: return
            val now = System.currentTimeMillis() + serverOffset
            var farther = 0
            val people = snap.children.mapNotNull { c ->
                val uid = c.key ?: return@mapNotNull null
                if (uid == myId || uid in blocked) return@mapNotNull null
                val lat = c.child("lat").getValue(Double::class.java) ?: return@mapNotNull null
                val lng = c.child("lng").getValue(Double::class.java) ?: return@mapNotNull null
                val at = c.child("at").getValue(Long::class.java) ?: 0L
                if (now - at > ACTIVE_MS) return@mapNotNull null
                val d = FloatArray(1)
                Location.distanceBetween(here.latitude, here.longitude, lat, lng, d)
                val km = d[0] / 1000.0
                if (km > RADIUS_KM) { farther++; return@mapNotNull null }
                NearbyPerson(uid, c.child("name").getValue(String::class.java).orEmpty(),
                    c.child("photo").getValue(String::class.java).orEmpty(), km, at)
            }.sortedBy { it.km }
            onChange(people, farther)
        }

        val nearbyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { last = snapshot; emit() }
            override fun onCancelled(error: DatabaseError) = onChange(emptyList(), 0)
        }
        val blocksRef = db.child("blocks").child(myId)
        val blocksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                blocked = snapshot.children.mapNotNull { it.key }.toSet(); emit()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(nearbyListener)
        blocksRef.addValueEventListener(blocksListener)
        return { ref.removeEventListener(nearbyListener); blocksRef.removeEventListener(blocksListener) }
    }

    // ------------------------------------------------------------------ chats

    fun listenChats(onChange: (List<ChatSummary>) -> Unit): () -> Unit {
        val myId = me ?: return {}
        val ref = db.child("userChats").child(myId)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.children.mapNotNull { c ->
                    ChatSummary(
                        chatId = c.key ?: return@mapNotNull null,
                        with = c.child("with").getValue(String::class.java).orEmpty(),
                        name = c.child("name").getValue(String::class.java).orEmpty(),
                        photo = c.child("photo").getValue(String::class.java).orEmpty(),
                        lastText = c.child("lastText").getValue(String::class.java).orEmpty(),
                        lastFromMe = c.child("lastFrom").getValue(String::class.java) == myId,
                        at = c.child("at").getValue(Long::class.java) ?: 0L,
                        unread = c.child("unread").getValue(Int::class.java) ?: 0,
                    )
                }.sortedByDescending { it.at })
            }
            override fun onCancelled(error: DatabaseError) = onChange(emptyList())
        }
        ref.addValueEventListener(l)
        return { ref.removeEventListener(l) }
    }

    fun listenMessages(chatId: String, onChange: (List<Message>) -> Unit): () -> Unit {
        val ref = db.child("chats").child(chatId).child("messages").orderByChild("at").limitToLast(200)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.children.mapNotNull { c ->
                    Message(c.key ?: return@mapNotNull null,
                        c.child("from").getValue(String::class.java).orEmpty(),
                        c.child("text").getValue(String::class.java).orEmpty(),
                        c.child("at").getValue(Long::class.java) ?: 0L)
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        return { ref.removeEventListener(l) }
    }

    /**
     * Sends a message and updates both people's chat lists in one atomic write. If the other person
     * blocked you, the database rules refuse the whole write and [onDone] gets false.
     */
    fun send(otherUid: String, otherName: String, otherPhoto: String, text: String, onDone: (Boolean) -> Unit) {
        val myId = me ?: return onDone(false)
        val clean = text.trim().take(1000)
        if (clean.isEmpty()) return onDone(false)
        Profile.mine { myName, myPhoto ->
            val id = chatId(myId, otherUid)
            val key = db.child("chats").child(id).child("messages").push().key ?: return@mine onDone(false)
            val now = ServerValue.TIMESTAMP
            val updates = hashMapOf<String, Any>(
                "chats/$id/messages/$key" to mapOf("from" to myId, "text" to clean, "at" to now),
                "userChats/$myId/$id/with" to otherUid,
                "userChats/$myId/$id/name" to otherName,
                "userChats/$myId/$id/photo" to otherPhoto,
                "userChats/$myId/$id/lastText" to clean,
                "userChats/$myId/$id/lastFrom" to myId,
                "userChats/$myId/$id/at" to now,
                "userChats/$myId/$id/unread" to 0,
                "userChats/$otherUid/$id/with" to myId,
                "userChats/$otherUid/$id/name" to myName,
                "userChats/$otherUid/$id/photo" to myPhoto,
                "userChats/$otherUid/$id/lastText" to clean,
                "userChats/$otherUid/$id/lastFrom" to myId,
                "userChats/$otherUid/$id/at" to now,
                "userChats/$otherUid/$id/unread" to ServerValue.increment(1),
            )
            db.updateChildren(updates).addOnCompleteListener { onDone(it.isSuccessful) }
        }
    }

    fun markRead(chatId: String) {
        val myId = me ?: return
        db.child("userChats").child(myId).child(chatId).child("unread").setValue(0)
    }

    fun setBlocked(otherUid: String, blocked: Boolean, onDone: () -> Unit = {}) {
        val myId = me ?: return
        val ref = db.child("blocks").child(myId).child(otherUid)
        (if (blocked) ref.setValue(true) else ref.removeValue()).addOnCompleteListener { onDone() }
    }

    fun listenBlocked(otherUid: String, onChange: (Boolean) -> Unit): () -> Unit {
        val myId = me ?: return {}
        val ref = db.child("blocks").child(myId).child(otherUid)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onChange(snapshot.exists())
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        return { ref.removeEventListener(l) }
    }

    fun listenUnreadTotal(onChange: (Int) -> Unit): () -> Unit =
        listenChats { chats -> onChange(chats.sumOf { it.unread }) }
}

/** The signed-in user's own name and photo, read once and remembered. */
object Profile {
    private var cached: Pair<String, String>? = null

    fun mine(onReady: (String, String) -> Unit) {
        cached?.let { return onReady(it.first, it.second) }
        val user = FirebaseAuth.getInstance().currentUser ?: return onReady("", "")
        FirebaseDatabase.getInstance().reference.child("users").child(user.uid).get()
            .addOnSuccessListener { s ->
                val name = s.child("fullName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: user.displayName.orEmpty()
                val photo = s.child("imgUrl").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: user.photoUrl?.toString().orEmpty()
                cached = name to photo
                onReady(name, photo)
            }
            .addOnFailureListener { onReady(user.displayName.orEmpty(), user.photoUrl?.toString().orEmpty()) }
    }

    fun forget() { cached = null }
}

/**
 * Keeps nearby/{uid} up to date while the app is in the foreground (and the user chose to be
 * visible), and removes it when the app goes to the background or the connection drops.
 */
object Presence : DefaultLifecycleObserver {
    private lateinit var app: Context
    private val handler = Handler(Looper.getMainLooper())
    private var ref: DatabaseReference? = null
    private var lastFix: Location? = null
    private var foreground = false
    private val connected get() = FirebaseDatabase.getInstance().reference.child(".info/connected")
    private val offset get() = FirebaseDatabase.getInstance().reference.child(".info/serverTimeOffset")

    // The server removes nearby/{uid} whenever the connection drops, even for a second (a network
    // switch, the phone dozing). Write it again as soon as we are back online.
    private val onConnection = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (foreground && snapshot.getValue(Boolean::class.java) == true) publish(app, lastFix)
        }
        override fun onCancelled(error: DatabaseError) {}
    }
    private val onOffset = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Social.serverOffset = snapshot.getValue(Long::class.java) ?: 0L
        }
        override fun onCancelled(error: DatabaseError) {}
    }
    private val beat = object : Runnable {
        override fun run() { publish(app); handler.postDelayed(this, 3L * 60 * 1000) }
    }

    fun install(context: Context) {
        app = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        offset.addValueEventListener(onOffset)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        handler.removeCallbacks(beat)
        handler.post(beat)
        connected.addValueEventListener(onConnection)
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        handler.removeCallbacks(beat)
        connected.removeEventListener(onConnection)
        withdraw()
    }

    /** The phone's approximate position, or null without permission or a fix. */
    @SuppressLint("MissingPermission")
    fun locate(context: Context, onResult: (Location?) -> Unit) {
        if (!Social.hasLocationPermission(context)) return onResult(null)
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = com.google.android.gms.tasks.CancellationTokenSource()
        var done = false
        fun finish(loc: Location?) {
            if (done) return
            if (loc != null) { done = true; return onResult(loc) }
            // No fresh fix (indoors, emulator): the last known position is close enough for ~1 km.
            done = true
            try {
                client.lastLocation
                    .addOnSuccessListener { onResult(it) }
                    .addOnFailureListener { onResult(null) }
            } catch (_: SecurityException) { onResult(null) }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ if (!done) { cancel.cancel(); finish(null) } }, 8_000)
        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token)
                .addOnSuccessListener { finish(it) }
                .addOnFailureListener { finish(null) }
        } catch (_: SecurityException) { finish(null) }
    }

    /** Writes nearby/{uid}. [known] skips a second location request when the caller has a fix. */
    fun publish(context: Context, known: Location? = null) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        if (!::app.isInitialized) app = context.applicationContext
        if (!Social.isVisible(context)) return
        if (known != null) return write(known)
        locate(context) { fresh -> (fresh ?: lastFix)?.let { write(it) } }
    }

    private fun write(loc: Location) {
        lastFix = loc
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Profile.mine { name, photo ->
            val r = FirebaseDatabase.getInstance().reference.child("nearby").child(uid)
            ref = r
            r.onDisconnect().removeValue()
            // Rounded to two decimals (about 1 km) before it leaves the phone.
            r.setValue(mapOf(
                "name" to name,
                "photo" to photo,
                "lat" to (loc.latitude * 100).roundToInt() / 100.0,
                "lng" to (loc.longitude * 100).roundToInt() / 100.0,
                "at" to ServerValue.TIMESTAMP,
            ))
        }
    }

    fun withdraw() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        (ref ?: FirebaseDatabase.getInstance().reference.child("nearby").child(uid)).removeValue()
    }
}
