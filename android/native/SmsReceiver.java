package com.shyamal.kharcha;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import androidx.core.app.NotificationCompat;

/**
 * Woken by Android for each incoming SMS (RECEIVE_SMS). If it looks like a bank / UPI debit
 * and auto-detect is on, it shows "₹250 spent · Tap to add" within seconds, and tells an
 * open app to refresh its "Found in your SMS" list. The message itself is not stored; the
 * app reads it from the inbox and parses it fully (parseSms in index.html).
 * The spend check itself lives in SmsRules.java.
 */
public class SmsReceiver extends BroadcastReceiver {
    static final String PREFS = "pm_sms";
    static final String KEY_ON = "notify";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        String from = parts[0].getDisplayOriginatingAddress();
        StringBuilder sb = new StringBuilder();
        for (SmsMessage p : parts) sb.append(p.getDisplayMessageBody());
        String body = sb.toString();
        String amount = SmsRules.spendAmount(from, body);
        if (amount == null) return;
        if (!ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, false)) return;
        SmsReader.onIncoming(); // app open: refresh the list in a few seconds
        showNotification(ctx, amount, (from + body).hashCode());
    }

    private void showNotification(Context ctx, String amount, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("sms_spends", "Spends found in SMS", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        int icon = ctx.getResources().getIdentifier("ic_stat_coin", "drawable", ctx.getPackageName());
        String rupees = amount.replaceAll("\\.00$", "");
        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, "sms_spends")
            .setSmallIcon(icon != 0 ? icon : android.R.drawable.ic_dialog_info)
            .setColor(0xFF0A8F5C)
            .setContentTitle("₹" + rupees + " spent · Tap to add")
            .setContentText("Pocket Money found it in your bank SMS.")
            .setContentIntent(pi)
            .setAutoCancel(true);
        try {
            nm.notify(id, nb.build());
        } catch (SecurityException e) {
            // notifications not allowed: the spend still shows up when the app opens
        }
    }
}
