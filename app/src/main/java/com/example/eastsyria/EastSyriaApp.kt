package com.example.eastsyria

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * Why the home screen used to sit on skeletons after every launch: each cold
 * start fetched the whole catalog over the network before painting anything.
 *
 * With disk persistence on, the catalog (and the user's saved list) is kept on
 * the device and listeners fire from that copy straight away; the network
 * result then arrives and updates it in place. `keepSynced` keeps the catalog
 * warm even while no screen is listening to it.
 *
 * Persistence must be switched on before the database is used anywhere, so it
 * lives here rather than in an Activity.
 */
class EastSyriaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Shows you to people nearby while the app is open (only if you turned it on).
        com.example.eastsyria.Social.Presence.install(this)
        try {
            val database = FirebaseDatabase.getInstance()
            database.setPersistenceEnabled(true)
            database.reference.child("landmarks").keepSynced(true)
        } catch (e: Exception) {
            // Already configured (for example after a process restart within the
            // same run): the catalog still works, just without the head start.
            Log.w("EastSyriaApp", "Database persistence not enabled: ${e.message}")
        }
    }
}
