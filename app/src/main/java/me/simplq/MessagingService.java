package me.simplq;

import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.SmsManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Handles push notifications from FCM.
 * Ref: https://github.com/firebase/quickstart-android/blob/master/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/java/MyFirebaseMessagingService.java
 */
public class MessagingService extends FirebaseMessagingService {
    private final SmsManager smsManager = SmsManager.getDefault();
    private static final String SMS_NUMBER_KEY = "SMS_NUMBER_KEY";
    private static final String SMS_PAYLOAD_KEY = "SMS_PAYLOAD";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Data message
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            sendSMS(data.get(SMS_NUMBER_KEY), data.get(SMS_PAYLOAD_KEY));
        }
    }

    @Override
    public void onNewToken(String token) {
        // TODO: Handle token refreshes
    }

    private void sendSMS(String phoneNumber, String message) {
        // TODO log/notify on failure
        smsManager.sendTextMessage(phoneNumber, null, message, PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0), null);
    }
}