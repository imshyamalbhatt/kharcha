package com.shyamal.kharcha;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * Reads inbox SMS newer than a timestamp so the app can find bank / UPI debit messages.
 * Only messages mentioning an amount (Rs / INR / ₹) are returned; all parsing happens in
 * the app's JS so new bank formats can ship as online updates. Nothing leaves the phone.
 * JS: Capacitor.Plugins.SmsReader.read({ since: <ms>, limit }) -> { messages: [{id, address, body, date}] }
 */
@CapacitorPlugin(name = "SmsReader", permissions = { @Permission(alias = "sms", strings = { Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS }) })
public class SmsReader extends Plugin {
    private static SmsReader instance;

    @Override
    public void load() {
        instance = this;
    }

    /** Called by SmsReceiver when a bank debit SMS arrives; an open app rescans its inbox. */
    static void onIncoming() {
        if (instance != null) instance.notifyListeners("smsReceived", new JSObject());
    }

    /** Auto-detect on/off, saved where SmsReceiver can see it even when the app is closed. */
    @PluginMethod
    public void setAutoDetect(PluginCall call) {
        boolean on = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        getContext().getSharedPreferences(SmsReceiver.PREFS, Context.MODE_PRIVATE).edit().putBoolean(SmsReceiver.KEY_ON, on).apply();
        call.resolve();
    }

    @PluginMethod
    public void read(PluginCall call) {
        if (getPermissionState("sms") != PermissionState.GRANTED) {
            requestPermissionForAlias("sms", call, "afterPermission");
            return;
        }
        readInbox(call);
    }

    @PermissionCallback
    private void afterPermission(PluginCall call) {
        if (getPermissionState("sms") == PermissionState.GRANTED) readInbox(call);
        else call.reject("SMS permission denied", "DENIED");
    }

    private void readInbox(PluginCall call) {
        long since = call.getLong("since", 0L);
        int limit = call.getInt("limit", 3000);
        JSArray out = new JSArray();
        String[] cols = { "_id", "address", "body", "date" };
        try (Cursor c = getContext().getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI, cols, "date > ?", new String[] { String.valueOf(since) }, "date DESC")) {
            int n = 0;
            while (c != null && c.moveToNext() && n < limit) {
                String body = c.getString(2);
                if (body == null) continue;
                String low = body.toLowerCase();
                if (!(low.contains("rs") || low.contains("inr") || body.contains("₹") || low.contains("debit"))) continue;
                JSObject o = new JSObject();
                o.put("id", c.getLong(0));
                o.put("address", c.getString(1));
                o.put("body", body);
                o.put("date", c.getLong(3));
                out.put(o);
                n++;
            }
        } catch (Exception e) {
            call.reject("Could not read SMS: " + e.getMessage());
            return;
        }
        JSObject r = new JSObject();
        r.put("messages", out);
        call.resolve(r);
    }
}
