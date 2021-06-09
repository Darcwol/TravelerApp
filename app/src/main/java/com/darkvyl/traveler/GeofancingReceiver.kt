package com.darkvyl.traveler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

const val NOTIFICATION_UNIQUE_ID = 1

class GeofencingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            println(errorMessage)
        }

        val geofencingTransition = geofencingEvent.geofenceTransition
        if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val geofence = geofencingEvent.triggeringGeofences.first()

            val intentToOpenDetails = Intent(context, PlaceDetailsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("placeId", geofence.requestId.toLong())
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intentToOpenDetails, 0)

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_DEFAULT)
                .setContentTitle("You visited this place before!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_UNIQUE_ID, notification)
        }
    }
}