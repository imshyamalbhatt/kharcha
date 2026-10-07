package com.shyamal.kharcha;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Opens the phone's own contact picker and returns the one contact the user chose
 * (name + number). Uses Android's picker, so no contacts permission is needed and
 * the app never sees the rest of the address book.
 * JS: Capacitor.Plugins.ContactPicker.pick() -> { name, phone }
 */
@CapacitorPlugin(name = "ContactPicker")
public class ContactPicker extends Plugin {

    @PluginMethod
    public void pick(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(call, i, "picked");
    }

    @ActivityCallback
    private void picked(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("cancelled", "CANCELLED");
            return;
        }
        Uri uri = result.getData().getData();
        String[] cols = { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        try (Cursor c = getContext().getContentResolver().query(uri, cols, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                JSObject r = new JSObject();
                r.put("name", c.getString(0));
                r.put("phone", c.getString(1));
                call.resolve(r);
                return;
            }
        } catch (Exception e) {
            call.reject("Could not read contact: " + e.getMessage());
            return;
        }
        call.reject("No contact found");
    }
}
