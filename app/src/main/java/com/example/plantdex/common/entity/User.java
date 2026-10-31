package com.example.plantdex.common.entity;

import org.json.JSONObject;

/**
 * The signed-in account's data, as returned by the login endpoint. Immutable
 * on purpose — once you're logged in this doesn't change under you; a fresh
 * login (or Session.updateProfile after an edit) is what updates it.
 */
public class User {

    private final int userId;
    private final String username;

    // `role` is the fixed internal code (e.g. "USER ADMIN") the app's
    // navigation switches on — it never changes even if a User Admin renames
    // the profile type. `profileTypeName` is that editable display name,
    // only used for showing the user something readable on screen.
    private final String role;
    private final int profileTypeId;
    private final String profileTypeName;

    private final String email;
    private final String fullName;
    private final String accountStatus;
    private final String memberSince;

    public User(int userId, String username, String role, int profileTypeId, String profileTypeName,
                String email, String fullName, String accountStatus, String memberSince) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.profileTypeId = profileTypeId;
        this.profileTypeName = profileTypeName;
        this.email = email;
        this.fullName = fullName;
        this.accountStatus = accountStatus;
        this.memberSince = memberSince;
    }

    /** Builds a User straight from the login response's JSON body. Missing fields fall back to safe defaults rather than throwing. */
    public static User fromJson(JSONObject json) {
        return new User(
                json.optInt("userId", 0),
                json.optString("username", ""),
                json.optString("role", ""),
                json.optInt("profileTypeId", 0),
                json.optString("profileTypeName", ""),
                json.optString("email", ""),
                json.optString("fullName", ""),
                json.optString("accountStatus", "ACTIVE"),
                json.optString("memberSince", ""));
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public int getProfileTypeId() {
        return profileTypeId;
    }

    public String getProfileTypeName() {
        return profileTypeName;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public String getMemberSince() {
        return memberSince;
    }
}
