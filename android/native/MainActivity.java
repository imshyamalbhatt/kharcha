package com.shyamal.kharcha;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ApkUpdater.class);    // in-app updates
        // registerPlugin(SmsReader.class); // SMS auto-capture: off in 2.3 (2.2 with SMS failed to install, likely Play Protect); see AndroidManifest.sms.xml
        registerPlugin(ContactPicker.class); // link a friend's WhatsApp for udhaar nudges
        super.onCreate(savedInstanceState);
    }
}
