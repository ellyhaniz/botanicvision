package com.example.plantdex.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.plantdex.common.entity.User;

/**
 * Remembers who's currently signed in, so screens don't have to pass the
 * account around via Intent extras everywhere. Backed by plain
 * SharedPreferences — there's nothing sensitive enough in here (no password,
 * no token) to justify EncryptedSharedPreferences for a student project.
 * Cleared on logout; overwritten wholesale on every fresh login.
 */
public final class Session {

    private static final String PREFS = "plantdex_session";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_PROFILE_TYPE_ID = "profileTypeId";
    private static final String KEY_PROFILE_TYPE_NAME = "profileTypeName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ACCOUNT_STATUS = "accountStatus";
    private static final String KEY_MEMBER_SINCE = "memberSince";

    private Session() {}

    /** Called once, right after a successful login — stores everything the rest of the app reads back below. */
    public static void save(Context context, User user) {
        prefs(context).edit()
                .putInt(KEY_USER_ID, user.getUserId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_ROLE, user.getRole())
                .putInt(KEY_PROFILE_TYPE_ID, user.getProfileTypeId())
                .putString(KEY_PROFILE_TYPE_NAME, user.getProfileTypeName())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_ACCOUNT_STATUS, user.getAccountStatus())
                .putString(KEY_MEMBER_SINCE, user.getMemberSince())
                .apply();
    }

    public static int getUserId(Context context) {
        return prefs(context).getInt(KEY_USER_ID, 0);
    }

    public static String getUsername(Context context) {
        return prefs(context).getString(KEY_USERNAME, null);
    }

    public static String getRole(Context context) {
        return prefs(context).getString(KEY_ROLE, null);
    }

    public static int getProfileTypeId(Context context) {
        return prefs(context).getInt(KEY_PROFILE_TYPE_ID, 0);
    }

    public static String getProfileTypeName(Context context) {
        return prefs(context).getString(KEY_PROFILE_TYPE_NAME, null);
    }

    public static String getEmail(Context context) {
        return prefs(context).getString(KEY_EMAIL, null);
    }

    public static String getFullName(Context context) {
        return prefs(context).getString(KEY_FULL_NAME, null);
    }

    public static String getAccountStatus(Context context) {
        return prefs(context).getString(KEY_ACCOUNT_STATUS, null);
    }

    public static String getMemberSince(Context context) {
        return prefs(context).getString(KEY_MEMBER_SINCE, null);
    }

    /** Updates the cached full name/email after a successful self-service profile edit. */
    public static void updateProfile(Context context, String fullName, String email) {
        prefs(context).edit()
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
