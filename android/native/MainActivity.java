package com.shyamal.kharcha;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ApkUpdater.class);    // in-app updates
        registerPlugin(SmsReader.class);     // bank / UPI SMS auto-capture (2.4: retried via the in-app installer)
        registerPlugin(ContactPicker.class); // link a friend's WhatsApp for udhaar nudges
        super.onCreate(savedInstanceState);
    }
}
