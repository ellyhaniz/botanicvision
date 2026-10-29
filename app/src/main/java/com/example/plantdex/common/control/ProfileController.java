package com.example.plantdex.common.control;

import android.content.Context;

import com.example.plantdex.common.ApiClient;
import com.example.plantdex.common.Session;

/** Business logic for a signed-in user managing their own account (as opposed to an admin managing someone else's). */
public final class ProfileController {

    private ProfileController() {}

    /** Edits full name/email and keeps the local session in sync on success. */
    public static void updateMyProfile(Context context, int userId, String fullName, String email,
                                        AuthController.SimpleCallback callback) {
        ApiClient.updateMyProfile(userId, fullName, email, new ApiClient.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                Session.updateProfile(context, fullName, email);
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Self-service "forgot password" — no login required, identified by email + username. */
    public static void resetPasswordSelf(String email, String username, String newPassword,
                                          AuthController.SimpleCallback callback) {
        ApiClient.resetPasswordSelf(email, username, newPassword, new ApiClient.Callback() {
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
}
