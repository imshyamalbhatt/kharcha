package com.shyamal.kharcha;

import android.app.PendingIntent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import java.util.Arrays;

/**
 * Gets a short-lived access token for the user's own Google Drive *app folder* (hidden,
 * only this app can see it) using Google's AuthorizationClient. No other Drive files, no
 * Gmail, no contacts. The backup upload/download itself happens in index.html (Drive REST).
 * Needs an Android OAuth client in Google Cloud for this package + signing SHA-1.
 * JS: Capacitor.Plugins.GoogleDrive.authorize({ interactive }) -> { accessToken, email }
 */
@CapacitorPlugin(name = "GoogleDrive")
public class GoogleDrive extends Plugin {
    private static final Scope APP_FOLDER = new Scope("https://www.googleapis.com/auth/drive.appdata");
    private static final Scope EMAIL = new Scope("email");
    private ActivityResultLauncher<IntentSenderRequest> consent;
    private PluginCall waiting;

    @Override
    public void load() {
        consent = getActivity().registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), res -> {
            PluginCall call = waiting;
            waiting = null;
            if (call == null) return;
            try {
                AuthorizationResult r = Identity.getAuthorizationClient(getActivity()).getAuthorizationResultFromIntent(res.getData());
                resolve(call, r);
            } catch (ApiException e) {
                call.reject("Google sign-in was cancelled or failed (" + e.getStatusCode() + ")", "CANCELLED");
            } catch (Exception e) {
                call.reject("Google sign-in failed: " + e.getMessage(), "FAILED");
            }
        });
    }

    @PluginMethod
    public void authorize(PluginCall call) {
        boolean interactive = Boolean.TRUE.equals(call.getBoolean("interactive", true));
        AuthorizationRequest req = AuthorizationRequest.builder().setRequestedScopes(Arrays.asList(APP_FOLDER, EMAIL)).build();
        Identity.getAuthorizationClient(getActivity()).authorize(req)
            .addOnSuccessListener(result -> {
                if (!result.hasResolution()) { resolve(call, result); return; }
                if (!interactive) { call.reject("Google needs you to sign in again", "NEEDS_CONSENT"); return; }
                PendingIntent pi = result.getPendingIntent();
                if (pi == null) { call.reject("Google sign-in could not start", "FAILED"); return; }
                waiting = call;
                consent.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            })
            .addOnFailureListener(e -> {
                String code = (e instanceof ApiException) ? String.valueOf(((ApiException) e).getStatusCode()) : "";
                // status 10 (DEVELOPER_ERROR) = the Google Cloud setup (Android client / SHA-1) is missing or wrong
                call.reject("Google sign-in failed " + code + ": " + e.getMessage(), "10".equals(code) ? "NOT_SET_UP" : "FAILED");
            });
    }

    private void resolve(PluginCall call, AuthorizationResult r) {
        JSObject out = new JSObject();
        out.put("accessToken", r.getAccessToken());
        GoogleSignInAccount acc = r.toGoogleSignInAccount();
        out.put("email", acc != null ? acc.getEmail() : null);
        call.resolve(out);
    }
}
