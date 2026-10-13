package com.shyamal.kharcha;

import android.os.Build;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * App lock: Android's own fingerprint / face prompt, falling back to the phone's PIN,
 * pattern or password. Nothing is stored by the app; Android does the checking.
 * JS: Capacitor.Plugins.AppLock.available() -> { available }
 *     Capacitor.Plugins.AppLock.authenticate({ title, subtitle }) -> resolves on success
 */
@CapacitorPlugin(name = "AppLock")
public class AppLock extends Plugin {

    private int authenticators() {
        // Android 11+: biometrics or device PIN in one prompt. Older: handled via setDeviceCredentialAllowed.
        return BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
    }

    @PluginMethod
    public void available(PluginCall call) {
        BiometricManager bm = BiometricManager.from(getContext());
        int res = Build.VERSION.SDK_INT >= 30
            ? bm.canAuthenticate(authenticators())
            : bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        boolean keyguard = ((android.app.KeyguardManager) getContext().getSystemService(android.content.Context.KEYGUARD_SERVICE)).isDeviceSecure();
        JSObject out = new JSObject();
        out.put("available", res == BiometricManager.BIOMETRIC_SUCCESS || keyguard);
        call.resolve(out);
    }

    @PluginMethod
    @SuppressWarnings("deprecation")
    public void authenticate(PluginCall call) {
        String title = call.getString("title", "Unlock Pocket Money");
        String subtitle = call.getString("subtitle", "Use your fingerprint, face or phone PIN");
        getActivity().runOnUiThread(() -> {
            FragmentActivity act = (FragmentActivity) getActivity();
            BiometricPrompt prompt = new BiometricPrompt(act, ContextCompat.getMainExecutor(act), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    call.resolve();
                }

                @Override
                public void onAuthenticationError(int code, CharSequence msg) {
                    call.reject(String.valueOf(msg), code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ? "CANCELLED" : "ERROR_" + code);
                }
                // onAuthenticationFailed (wrong finger) just lets the user try again inside the prompt
            });
            BiometricPrompt.PromptInfo.Builder b = new BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle);
            if (Build.VERSION.SDK_INT >= 30) b.setAllowedAuthenticators(authenticators());
            else b.setDeviceCredentialAllowed(true);
            try {
                prompt.authenticate(b.build());
            } catch (Exception e) {
                call.reject("Could not show the lock: " + e.getMessage(), "ERROR");
            }
        });
    }
}
