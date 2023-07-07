package android.jonas.fakestandby;

import android.telephony.SmsManager;

public class SendSMS {

    public static void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            System.out.println("SMS sent successfully!");
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
}
