package com.webtech.kamuskorea.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {

    companion object {
        private const val TAG = "NotificationManager"
        private const val TOPIC_ALL_USERS = "all_users"
    }

    /**
     * Subscribe user to "all_users" topic untuk menerima broadcast notifications
     */
    fun subscribeToAllUsersNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL_USERS)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Successfully subscribed to $TOPIC_ALL_USERS topic")
                } else {
                    Log.e(TAG, "❌ Failed to subscribe to $TOPIC_ALL_USERS topic", task.exception)
                }
            }
    }

    /**
     * Unsubscribe dari topic (optional, jika user ingin disable notifications)
     */
    fun unsubscribeFromAllUsersNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC_ALL_USERS)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Successfully unsubscribed from $TOPIC_ALL_USERS topic")
                } else {
                    Log.e(TAG, "❌ Failed to unsubscribe from $TOPIC_ALL_USERS topic", task.exception)
                }
            }
    }

    /**
     * Get FCM token (untuk debugging atau kirim ke server)
     */
    fun getToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val token = task.result
                Log.d(TAG, "🔑 FCM Token: $token")
                onTokenReceived(token)
            } else {
                Log.e(TAG, "❌ Failed to get FCM token", task.exception)
            }
        }
    }
}
