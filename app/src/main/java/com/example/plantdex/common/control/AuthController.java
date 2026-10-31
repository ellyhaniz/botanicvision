package com.example.plantdex.common.control;

import android.content.Context;

import com.example.plantdex.common.ApiClient;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.entity.User;

/**
 * Business logic for signing in, registering, and signing out. Boundary
 * classes (the Activities) call into this instead of talking to ApiClient
 * or Session directly.
 */
public final class AuthController {

    private AuthController() {}

    // Login hands back the account it just authenticated as (the screen needs
    // it to route to the right dashboard); everything else here either
    // succeeds or fails with nothing more to report, hence SimpleCallback.
    public interface LoginCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    /** Authenticates the account and, on success, starts its session. */
    public static void login(Context context, String username, String password, LoginCallback callback) {
        ApiClient.login(username, password, new ApiClient.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                User user = User.fromJson(response);
                Session.save(context, user);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Public self-registration — always creates a Visitor account (enforced server-side too). */
    public static void register(String email, String fullName, String username, String password,
                                 SimpleCallback callback) {
        ApiClient.register(email, fullName, username, password, new ApiClient.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Records the sign-out in the audit log, then clears the local session either way. */
    public static void logout(Context context, int userId, SimpleCallback callback) {
        ApiClient.logout(userId, new ApiClient.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                Session.clear(context);
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                // Still log the user out locally even if the audit-log call failed —
                // don't strand them on a screen they can't get back to.
                Session.clear(context);
                callback.onSuccess();
            }
        });
    }
}
